package dev.statusbar.networkicons

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

internal fun showStatusBarOptions(
    context: Context, initial: StatusBarOptions, batteryColor: Int, chargingAnimation: ChargingAnimation,
    lineWidthDp: Int = 2,
    onSave: (StatusBarOptions) -> Unit,
) {
    var draft = initial
    val density = context.resources.displayMetrics.density
    val padding = (20 * density).toInt()
    val body = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(padding, 0, padding, padding)
    }
    fun title(label: String) {
        body.addView(TextView(context).apply {
            text = label
            textSize = 17f
            setPadding(0, padding, 0, padding / 3)
            isAccessibilityHeading = true
        })
    }
    fun toggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
        body.addView(CheckBox(context).apply {
            text = label
            isChecked = checked
            setOnCheckedChangeListener { _, enabled -> change(enabled) }
        })
    }
    fun field(label: String, value: String): EditText {
        body.addView(TextView(context).apply { text = label; setPadding(0, padding / 2, 0, 0) })
        return EditText(context).apply {
            setSingleLine()
            setText(value)
            body.addView(this)
        }
    }
    fun colorButton(titleId: Int, current: () -> Int, change: (Int) -> Unit) {
        body.addView(Button(context).apply {
            fun updateLabel() {
                text = context.getString(R.string.color_setting_value, context.getString(titleId), colorHex(current()))
            }
            updateLabel()
            setOnClickListener {
                showColorPicker(context, titleId, current()) {
                    change(it)
                    updateLabel()
                }
            }
        })
    }
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val apps = context.packageManager.queryIntentActivities(launcher, 0)
        .associate { it.activityInfo.packageName to it.loadLabel(context.packageManager).toString() }
    fun appField(label: String, value: String): EditText {
        val input = field(label, value)
        body.addView(android.widget.Button(context).apply {
            text = "选择应用"
            setOnClickListener {
                val selected = packageNames(input.text.toString()).toMutableSet()
                val entries = (apps + selected.filterNot(apps::containsKey).associateWith { it })
                    .entries.sortedBy { it.value.lowercase() }
                val checked = entries.map { it.key in selected }.toBooleanArray()
                AlertDialog.Builder(context).setTitle(label)
                    .setMultiChoiceItems(entries.map { it.value }.toTypedArray(), checked) { _, i, enabled ->
                        if (enabled) selected.add(entries[i].key) else selected.remove(entries[i].key)
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ -> input.setText(selected.joinToString(", ")) }
                    .setNegativeButton(android.R.string.cancel, null).show()
            }
        })
        return input
    }

    title("天气")
    toggle("显示天气（状态栏最右侧）", initial.showWeather) { draft = draft.copy(showWeather = it) }
    toggle("降雨提醒（需开启天气）", initial.rainNotice) { draft = draft.copy(rainNotice = it) }
    toggle("降雨提醒出现时播放小雨滴", initial.rainDrops) { draft = draft.copy(rainDrops = it) }
    toggle(context.getString(R.string.weather_condition), initial.showWeatherCondition) {
        draft = draft.copy(showWeatherCondition = it)
    }
    val weatherLanguages = context.resources.getStringArray(R.array.weather_languages)
    body.addView(Button(context).apply {
        fun updateLabel() {
            text = context.getString(R.string.weather_language_value, weatherLanguages[draft.weatherLanguage.ordinal])
        }
        updateLabel()
        setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(R.string.weather_language)
                .setSingleChoiceItems(weatherLanguages, draft.weatherLanguage.ordinal) { dialog, index ->
                    draft = draft.copy(weatherLanguage = WeatherLanguage.entries[index])
                    updateLabel()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    })
    title("网速与电量")
    toggle("网速分档变色", initial.speedColors) { draft = draft.copy(speedColors = it) }
    val mediumSpeed = field("中速起点（KB/s，低于此值淡化）", initial.mediumSpeedKb.toString())
    val highSpeed = field("高速起点（KB/s）", initial.highSpeedKb.toString())
    mediumSpeed.inputType = android.text.InputType.TYPE_CLASS_NUMBER
    highSpeed.inputType = android.text.InputType.TYPE_CLASS_NUMBER
    colorButton(R.string.medium_speed_color, { draft.mediumSpeedColor }) {
        draft = draft.copy(mediumSpeedColor = it)
    }
    colorButton(R.string.high_speed_color, { draft.highSpeedColor }) {
        draft = draft.copy(highSpeedColor = it)
    }
    toggle("网速防抖（连续低速至少 3 秒才隐藏）", initial.speedSmoothing) { draft = draft.copy(speedSmoothing = it) }
    toggle("电量显示按电量变色", initial.batteryColors) { draft = draft.copy(batteryColors = it) }
    val batteryStart = field("中电量起点（%，低于此值为低电量）", initial.batteryLineColors.mediumStart.toString())
    val batteryEnd = field("中电量上限（%，高于此值为高电量）", initial.batteryLineColors.mediumEnd.toString())
    batteryStart.inputType = android.text.InputType.TYPE_CLASS_NUMBER
    batteryEnd.inputType = android.text.InputType.TYPE_CLASS_NUMBER
    fun readBatteryLineColors(): BatteryLineColors? {
        val start = batteryStart.text.toString().trim().toIntOrNull()
        if (start == null || start !in 1..98) {
            batteryStart.error = "请输入 1–98 的整数"
            batteryStart.requestFocus()
            return null
        }
        val end = batteryEnd.text.toString().trim().toIntOrNull()
        if (end == null || end !in (start + 1)..99) {
            batteryEnd.error = "请输入大于中电量起点且不超过 99 的整数"
            batteryEnd.requestFocus()
            return null
        }
        return draft.batteryLineColors.copy(mediumStart = start, mediumEnd = end)
    }
    colorButton(R.string.low_battery_line_color, { draft.batteryLineColors.low }) {
        draft = draft.copy(batteryLineColors = draft.batteryLineColors.copy(low = it))
    }
    colorButton(R.string.medium_battery_line_color, { draft.batteryLineColors.medium }) {
        draft = draft.copy(batteryLineColors = draft.batteryLineColors.copy(medium = it))
    }
    colorButton(R.string.high_battery_line_color, { draft.batteryLineColors.high }) {
        draft = draft.copy(batteryLineColors = draft.batteryLineColors.copy(high = it))
    }
    toggle("系统计时器进度线", initial.timerLine) { draft = draft.copy(timerLine = it) }
    toggle("充电达到 80% 和 100% 时显示粒子", initial.chargeParticles) { draft = draft.copy(chargeParticles = it) }
    toggle("点击状态栏空白处查看电量百分比", initial.batteryPercentage) { draft = draft.copy(batteryPercentage = it) }
    toggle("充满电时播放星星收尾动画", initial.chargeFinale) { draft = draft.copy(chargeFinale = it) }
    title("时间")
    toggle("下一场日程倒计时（提前 30 分钟）", initial.calendarCountdown) { draft = draft.copy(calendarCountdown = it) }
    toggle("同时显示日程标题（默认隐藏）", initial.calendarTitle) { draft = draft.copy(calendarTitle = it) }
    toggle("时间前显示星期", initial.weekday) { draft = draft.copy(weekday = it) }
    toggle("淡化秒数", initial.dimSeconds) { draft = draft.copy(dimSeconds = it) }
    toggle("白天整点星光", initial.hourlyStar) { draft = draft.copy(hourlyStar = it) }
    toggle("时钟手势：单击切换显秒，长按打开闹钟", initial.clockGestures) { draft = draft.copy(clockGestures = it) }
    title("图标布局")
    toggle("网速移到时间旁边", initial.speedByClock) { draft = draft.copy(speedByClock = it) }
    body.addView(Button(context).apply {
        fun updateLabel() {
            text = if (draft.iconOrder.isEmpty()) "图标顺序（跟随系统）" else "图标顺序（自定义）"
        }
        updateLabel()
        setOnClickListener {
            showIconOrderDialog(context, draft.iconOrder) { order ->
                draft = draft.copy(iconOrder = order)
                updateLabel()
            }
        }
    })
    val spacing = field("图标间距（0–12 dp，留空为系统间距）", initial.iconSpacing?.toString().orEmpty())
    spacing.inputType = android.text.InputType.TYPE_CLASS_NUMBER
    title("通知与应用样式")
    toggle("显示未读通知小点", initial.unreadDot) { draft = draft.copy(unreadDot = it) }
    toggle("收到通知时闪烁状态栏", initial.notificationFlash) { draft = draft.copy(notificationFlash = it) }
    val flashSeconds = field("闪烁持续时间（${FLASH_SECONDS.first}–${FLASH_SECONDS.last} 秒）",
        initial.notificationFlashSeconds.toString())
    flashSeconds.inputType = android.text.InputType.TYPE_CLASS_NUMBER
    colorButton(R.string.notification_flash_start, { draft.notificationFlashStart }) {
        draft = draft.copy(notificationFlashStart = it)
    }
    colorButton(R.string.notification_flash_end, { draft.notificationFlashEnd }) {
        draft = draft.copy(notificationFlashEnd = it)
    }
    body.addView(Button(context).apply {
        text = "模拟预览"
        setOnClickListener {
            val duration = flashSeconds.text.toString().trim().toIntOrNull()?.takeIf { it in FLASH_SECONDS }
                ?: draft.notificationFlashSeconds
            val colors = readBatteryLineColors() ?: return@setOnClickListener
            val preview = draft.copy(notificationFlashSeconds = duration, batteryLineColors = colors)
            showStatusBarPreview(context, preview, batteryColor, chargingAnimation, lineWidthDp)
        }
    })
    toggle("耳机播放时短暂显示新曲名", initial.headphoneTrack) { draft = draft.copy(headphoneTrack = it) }
    toggle("在系统隐私指示器旁显示摄像头／麦克风文字", initial.privacyLabels) { draft = draft.copy(privacyLabels = it) }
    toggle("启用顶部通知图标白名单", initial.notificationApps != null) {
        draft = draft.copy(notificationApps = if (it) "" else null)
    }
    val allowed = appField("允许的通知应用", initial.notificationApps.orEmpty())
    val reading = appField("阅读样式应用（仅保留时间和电量线）", initial.readingApps)
    val gaming = appField("游戏样式应用（仅保留网速）", initial.gamingApps)

    val dialog = AlertDialog.Builder(context).setTitle("状态栏进阶设置")
        .setView(ScrollView(context).apply { addView(body) })
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, null).show()
    dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val batteryLineColors = readBatteryLineColors() ?: return@setOnClickListener
        val mediumKb = mediumSpeed.text.toString().trim().toIntOrNull()
        if (mediumKb == null || mediumKb <= 0) {
            mediumSpeed.error = "请输入大于 0 的整数"
            mediumSpeed.requestFocus()
            return@setOnClickListener
        }
        val highKb = highSpeed.text.toString().trim().toIntOrNull()
        if (highKb == null || highKb <= mediumKb) {
            highSpeed.error = "高速起点必须大于中速起点"
            highSpeed.requestFocus()
            return@setOnClickListener
        }
        val seconds = flashSeconds.text.toString().trim().toIntOrNull()
        if (seconds == null || seconds !in FLASH_SECONDS) {
            flashSeconds.error = "请输入 ${FLASH_SECONDS.first}–${FLASH_SECONDS.last} 秒"
            flashSeconds.requestFocus()
            return@setOnClickListener
        }
        val spacingText = spacing.text.toString().trim()
        val dp = spacingText.toIntOrNull()
        if (spacingText.isNotEmpty() && (dp == null || dp !in 0..12)) {
            spacing.error = "请输入 0–12"
            return@setOnClickListener
        }
        val packages = listOf(allowed, reading, gaming)
        val invalid = packages.firstOrNull { field ->
            packageNames(field.text.toString()).any { !it.matches(Regex("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+")) }
        }
        if (invalid != null) {
            invalid.error = "请选择应用或输入包名"
            return@setOnClickListener
        }
        onSave(draft.copy(
            mediumSpeedKb = mediumKb,
            highSpeedKb = highKb,
            batteryLineColors = batteryLineColors,
            iconSpacing = dp,
            notificationApps = draft.notificationApps?.let { packageNames(allowed.text.toString()).joinToString(",") },
            notificationFlashSeconds = seconds,
            readingApps = packageNames(reading.text.toString()).joinToString(","),
            gamingApps = packageNames(gaming.text.toString()).joinToString(","),
        ))
        dialog.dismiss()
    }
}

