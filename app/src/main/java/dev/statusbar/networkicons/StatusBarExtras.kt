package dev.statusbar.networkicons

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors
import kotlin.math.roundToInt

internal data class TimerProgress(val end: Long, val duration: Long)

internal class StatusBarExtras(private val host: View) {
    private val density = host.resources.displayMetrics.density
    private val track = label(11f).apply { maxWidth = (92 * density).roundToInt() }
    private val privacy = label(10f)
    private val power = label(11f)
    private val glance = label(11f).apply { maxWidth = (118 * density).roundToInt() }
    private val unread = label(13f).apply {
        text = "•"
        contentDescription = "未读通知"
    }
    private var lastTrack: String? = null
    private var trackUntil = 0L
    @Volatile private var observedTrack: String? = null
    private var readingTrack = false
    private var trackRequest = 0
    private var mediaFailed = false
    private val chinese get() = host.resources.configuration.locales[0].language == "zh"

    fun update(
        options: StatusBarOptions,
        clock: TextView?,
        privacyKinds: Set<String>,
        tint: Int,
        glanceText: String?,
        unreadVisible: Boolean,
        chargingPower: String?,
    ) {
        val parent = clock?.parent as? LinearLayout ?: find<LinearLayout>("phone_status_bar_left_container")
        var index = if (parent == null) 0 else parent.indexOfChild(clock) + 1
        setLabel(power, chargingPower, tint, parent, index)
        if (chargingPower != null) index++
        val title = if (options.headphoneTrack && parent != null &&
            host.context.getSystemService(android.app.KeyguardManager::class.java)?.isKeyguardLocked != true
        ) readTrack() else {
            observedTrack = null
            trackRequest++
            null
        }
        val now = SystemClock.elapsedRealtime()
        if (title != lastTrack) {
            lastTrack = title
            trackUntil = now + 4000
        }
        if (glanceText != null) {
            detach(track)
            setLabel(glance, glanceText, tint, parent, index++)
        } else {
            detach(glance)
            val trackTitle = title?.takeIf { now < trackUntil }
            setLabel(track, trackTitle, tint, parent, index)
            if (trackTitle != null) index++
        }
        setLabel(unread, "•".takeIf { unreadVisible }, 0xFF2EAE76.toInt(), parent, index)

        val indicator = find<View>("home_privacy_container")?.takeIf { it.isShown }
        val privacyGroup = (indicator?.parent as? LinearLayout) ?: find<LinearLayout>("privacy_container")
        val label = privacyKinds.mapNotNull {
            when (it) {
                "TYPE_CAMERA" -> if (chinese) "摄像头" else "Camera"
                "TYPE_MICROPHONE" -> if (chinese) "麦克风" else "Mic"
                else -> null
            }
        }.joinToString(" · ").ifEmpty { null }
        val privacyIndex = if (indicator != null && privacyGroup != null && indicator.parent === privacyGroup) {
            val position = privacyGroup.indexOfChild(indicator)
            val existing = privacyGroup.indexOfChild(privacy)
            if (existing in 0 until position) position - 1 else position
        } else 0
        setLabel(privacy, label?.takeIf { options.privacyLabels }, tint, privacyGroup, privacyIndex)
    }

    fun clear() {
        listOf(track, privacy, glance, unread, power).forEach(::detach)
        lastTrack = null
        trackUntil = 0
        observedTrack = null
        trackRequest++
    }

    private fun readTrack(): String? {
        if (!readingTrack && !mediaFailed) {
            readingTrack = true
            val request = ++trackRequest
            val context = host.context
            mediaReader.execute {
                val title = try {
                    currentTrack(context)
                } catch (error: RuntimeException) {
                    main.post { mediaFailed = true }
                    Log.w(LOG_TAG, "Media playback route unavailable", error)
                    null
                }
                main.post {
                    readingTrack = false
                    if (request == trackRequest) observedTrack = title
                }
            }
        }
        return observedTrack
    }

    private fun currentTrack(context: Context): String? {
        val audio = context.getSystemService(AudioManager::class.java) ?: return null
        if (!isHeadphoneRoute(audio)) return null
        val sessions = context.getSystemService(MediaSessionManager::class.java) ?: return null
        return sessions.getActiveSessions(null).firstNotNullOfOrNull { controller ->
            if (controller.playbackState?.state != PlaybackState.STATE_PLAYING) return@firstNotNullOfOrNull null
            controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?.trim()?.takeIf(String::isNotEmpty)
        }
    }

    @Suppress("DEPRECATION")
    @android.annotation.SuppressLint("InlinedApi")
    private fun isHeadphoneRoute(audio: AudioManager): Boolean {
        if (Build.VERSION.SDK_INT < 33) return audio.isWiredHeadsetOn || audio.isBluetoothA2dpOn
        val devices = audio.getAudioDevicesForAttributes(mediaAttributes)
        val headphoneTypes = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_HEARING_AID,
        )
        return devices.isNotEmpty() && devices.all { it.type in headphoneTypes }
    }

    private inline fun <reified T : View> find(name: String): T? {
        val id = host.resources.getIdentifier(name, "id", host.context.packageName)
        return host.findViewById<View>(id) as? T
    }

    private fun label(size: Float) = TextView(host.context).apply {
        gravity = Gravity.CENTER_VERTICAL
        textSize = size
        setSingleLine()
        ellipsize = TextUtils.TruncateAt.END
        setPaddingRelative((4 * density).roundToInt(), 0, (2 * density).roundToInt(), 0)
    }

    private fun setLabel(
        label: TextView, value: String?, tint: Int, parent: LinearLayout?, index: Int,
    ) {
        if (value == null || parent == null) {
            detach(label)
            return
        }
        if (label.text != value) label.text = value
        if (label.currentTextColor != tint) label.setTextColor(tint)
        place(label, parent, index)
    }

    private fun place(view: View, parent: LinearLayout, index: Int) {
        if (view.parent === parent && parent.indexOfChild(view) == index) return
        detach(view)
        parent.addView(view, index.coerceIn(0, parent.childCount), LinearLayout.LayoutParams(-2, -1))
    }

    private fun detach(view: View) {
        (view.parent as? LinearLayout)?.removeView(view)
    }

    private companion object {
        val mediaAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        val mediaReader = Executors.newSingleThreadExecutor()
        val main = android.os.Handler(android.os.Looper.getMainLooper())
    }
}
