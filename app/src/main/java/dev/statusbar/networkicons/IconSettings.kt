package dev.statusbar.networkicons

import android.content.SharedPreferences

internal const val LOG_TAG = "NetworkStatusIcons"
internal const val PREF_GROUP = "network_icons"

internal const val DEFAULT_RING_WIDTH = 5
internal const val DEFAULT_RING_OFFSET_Y = -0.5f

internal const val DEFAULT_LINE_COLOR = 0xFF00C896.toInt()

internal enum class ChargingAnimation {
    OFF, GRADIENT, WAVE, COMET, DOUBLE_SWEEP, BREATH, RAINBOW;

    companion object {
        fun fromPreference(value: Any?): ChargingAnimation =
            if (value == "ECG") WAVE else entries.firstOrNull { it.name == value } ?: OFF
    }
}

internal data class IconSettings(
    val hideMobile: Boolean,
    val hideWifi: Boolean,
    val hideNoSim: Boolean,
    val hideBattery: Boolean,
    val hideClock: Boolean,
    val networkSpeedThreshold: Int?,
    val batteryLine: Boolean,
    val batteryLineColor: Int,
    val chargingAnimation: ChargingAnimation,
    val clockSeconds: Boolean,
    val clockColor: Int?,
    val debug: Boolean,
    val options: StatusBarOptions,
    val batteryRing: Boolean = true,
    val batteryRingColor: Int = DEFAULT_LINE_COLOR,
    val ringAnimation: RingChargingAnimation = RingChargingAnimation.RAINBOW,
    val batteryLineWidth: Int = 2,
    val batteryRingWidth: Int = DEFAULT_RING_WIDTH,
    val batteryRingOffsetX: Float = 0f,
    val batteryRingOffsetY: Float = DEFAULT_RING_OFFSET_Y,
) {
    fun save(prefs: SharedPreferences): Boolean = prefs.edit()
        .putBoolean("hide_mobile", hideMobile)
        .putBoolean("hide_wifi", hideWifi)
        .putBoolean("hide_no_sim", hideNoSim)
        .putBoolean("hide_battery", hideBattery)
        .putBoolean("hide_clock", hideClock)
        .apply {
            if (networkSpeedThreshold == null) remove("network_speed_threshold")
            else putInt("network_speed_threshold", networkSpeedThreshold)
        }
        .putInt("battery_line_width", batteryLineWidth)
        .putInt("battery_ring_width", batteryRingWidth)
        .putFloat("battery_ring_offset_x", batteryRingOffsetX)
        .putFloat("battery_ring_offset_y", batteryRingOffsetY)
        .putBoolean("battery_ring", batteryRing)
        .putInt("battery_ring_color", batteryRingColor)
        .putString("ring_animation", ringAnimation.name)
        .putBoolean("battery_line", batteryLine)
        .putInt("battery_line_color", batteryLineColor)
        .putString("charging_animation", chargingAnimation.name)
        .putBoolean("clock_seconds", clockSeconds)
        .apply {
            if (clockColor == null) remove("clock_color") else putInt("clock_color", clockColor)
        }
        .putBoolean("debug", debug)
        .apply { options.save(this) }
        .commit()

    companion object {
        fun read(prefs: SharedPreferences): IconSettings {
            val values = prefs.all
            return IconSettings(
                values["hide_mobile"] == true,
                values["hide_wifi"] == true,
                values["hide_no_sim"] == true,
                values["hide_battery"] == true,
                values["hide_clock"] == true,
                (values["network_speed_threshold"] as? Int)?.takeIf { it >= 0 },
                values["battery_line"] == true,
                (values["battery_line_color"] as? Int) ?: DEFAULT_LINE_COLOR,
                ChargingAnimation.fromPreference(values["charging_animation"]),
                values["clock_seconds"] == true,
                values["clock_color"] as? Int,
                values["debug"] == true,
                StatusBarOptions.read(values),
                values["battery_ring"] as? Boolean ?: true,
                values["battery_ring_color"] as? Int ?: DEFAULT_LINE_COLOR,
                RingChargingAnimation.entries.firstOrNull { it.name == values["ring_animation"] } ?: RingChargingAnimation.RAINBOW,
                (values["battery_line_width"] as? Int)?.takeIf { it in 1..6 } ?: 2,
                (values["battery_ring_width"] as? Int)?.takeIf { it in 1..6 } ?: DEFAULT_RING_WIDTH,
                (values["battery_ring_offset_x"] as? Float)?.takeIf { it in -10f..10f } ?: 0f,
                (values["battery_ring_offset_y"] as? Float)?.takeIf { it in (DEFAULT_RING_OFFSET_Y - 10f)..10f } ?: DEFAULT_RING_OFFSET_Y,
            )
        }
    }
}
