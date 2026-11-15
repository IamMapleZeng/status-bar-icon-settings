# Status Bar Icon Settings

[简体中文](README.md) · **English**

A Kotlin module for the Xiaomi 15 Ultra running HyperOS 4, SukiSU Ultra, and official LSPosed 2.2.1 (7912). The settings screen offers independent controls for the mobile network, Wi-Fi, no-SIM, and battery icons, plus a battery line at the top of the screen and detailed logging. All switches default to off. The mobile network switch controls the entire cellular icon group, including signal bars and any 4G or 5G labels rendered by the same component. The module changes how SystemUI displays these icons. Android continues to manage network connections.

## Installation

1. Confirm that SukiSU Ultra and your Zygisk implementation are running. In LSPosed Manager, verify that official LSPosed 2.2.1 (7912) is active. Keep LSPosed safe mode available and make sure you have either an authorized USB debugging connection or access to SukiSU Manager for recovery.
2. Build the debug APK as described below and install `app/build/outputs/apk/debug/app-debug.apk`. You can also run `adb install -r app/build/outputs/apk/debug/app-debug.apk`. You can install over an existing version when both the package name and signature match. The LSPosed repository version uses `io.github.iammaplezeng.statusbaricons`. When switching from `dev.statusbar.networkicons`, disable the old module in LSPosed and configure the new one.
3. In LSPosed Manager, enable **Status Bar Icon Settings** under **Modules** and select only **System UI** (`com.android.systemui`) in its scope. The module hooks only the main process of that package.
4. After enabling or upgrading the module, reload System UI once from LSPosed or reboot the phone. Open **Status Bar Icon Settings** from the launcher. The connection status should show the framework version and API 102. If it is waiting for the service, verify that the module is enabled, its scope is saved, and the framework is active after restarting.
5. Run the checks below. The app saves switches and colors automatically. SystemUI updates the visible status bar when it receives the new settings.

## On-device verification

Start with the phone unlocked and check the normal top status bar. Use a working SIM and a stable Wi-Fi connection. If your ROM hides the 4G or 5G label while connected to Wi-Fi, temporarily disconnect Wi-Fi and use mobile data to check the cellular icon separately.

| Hide mobile network | Hide Wi-Fi | Expected result |
| --- | --- | --- |
| Off | Off | Same display as before enabling the module |
| Off | On | Wi-Fi icon disappears and cellular icons follow system behavior |
| On | Off | Cellular icons disappear and Wi-Fi follows system behavior |
| On | On | Both icon groups disappear |
| Off | Off | Both groups return to their original system behavior |

Turn off the other two hide switches when testing mobile network and Wi-Fi. With no SIM installed, test the following controls separately.

| Hide no-SIM icon | Hide battery | Expected result |
| --- | --- | --- |
| Off | Off | Both the no-SIM cross and the battery icon with its percentage are visible |
| On | Off | Only the no-SIM cross disappears |
| Off | On | Only the battery icon and its percentage disappear |
| On | On | Both icons disappear |

Check the network speed indicator on the left for each combination. With the battery icon hidden, connect and disconnect the charger and watch for the icon to reappear as the battery reading updates. Turn off both switches and verify that the icons return.

Verify that Wi-Fi and mobile data still connect and that the icon settings apply after switching networks. On dual-SIM devices, check both cellular icons. Inspect the lock screen, notification shade, and Control Center for related changes. The network icon controls target the top status bar. After hiding Wi-Fi or the no-SIM icon, check for gaps on the right, then disable the switch and verify that the icon returns. Battery settings may affect the lock screen or Control Center if they share the same battery view.

### Battery line

1. Enable **Battery line** and open the color picker. The default color is teal. Drag to select hue, saturation, and brightness, or enter a `#RRGGBB` value.
2. Look for a 2 dp line extending from the left edge at the top of the status bar. At around 57% charge, it should span about 57% of the bar. The line updates automatically as the battery charges or discharges.
3. You can also hide the battery icon and use the line as the battery indicator. Pick another color and verify that it applies.
4. Rotate the phone and open a full-screen app. The line follows the status bar's visibility. Turn off the battery line and verify that it disappears.

### Charging animations

With the battery line enabled, open the charging animation setting. Options include Off, Gradient Sweep, Wave, Comet Trail, Dual Sweep, and Breathing Glow. The default is Off.

- Gradient Sweep moves a brighter, thicker highlight along the 2 dp line. The highlight reaches about 5 dp at its thickest point.
- Wave moves rounded peaks and troughs along the line. The legacy ECG setting automatically maps to Wave.
- Comet Trail sweeps a series of fading dots along the line.
- Dual Sweep moves two highlights toward each other from opposite ends.
- Breathing Glow periodically thickens and thins the line.
- Each animation stays within the current battery level. The solid line returns when the charger is unplugged, the battery is full, or charging pauses.

