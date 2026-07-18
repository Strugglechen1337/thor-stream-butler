package de.thorstream.butler.core.network

import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.service.StreamingPortProbe

/**
 * Curated TCP ports of common local game-streaming services. Only these and
 * the host's own configured port are ever probed — no port ranges, no
 * network scans.
 */
object StreamingPorts {
    val KNOWN: List<StreamingPortProbe> = listOf(
        StreamingPortProbe(47984, "Sunshine / GameStream HTTPS"),
        StreamingPortProbe(47989, "Sunshine / GameStream HTTP"),
        StreamingPortProbe(47990, "Sunshine Web UI"),
        StreamingPortProbe(48010, "Sunshine / GameStream RTSP"),
        StreamingPortProbe(9295, "PlayStation Remote Play"),
        StreamingPortProbe(27036, "Steam Remote Play"),
    )

    /** Probes for one host: its configured port first, then the curated list. */
    fun probesFor(host: LocalHost): List<StreamingPortProbe> {
        val custom = host.port
            ?.takeIf { candidate -> KNOWN.none { it.port == candidate } }
            ?.let { StreamingPortProbe(it, null) }
        return listOfNotNull(custom) + KNOWN
    }
}
