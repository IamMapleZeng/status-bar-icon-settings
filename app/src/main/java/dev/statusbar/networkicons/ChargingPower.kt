package dev.statusbar.networkicons

internal fun chargingPowerWatts(
    plugged: Boolean,
    charging: Boolean,
    voltageMv: Int,
    currentUa: Int,
    allowNegativeCurrent: Boolean,
): Double? {
    if (!plugged || !charging || voltageMv <= 0 || currentUa == Int.MIN_VALUE || currentUa == 0) return null
    if (currentUa < 0 && !allowNegativeCurrent) return null
    return kotlin.math.abs(currentUa.toDouble()) * voltageMv / 1_000_000_000.0
}
