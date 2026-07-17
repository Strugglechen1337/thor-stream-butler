package de.thorstream.butler.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.thorstream.butler.R
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.StreamingType
import de.thorstream.butler.domain.service.DiagnosticStep

/** Localized display name for a connection type. */
@Composable
fun ConnectionType.label(): String = stringResource(
    when (this) {
        ConnectionType.ETHERNET -> R.string.conn_ethernet
        ConnectionType.WIFI -> R.string.conn_wifi
        ConnectionType.CELLULAR -> R.string.conn_cellular
        ConnectionType.VPN -> R.string.conn_vpn
        ConnectionType.OTHER -> R.string.conn_other
        ConnectionType.NONE -> R.string.conn_none
    },
)

/** Localized display name for a network quality rating. */
@Composable
fun NetworkQuality.label(): String = stringResource(labelRes())

@StringRes
fun NetworkQuality.labelRes(): Int = when (this) {
    NetworkQuality.OPTIMAL -> R.string.quality_optimal
    NetworkQuality.USABLE -> R.string.quality_usable
    NetworkQuality.PROBLEMATIC -> R.string.quality_problematic
    NetworkQuality.NOT_MEASURABLE -> R.string.quality_not_measurable
}

/** Brand name of a streaming category, or the localized "custom" label. */
@Composable
fun StreamingType.label(): String = brandName ?: stringResource(R.string.streaming_type_custom)

/** Localized progress label for a diagnostics phase. */
@StringRes
fun DiagnosticStep.labelRes(): Int = when (this) {
    DiagnosticStep.DETECTING_CONNECTION -> R.string.step_detecting_connection
    DiagnosticStep.NETWORK_UNAVAILABLE -> R.string.step_network_unavailable
    DiagnosticStep.CONNECTION_READ -> R.string.step_connection_read
    DiagnosticStep.DNS_CHECKED -> R.string.step_dns_checked
    DiagnosticStep.LATENCY_MEASURED -> R.string.step_latency_measured
    DiagnosticStep.HOST_CHECKED -> R.string.step_host_checked
    DiagnosticStep.DOWNLOAD_MEASURED -> R.string.step_download_measured
    DiagnosticStep.COMPLETED -> R.string.step_completed
}