private fun showIconOrderDialog(context: Context, current: String, onSelect: (String) -> Unit) {
    val labels = mapOf("network_speed" to "网速", "wifi" to "Wi-Fi", "mobile" to "信号",
        "bluetooth" to "蓝牙", "battery" to "电池")
    val saved = current.split(',').filter(sortableSlots::contains).distinct()
    val slots = (saved + sortableSlots).distinct().toMutableList()
    if (saved.lastOrNull() == "battery") {
        slots.remove("battery")
        slots.add("battery")
    }
    val initial = slots.toList()
    val density = context.resources.displayMetrics.density
    val padding = (20 * density).toInt()
    val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    val instructions = "长按图标名称后拖动排序。电池只能放在首位或末位。"
    val hint = TextView(context).apply {
        text = instructions
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(padding, padding / 2, padding, padding / 2)
        addView(hint)
        addView(rows)
    }
    for (slot in slots) {
        rows.addView(TextView(context).apply {
            text = labels.getValue(slot)
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            minHeight = (48 * density).toInt()
            setPadding(padding / 2, 0, padding / 2, 0)
            setOnLongClickListener { row ->
                row.startDragAndDrop(null, View.DragShadowBuilder(row), row, 0)
            }
        }, LinearLayout.LayoutParams(-1, -2))
    }
    val dialog = AlertDialog.Builder(context)
        .setTitle("图标顺序")
        .setView(ScrollView(context).apply { addView(content) })
        .setNeutralButton("恢复系统顺序") { _, _ -> onSelect("") }
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            onSelect(if (slots == initial) current else slots.joinToString(","))
        }
        .show()
    rows.setOnDragListener { _, event ->
        val row = event.localState as? View ?: return@setOnDragListener false
        if (row.parent !== rows) return@setOnDragListener false
        if (event.action == DragEvent.ACTION_DROP) {
            val from = rows.indexOfChild(row)
            val to = (0 until rows.childCount).firstOrNull { event.y < rows.getChildAt(it).bottom }
                ?: rows.childCount - 1
            if (from != to) {
                slots.add(to, slots.removeAt(from))
                rows.removeViewAt(from)
                rows.addView(row, to)
                val valid = slots.first() == "battery" || slots.last() == "battery"
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = valid
                hint.text = if (valid) instructions else "请把电池移到首位或末位"
            }
        }
        true
    }
}
