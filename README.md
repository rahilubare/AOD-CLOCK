# StandBy Clock (Android, Kotlin + Jetpack Compose)

## Run / install
1. Android Studio (Koala or newer) -> File > Open -> this folder, let Gradle sync.
2. Phone: enable Developer options + USB debugging, plug in, press Run.
   APK file: Build > Build Bundle(s)/APK(s) > Build APK(s) -> app/build/outputs/apk/debug/app-debug.apk

## Views
Swipe left/right: Widgets | Photos | Clock. Swipe up/down inside a view.
- Widgets: two stacks (left/right). Long-press to choose widgets + write reminders.
- Photos: frame slideshow (choose photos from main screen or long-press). Swipe up/down = next/previous.
- Clock: Digital, Analog, World, Solar, Float. Long-press -> colours.
- Long-press anywhere: edit panel + full-screen timer (1-30 min).
- Double-tap: night mode (red tint + dim; also automatic in a dark room).

## Auto-start in landscape (main feature)
Main screen -> turn on "Auto-start when placed in landscape", then fix the checklist:
Display over other apps (required), Notifications, Battery unrestricted.
Works with the screen locked/off too (while charging): the screen wakes and StandBy shows over the lock screen.
Xiaomi/HyperOS: also allow "Show on Lock screen" in the app's Other permissions.
Then: plug in the charger, stand the phone sideways and keep it still ~2.5 s -> StandBy opens.
To work without the charger: turn OFF "Only while charging" (costs extra battery, esp. with the screen locked).
Pick the phone up (leave landscape) -> StandBy closes. Tuning constants: OrientationService.kt (companion).
Xiaomi/Oppo/Vivo: also allow "Display pop-up windows while running in background" + Autostart in the app's settings.


## Extend
- New clock page: ClockStyles.kt.   New widget: Widgets.kt (WidgetCatalog).
- Solar clock latitude: SOLAR_LATITUDE in SolarClock.kt.

## Known limits
- Auto-start only triggers while the screen is on; the screensaver (DreamService) was removed because HyperOS disables it.
- OEM skins may hide screensaver settings.

## Release build (signed with your own key)
1. Create a key once (keep the .jks file and passwords safe, you need the same key for every update):
   keytool -genkeypair -v -keystore standby-release.jks -alias standby -keyalg RSA -keysize 2048 -validity 10000
2. Create keystore.properties next to settings.gradle.kts:
   storeFile=standby-release.jks
   storePassword=YOUR_PASSWORD
   keyAlias=standby
   keyPassword=YOUR_PASSWORD
3. .\gradlew assembleRelease  ->  app\build\outputs\apk\release\app-release.apk
Fonts: Nunito (SIL Open Font License) in res/font.

## Reliability notes
- Watcher: foreground service; the main screen shows "Watcher is running / not running" with a Restart button; Quick Settings tile restarts it too.
- Android blocks background launches: if the direct launch fails, a full-screen notification opens the clock instead.
- Opens only while the phone is LOCKED by default (so it never covers a video). Setting: "Also open when the phone is unlocked".
- Calibration: any stand angle, any device. Sensitivity: Relaxed / Balanced / Strict.
- Lock screen privacy: reminders, photos, calendar and notification text are hidden while locked (setting).
- Timer uses a system alarm clock entry: rings on the alarm volume even if the app was closed.
- Photos are copied into app storage (resized). Weather: Open-Meteo, only the city name is sent.
- Not covered: UI text is English only; untested on brands other than the author's phone.
