package de.thorstream.butler.data.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppError
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.core.network.ConnectionTypeResolver
import de.thorstream.butler.core.network.NetworkCalculations
import de.thorstream.butler.core.validation.NetworkValidators
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.repository.DiagnosticEvent
import de.thorstream.butler.domain.repository.DiagnosticLogRepository
import de.thorstream.butler.domain.service.DiagnosticProgress
import de.thorstream.butler.domain.service.DiagnosticStep
import de.thorstream.butler.domain.service.HostDiscoveryService
import de.thorstream.butler.domain.service.NetworkDiagnosticsService
import de.thorstream.butler.domain.service.PingResult
import de.thorstream.butler.domain.service.PingService
import de.thorstream.butler.domain.service.SpeedTestService
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class AndroidPingService @Inject constructor(private val strings: StringProvider) : PingService {
    private val timeRegex = Regex("time[=<]([0-9.]+) ?ms", RegexOption.IGNORE_CASE)

    override suspend fun ping(host: String, count: Int, timeoutMillis: Int): AppResult<PingResult> = withContext(Dispatchers.IO) {
        val safeCount = count.coerceIn(1, 20)
        val latencies = mutableListOf<Double>()
        try {
            repeat(safeCount) {
                currentCoroutineContext().ensureActive()
                val process = ProcessBuilder(
                    "/system/bin/ping", "-c", "1", "-W", ((timeoutMillis + 999) / 1000).toString(), host,
                ).redirectErrorStream(true).start()
                try {
                    val output = withTimeoutOrNull(timeoutMillis.toLong() + 750L) { process.inputStream.bufferedReader().use { it.readText() } }
                    output?.let { timeRegex.find(it)?.groupValues?.getOrNull(1)?.toDoubleOrNull() }?.let(latencies::add)
                } finally {
                    process.destroy()
                }
                if (it < safeCount - 1) delay(120)
            }
            AppResult.Success(PingResult(latencies, safeCount))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            fallbackReachability(host, safeCount, timeoutMillis)
        }
    }

    private suspend fun fallbackReachability(host: String, count: Int, timeoutMillis: Int): AppResult<PingResult> = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(host)
            val latencies = buildList {
                repeat(count) {
                    currentCoroutineContext().ensureActive()
                    var reachable = false
                    val nanos = measureNanoTime { reachable = address.isReachable(timeoutMillis) }
                    if (reachable) add(nanos / 1_000_000.0)
                }
            }
            AppResult.Success(PingResult(latencies, count))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AppResult.Failure(AppError.Unavailable(strings.get(R.string.error_ping_unreachable)))
        }
    }
}

@Singleton
class TcpHostDiscoveryService @Inject constructor() : HostDiscoveryService {
    override suspend fun isReachable(host: String, port: Int?, timeoutMillis: Int): AppResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val reachable = if (port != null) {
                Socket().use { socket -> socket.connect(InetSocketAddress(host, port), timeoutMillis); true }
            } else {
                InetAddress.getByName(host).isReachable(timeoutMillis)
            }
            AppResult.Success(reachable)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AppResult.Success(false)
        }
    }
}

@Singleton
class HttpsSpeedTestService @Inject constructor(private val strings: StringProvider) : SpeedTestService {
    override suspend fun measureDownloadMbps(testDurationSeconds: Int): AppResult<Double> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val duration = testDurationSeconds.coerceIn(1, 15)
            connection = (URL("https://speed.cloudflare.com/__down?bytes=100000000").openConnection() as HttpURLConnection).apply {
                connectTimeout = 3_000
                readTimeout = duration * 1_000 + 2_000
                useCaches = false
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext AppResult.Failure(AppError.Unavailable(strings.get(R.string.error_download_unavailable)))
            }
            val started = System.nanoTime()
            var bytes = 0L
            BufferedInputStream(connection.inputStream).use { input ->
                val buffer = ByteArray(32 * 1024)
                val deadline = started + duration * 1_000_000_000L
                while (System.nanoTime() < deadline) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    bytes += read
                }
            }
            val seconds = (System.nanoTime() - started) / 1_000_000_000.0
            if (bytes == 0L || seconds <= 0.0) AppResult.Failure(AppError.Unavailable(strings.get(R.string.error_download_no_data)))
            else AppResult.Success(bytes * 8.0 / seconds / 1_000_000.0)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AppResult.Failure(AppError.Unavailable(strings.get(R.string.error_download_unavailable)))
        } finally {
            connection?.disconnect()
        }
    }
}

