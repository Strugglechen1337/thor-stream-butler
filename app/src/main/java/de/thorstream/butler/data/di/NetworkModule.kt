package de.thorstream.butler.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.thorstream.butler.data.service.AndroidDeviceStatusService
import de.thorstream.butler.data.service.AndroidNetworkDiagnosticsService
import de.thorstream.butler.data.service.AndroidConfigurationTransferService
import de.thorstream.butler.data.service.AndroidPingService
import de.thorstream.butler.data.service.HttpsSpeedTestService
import de.thorstream.butler.data.service.TcpHostDiscoveryService
import de.thorstream.butler.data.service.NsdLocalHostDiscoveryService
import de.thorstream.butler.data.service.UdpWakeOnLanService
import de.thorstream.butler.domain.service.DeviceStatusService
import de.thorstream.butler.domain.service.HostDiscoveryService
import de.thorstream.butler.domain.service.ConfigurationTransferService
import de.thorstream.butler.domain.service.NetworkDiagnosticsService
import de.thorstream.butler.domain.service.LocalHostDiscoveryService
import de.thorstream.butler.domain.service.PingService
import de.thorstream.butler.domain.service.SpeedTestService
import de.thorstream.butler.domain.service.WakeOnLanService

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds abstract fun bindConfigurationTransferService(implementation: AndroidConfigurationTransferService): ConfigurationTransferService
    @Binds abstract fun bindNetworkDiagnostics(implementation: AndroidNetworkDiagnosticsService): NetworkDiagnosticsService
    @Binds abstract fun bindPingService(implementation: AndroidPingService): PingService
    @Binds abstract fun bindSpeedTestService(implementation: HttpsSpeedTestService): SpeedTestService
    @Binds abstract fun bindHostDiscoveryService(implementation: TcpHostDiscoveryService): HostDiscoveryService
    @Binds abstract fun bindLocalHostDiscoveryService(implementation: NsdLocalHostDiscoveryService): LocalHostDiscoveryService
    @Binds abstract fun bindWakeOnLanService(implementation: UdpWakeOnLanService): WakeOnLanService
    @Binds abstract fun bindDeviceStatusService(implementation: AndroidDeviceStatusService): DeviceStatusService
}
