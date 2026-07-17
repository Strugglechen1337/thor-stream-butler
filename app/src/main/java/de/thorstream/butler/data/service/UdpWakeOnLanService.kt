package de.thorstream.butler.data.service

import de.thorstream.butler.core.common.AppError
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.network.WakeOnLanPacket
import de.thorstream.butler.domain.service.WakeOnLanService
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class UdpWakeOnLanService @Inject constructor() : WakeOnLanService {
    override suspend fun sendMagicPacket(macAddress: String, broadcastAddress: String, port: Int): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = WakeOnLanPacket.create(macAddress)
            val address = InetAddress.getByName(broadcastAddress)
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = 2_000
                socket.send(DatagramPacket(payload, payload.size, address, port.coerceIn(1, 65_535)))
            }
            AppResult.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalArgumentException) {
            AppResult.Failure(AppError.InvalidInput("Die MAC-Adresse oder der Wake-on-LAN-Port ist ungültig."))
        } catch (_: Exception) {
            AppResult.Failure(AppError.Unavailable("Das Wake-on-LAN-Paket konnte nicht gesendet werden."))
        }
    }
}

