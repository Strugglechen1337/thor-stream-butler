package de.thorstream.butler.data.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppError
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.domain.service.DiscoveredHost
import de.thorstream.butler.domain.repository.DiagnosticEvent
import de.thorstream.butler.domain.repository.DiagnosticLogRepository
import de.thorstream.butler.domain.service.LocalHostDiscoveryService
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class NsdLocalHostDiscoveryService @Inject constructor(
    @ApplicationContext context: Context,
    private val strings: StringProvider,
    private val diagnosticLogRepository: DiagnosticLogRepository,
) : LocalHostDiscoveryService {
    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private val resolveMutex = Mutex()

    override suspend fun discover(timeoutMillis: Long): AppResult<List<DiscoveredHost>> = try {
        logSafely(DiagnosticEvent.HOST_DISCOVERY_STARTED)
        val values = withTimeoutOrNull(timeoutMillis.coerceIn(2_000, 30_000)) {
            discoveryFlow().toList()
        }.orEmpty()
            .distinctBy { "${it.address}:${it.port}" }
            .sortedBy { it.name.lowercase() }
        logSafely(DiagnosticEvent.HOST_DISCOVERY_COMPLETED)
        AppResult.Success(values)
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        AppResult.Failure(AppError.Unavailable(strings.get(R.string.hosts_discovery_failed)))
    }

    private fun discoveryFlow() = callbackFlow {
        val stopped = AtomicBoolean(false)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.contains("_nvstream._tcp", ignoreCase = true)) return
                launch {
                    resolveMutex.withLock {
                        withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) { resolve(serviceInfo) }?.let { trySend(it) }
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                stopped.set(true)
                runCatching { nsdManager.stopServiceDiscovery(this) }
                close(IllegalStateException("NSD start failed: $errorCode"))
            }
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose {
            if (!stopped.getAndSet(true)) runCatching { nsdManager.stopServiceDiscovery(listener) }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolve(serviceInfo: NsdServiceInfo): DiscoveredHost? = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean(false)
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                if (completed.compareAndSet(false, true) && continuation.isActive) continuation.resume(null)
            }

            override fun onServiceResolved(resolved: NsdServiceInfo) {
                if (!completed.compareAndSet(false, true) || !continuation.isActive) return
                val host = resolved.host
                val address = host?.hostAddress?.takeUnless { it.contains(':') }
                    ?: host?.hostName
                continuation.resume(
                    address?.let {
                        DiscoveredHost(
                            name = resolved.serviceName.takeIf(String::isNotBlank) ?: it,
                            address = it.removeSuffix("."),
                            port = resolved.port.takeIf { port -> port in 1..65_535 } ?: DEFAULT_PORT,
                            serviceType = resolved.serviceType,
                        )
                    },
                )
            }
        })
    }

    private suspend fun logSafely(event: DiagnosticEvent) {
        try {
            diagnosticLogRepository.log(event)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Optional local logging must never change discovery behavior.
        }
    }

    private companion object {
        const val SERVICE_TYPE = "_nvstream._tcp."
        const val DEFAULT_PORT = 47989
        const val RESOLVE_TIMEOUT_MILLIS = 3_000L
    }
}