Plug in below 100% and confirm that the system reports charging. Check that the animation starts and that the line becomes static once unplugged. Switch animations while charging and verify that the new effect applies immediately.

### Status bar clock

Seconds are off by default. The clock color follows the system by default and has its own color picker. After changing these settings, check that seconds update once per second and that the custom color applies on both light and dark backgrounds. Turn off seconds or select the system color to restore the corresponding default.

## Network speed threshold

Enable real-time network speed in HyperOS first. Open the network speed threshold setting and enter an integer in KB/s. A threshold of 50 shows the indicator above 50 KB/s and hides it at or below 50 KB/s. Select **Follow system** to clear the threshold. The setting uses the speed reading already displayed by the system.

## Advanced status bar settings

Open the status bar options from **Advanced options**, make your changes, and tap **OK**. Reload System UI once after upgrading.

### Features introduced in 0.9.12

| Feature | How to use and test it |
| --- | --- |
| Battery percentage | Enable the battery percentage option in advanced settings. While unlocked, tap an empty part of the top status bar to briefly show the current charge next to the clock, for example `57%`. The battery line has its own switch. |
| Next calendar event | Enable the countdown and grant calendar permission. Create a timed event starting within 30 minutes in the system calendar, then unlock and check the minutes beside the clock. Event titles are hidden by default and can be enabled in advanced options. Calendar data refreshes roughly every 30 seconds. |
| Raindrop animation | Enable weather, rain alerts, and the raindrop effect. With the screen on, a raindrop animation plays once when a rain alert first appears. |
| Full-charge animation | With the screen on, charge from below 100% to 100%. Particles along the top form a star and fade out. The animation plays at most once per charger connection. |
| Preview | Open **Preview** from advanced options or from the advanced settings dialog to try the notification flash, charging animation, full-charge animation, and raindrops. The dialog uses the colors and durations currently being edited. |
| Unread notification dot | Enable the dot and receive a new notification. A dot appears beside the clock. It goes out when the last tracked notification is opened or dismissed. It tracks system notifications. With the notification icon allowlist enabled, it tracks only apps on the list. |

### Notification flash

Under **Notifications and app profiles**, enable the status bar flash for incoming notifications. Set a duration from 1 to 15 seconds (default 3) and choose the gradient's start and end colors. When the screen is on and the top status bar is visible, an incoming notification triggers a gradient pulse from left to right. Ongoing notifications, group summaries, and notifications the system suppresses in the status bar are skipped. The switch defaults to off.

With the screen on, send a message from another device. Check that the flash uses the chosen colors and ends after the configured duration. Disable the switch, send another message, and verify that the flash stays off.

### Options introduced in 0.9.8

| Option | How to use and test it |
| --- | --- |
| Rain alert | Enable weather display as well. If Xiaomi Weather updated within the last hour and the chance of rain in the next hour reaches 60%, the far right shows the rain probability. The wording follows the selected weather language. At other times it displays regular weather. The weather source checked for this feature provides hourly probabilities. |
| Track title on headphones | Connect headphones and switch to a new song. If the player supplies a title and audio is routed to the headphones, the title appears beside the clock for about four seconds. The title is hidden on the lock screen. |
| System timer progress line | Start a timer longer than 30 seconds in Xiaomi Clock and return to the launcher. The blue line at the top shrinks as time runs out and turns orange for the last 10 seconds. Canceling or finishing the timer restores the original battery line setting. This relies on the system clock's countdown notification. |
| Hourly star | Keep the screen on at any hour from 07:00 through 21:00. A star sweeps across the top of the screen on the hour. |
| Charging particles | Keep the screen on while charging past 80% or reaching 100%. A short particle effect appears at the battery level. Turning on the feature after reaching a threshold does not replay the effect. |
| Privacy labels | Open an app that is using the camera or microphone and check the text beside the system privacy indicator. Indicator positions and data sources can vary by ROM. |

These options default to off. Once SystemUI has loaded this version, saved switches typically take effect within a second.

