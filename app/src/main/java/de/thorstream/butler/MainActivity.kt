package de.thorstream.butler

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint
import de.thorstream.butler.navigation.ThorApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Set while an external entry point (Quick Settings tile) requests an immediate network test. */
    private val pendingNetworkTest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        setContent {
            ThorApp(
                runNetworkTest = pendingNetworkTest.value,
                onNetworkTestConsumed = { pendingNetworkTest.value = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent?.action == ACTION_RUN_NETWORK_TEST) {
            pendingNetworkTest.value = true
            intent.action = null
        }
    }

    companion object {
        const val ACTION_RUN_NETWORK_TEST = "de.thorstream.butler.action.RUN_NETWORK_TEST"
    }
}
