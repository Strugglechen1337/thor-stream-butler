package de.thorstream.butler.data.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.thorstream.butler.domain.service.DeviceStatus
import de.thorstream.butler.domain.service.DeviceStatusService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads battery level, charging state, and battery temperature from the
 * sticky [Intent.ACTION_BATTERY_CHANGED] broadcast. This requires no
 * permission and no receiver registration; missing extras map to null.
 */
@Singleton
class AndroidDeviceStatusService @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeviceStatusService {
    override fun readStatus(): DeviceStatus {
        val intent = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: RuntimeException) {
            null
        } ?: return DeviceStatus()

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val temperatureTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)

        return DeviceStatus(
            batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else null,
            charging = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> true
                BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false
                else -> null
            },
            batteryTemperatureCelsius = temperatureTenths
                .takeIf { it != Int.MIN_VALUE && it in -500..1500 }
                ?.let { it / 10.0 },
        )
    }
}