- **Weather** is off by default. It shows the current temperature from Xiaomi Weather at the far right, when data is available. Long-press the display to open Xiaomi Weather. Add a city and refresh it in Xiaomi Weather first. Weather conditions are off by default and support the system language, Simplified Chinese, or English. Temperature uses the unit set in Xiaomi Weather. Reading and gaming profiles hide weather.
- **Network speed colors** dim low speeds like the clock seconds, default to orange for medium speeds and red for high speeds. Both colors can be customized. The medium and high ranges start at 50 and 1024 KB/s by default. Both thresholds can be changed in advanced settings. With smoothing enabled, the indicator appears above the threshold and hides after the reading stays at or below 60% of the threshold for at least three seconds. Timing depends on the system's sampling interval.
- **Battery level colors** use red below 30%, orange from 30% through 60%, and green above 60%. These colors take precedence over the custom line color and also set the base color for charging animations. Turning this option off restores the previously selected color.
- **Clock options** include the day of the week and dimmed seconds. When gestures are enabled, tap to toggle seconds temporarily or long-press to open alarms. Seconds dimming has its own setting.
- Open **Icon order** and long-press an icon name to drag it into place. You can also restore the system order. The battery can sit at either end of the group. Spacing ranges from 0 to 12 dp, and network speed can appear next to the clock.
- **Notification icon allowlist** applies to the top status bar. An empty allowlist hides all top status bar notification icons when enabled.
- **App profiles** keep the clock and battery line in reading mode and retain the system network speed indicator in gaming mode. Gaming takes precedence when an app is in both profiles. The regular settings return when switching to other apps. Real-time network speed must be enabled in the system.

## Logs and recovery

Search for `NetworkStatusIcons` in the LSPosed module logs, or run these commands on an authorized computer.

```text
adb logcat -d -s NetworkStatusIcons
adb logcat -b crash -d
adb shell getprop ro.build.fingerprint
adb shell dumpsys package com.android.systemui
```

- `SystemUI loaded` indicates successful injection. `Live settings received` indicates that SystemUI received updated settings.
- `Battery line attached` indicates that the line is attached to the status bar. Check it with `adb logcat -d -s NetworkStatusIcons`.
- If you see `path unavailable` or `path incompatible`, record the ROM version, region, SystemUI version, and relevant logs.
- To roll back, disable the module in LSPosed Manager and reboot. If SystemUI keeps crashing and you cannot open the manager, run `adb uninstall io.github.iammaplezeng.statusbaricons` from an authorized computer. After a successful uninstall, run `adb reboot`. If you can only access SukiSU Manager on the phone, temporarily disable LSPosed and reboot. This also suspends the other LSPosed modules.

## Building

Use JDK 17, Android SDK Platform 37.0 (`platforms;android-37.0`), and Build Tools 36.0.0. Open the project root in Android Studio. For command-line builds, set `ANDROID_HOME` to the SDK path or create a `local.properties` file with `sdk.dir` at the project root.

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

Use `./gradlew` on macOS or Linux. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. The project includes the Gradle 9.5.1 Wrapper and uses the Kotlin support built into AGP 9.2.1. The Android compile SDK and the LSPosed API have independent version numbers. The app uses Android `minSdk` 28 and `targetSdk` 34.

## Compatibility

- The framework package from the official LSPosed site is labeled `v2.2.1 (7912)`, and `getApiVersion()` in its `framework.dex` returns 102. The project uses stable `io.github.libxposed:api:102.0.0` at compile time and packages `service:102.0.0` with the app. The module entry point, minimum and target APIs, and fixed scope are declared in `META-INF/xposed/`. The framework's remote preferences share settings across processes.
- The settings screen writes to LSPosed remote preferences. SystemUI listens for changes and updates views on the main thread. The module identifies network icons through the SystemUI icon manager and the battery through Xiaomi's battery view. It records original visibility, excludes hidden network icons from status bar measurement, and restores system visibility when the hide switches are cleared.
- The battery line attaches to the phone status bar's drawing layer and listens for system battery broadcasts. It does not receive touch events.
- The hooks draw on community implementations for HyperOS SystemUI. Xiaomi may change icon paths across regions, OTAs, or SystemUI versions. Use the checks above to verify injection, cross-process settings, and icon behavior on a Xiaomi 15 Ultra.

### References

- [Official LSPosed site and downloads](https://lsposed.org/)
- [Official LSPosed release channel](https://t.me/s/LSPosed)
- [Modern Xposed API module documentation](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API)
- [Stable libxposed artifacts and source packages](https://repo.maven.apache.org/maven2/io/github/libxposed/)
- [HyperCeiler HyperOS Wi-Fi blocklist implementation](https://github.com/ReChronoRain/HyperCeiler/blob/0830c5875570e66ae576b9276b0e57033cb6601a/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/icon/all/StatusBarIcon.java)
- [HyperCeiler HyperOS 4 cellular binder implementation](https://github.com/ReChronoRain/HyperCeiler/blob/0830c5875570e66ae576b9276b0e57033cb6601a/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/mobile/MobilePublicHookV.kt)
- [HyperCeiler Xiaomi battery view reference](https://github.com/ReChronoRain/HyperCeiler/blob/0830c5875570e66ae576b9276b0e57033cb6601a/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/icon/all/HideBatteryIcon.kt)
- [AOSP PhoneStatusBarView](https://android.googlesource.com/platform/frameworks/base/+/main/packages/SystemUI/src/com/android/systemui/statusbar/phone/PhoneStatusBarView.java)
- [ClockSeconds live update reference](https://github.com/kelebao/ClockSeconds)
