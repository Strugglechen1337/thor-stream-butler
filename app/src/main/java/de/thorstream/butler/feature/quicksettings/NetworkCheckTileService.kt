package de.thorstream.butler.feature.quicksettings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import de.thorstream.butler.MainActivity
import de.thorstream.butler.R
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.core.designsystem.labelRes
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings tile that mirrors the latest stored network quality and
 * opens the app directly into a running network test when tapped. The tile
 * itself never measures; it only reads the local history.
 */
@AndroidEntryPoint
class NetworkCheckTileService : TileService() {

    @Inject lateinit var historyRepository: NetworkHistoryRepository

    @Inject lateinit var strings: StringProvider

    private var listeningScope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        listeningScope = scope
        scope.launch {
            val latest = try {
                historyRepository.observeHistory().first().firstOrNull()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            updateTile(latest)
        }
    }

    override fun onStopListening() {
        listeningScope?.cancel()
        listeningScope = null
        super.onStopListening()
    }

    private fun updateTile(latest: NetworkMeasurement?) {
        val tile = qsTile ?: return
        val quality = latest?.assessment?.quality
        tile.state = when (quality) {
            NetworkQuality.OPTIMAL, NetworkQuality.USABLE -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = strings.get(R.string.tile_network_check)
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = quality?.let { strings.get(it.labelRes()) }
                ?: strings.get(R.string.tile_no_measurement)
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { openNetworkTest() }
        } else {
            openNetworkTest()
        }
    }

    // The Intent overload is the only option below API 34; the call is version-gated.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openNetworkTest() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_RUN_NETWORK_TEST)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (_: RuntimeException) {
            // The panel may already be collapsing; opening the app is best effort.
        }
    }
}