@Singleton
class AndroidNetworkDiagnosticsService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val strings: StringProvider,
    private val pingService: PingService,
    private val speedTestService: SpeedTestService,
    private val hostDiscoveryService: HostDiscoveryService,
    private val diagnosticLogRepository: DiagnosticLogRepository,
) : NetworkDiagnosticsService {
    private val connectivityManager get() = context.getSystemService(ConnectivityManager::class.java)

    @Suppress("DEPRECATION")
    override suspend fun readConnectionSnapshot(): AppResult<NetworkSnapshot> = withContext(Dispatchers.IO) {
        try {
            val network = connectivityManager.activeNetwork
                ?: return@withContext AppResult.Failure(AppError.NoNetwork(strings.get(R.string.error_no_network)))
            val capabilities = connectivityManager.getNetworkCapabilities(network)
                ?: return@withContext AppResult.Failure(AppError.NoNetwork(strings.get(R.string.error_no_network)))
            val properties = connectivityManager.getLinkProperties(network)
            val type = ConnectionTypeResolver.resolve(
                hasVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
                hasEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
                hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            )
            val wifiInfo = if (Build.VERSION.SDK_INT >= 29) capabilities.transportInfo as? WifiInfo else legacyWifiInfo(type)
            val ssid = wifiInfo?.ssid?.takeUnless { it == "<unknown ssid>" }?.trim('"')
            val localAddresses = properties?.linkAddresses.orEmpty().map { it.address }.filterNot { it.isLoopbackAddress }
            val localIp = (localAddresses.firstOrNull { it is Inet4Address } ?: localAddresses.firstOrNull())?.hostAddress
            val defaultGateways = properties?.routes.orEmpty().filter { it.isDefaultRoute }.mapNotNull { it.gateway }
            val gateway = (defaultGateways.firstOrNull { it is Inet4Address } ?: defaultGateways.firstOrNull())?.hostAddress
            AppResult.Success(
                NetworkSnapshot(
                    connectionType = type,
                    localIpAddress = localIp,
                    gateway = gateway,
                    ssid = ssid,
                    wifiFrequencyMhz = wifiInfo?.frequency?.takeIf { it > 0 },
                    linkSpeedMbps = wifiInfo?.linkSpeed?.takeIf { it >= 0 },
                    signalStrengthPercent = wifiInfo?.rssi?.takeIf { it in -126..0 }?.let { WifiManager.calculateSignalLevel(it, 101).coerceIn(0, 100) },
                    internetValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            AppResult.Failure(AppError.MissingPermission(strings.get(R.string.error_network_permission)))
        } catch (_: RuntimeException) {
            AppResult.Failure(AppError.Technical(strings.get(R.string.error_network_read)))
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyWifiInfo(type: ConnectionType): WifiInfo? =
        if (type == ConnectionType.WIFI) context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo else null

    override fun runDiagnostics(
        target: String,
        pingCount: Int,
        host: String?,
        port: Int?,
        includeDownloadTest: Boolean,
        testDurationSeconds: Int,
    ): Flow<DiagnosticProgress> = flow {
        var latestSnapshot: NetworkSnapshot? = null
        try {
            logSafely(DiagnosticEvent.TEST_STARTED)
            emit(DiagnosticProgress(DiagnosticStep.DETECTING_CONNECTION, 0.05f))
            val base = when (val result = readConnectionSnapshot()) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> {
                    logSafely(DiagnosticEvent.TEST_FAILED)
                    emit(DiagnosticProgress(DiagnosticStep.NETWORK_UNAVAILABLE, 1f, errorMessage = result.error.message, completed = true))
                    return@flow
                }
            }
            latestSnapshot = base
            logSafely(DiagnosticEvent.CONNECTION_READ)
            emit(DiagnosticProgress(DiagnosticStep.CONNECTION_READ, 0.2f, base))

            val dnsProbe = target.takeUnless { NetworkValidators.isValidIpv4(it) || NetworkValidators.isValidIpv6(it) }
                ?: DNS_PROBE_HOST
            val dnsReachable = withContext(Dispatchers.IO) {
                withTimeoutOrNull(2_500) {
                    try {
                        InetAddress.getByName(dnsProbe)
                        true
                    } catch (_: Exception) {
                        false
                    }
                } ?: false
            }
            var snapshot = base.copy(dnsReachable = dnsReachable)
            latestSnapshot = snapshot
            logSafely(DiagnosticEvent.DNS_CHECKED)
            emit(DiagnosticProgress(DiagnosticStep.DNS_CHECKED, 0.35f, snapshot))

            snapshot = when (val ping = pingService.ping(target, pingCount.coerceIn(1, 20))) {
                is AppResult.Success -> snapshot.copy(
                    latencyMs = ping.value.successfulLatenciesMs.takeIf { it.isNotEmpty() }?.average(),
                    jitterMs = NetworkCalculations.jitterMs(ping.value.successfulLatenciesMs),
                    packetLossPercent = NetworkCalculations.packetLossPercent(ping.value.sentCount, ping.value.receivedCount),
                )
                is AppResult.Failure -> snapshot
            }
            latestSnapshot = snapshot
            logSafely(DiagnosticEvent.LATENCY_MEASURED)
            emit(DiagnosticProgress(DiagnosticStep.LATENCY_MEASURED, 0.68f, snapshot))

            if (host != null) {
                val reachable = when (val result = hostDiscoveryService.isReachable(host, port)) {
                    is AppResult.Success -> result.value
                    is AppResult.Failure -> false
                }
                snapshot = snapshot.copy(host = host, hostReachable = reachable)
                latestSnapshot = snapshot
                logSafely(DiagnosticEvent.HOST_CHECKED)
                emit(DiagnosticProgress(DiagnosticStep.HOST_CHECKED, 0.82f, snapshot))
            }

            if (includeDownloadTest) {
                val speed = when (val result = speedTestService.measureDownloadMbps(testDurationSeconds)) {
                    is AppResult.Success -> result.value
                    is AppResult.Failure -> null
                }
                snapshot = snapshot.copy(downloadMbps = speed)
                latestSnapshot = snapshot
                logSafely(DiagnosticEvent.DOWNLOAD_MEASURED)
                emit(DiagnosticProgress(DiagnosticStep.DOWNLOAD_MEASURED, 0.94f, snapshot))
            }
            logSafely(DiagnosticEvent.TEST_COMPLETED)
            emit(DiagnosticProgress(DiagnosticStep.COMPLETED, 1f, snapshot, completed = true))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            logSafely(DiagnosticEvent.TEST_FAILED)
            emit(
                DiagnosticProgress(
                    step = DiagnosticStep.COMPLETED,
                    progress = 1f,
                    snapshot = latestSnapshot,
                    completed = true,
                    errorMessage = strings.get(R.string.error_diagnostics_failed),
                ),
            )
        }
    }

    private suspend fun logSafely(event: DiagnosticEvent) {
        try {
            diagnosticLogRepository.log(event)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Optional local logging must never change the diagnostic result.
        }
    }

    private companion object {
        const val DNS_PROBE_HOST = "example.com"
    }
}
