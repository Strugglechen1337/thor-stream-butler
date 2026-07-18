package de.thorstream.butler.data.service

import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppError
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.domain.service.PortCheckResult
import de.thorstream.butler.domain.service.PortCheckService
import de.thorstream.butler.domain.service.StreamingPortProbe
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Probes individual TCP ports on one explicitly entered host with bounded
 * concurrency. A refused or timed-out connection reports the port as closed;
 * only failures outside the probes (for example an unresolvable host) fail
 * the whole check.
 */
@Singleton
class TcpPortCheckService @Inject constructor(private val strings: StringProvider) : PortCheckService {
    override suspend fun checkPorts(
        host: String,
        probes: List<StreamingPortProbe>,
        timeoutMillis: Int,
    ): AppResult<List<PortCheckResult>> = withContext(Dispatchers.IO) {
        try {
            val limit = Semaphore(4)
            val results = coroutineScope {
                probes.map { probe ->
                    async {
                        limit.withPermit {
                            PortCheckResult(
                                port = probe.port,
                                serviceName = probe.serviceName,
                                open = isOpen(host, probe.port, timeoutMillis),
                            )
                        }
                    }
                }.awaitAll()
            }
            AppResult.Success(results)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AppResult.Failure(AppError.Unavailable(strings.get(R.string.error_port_check_failed)))
        }
    }

    private fun isOpen(host: String, port: Int, timeoutMillis: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMillis)
            true
        }
    } catch (_: Exception) {
        false
    }
}
