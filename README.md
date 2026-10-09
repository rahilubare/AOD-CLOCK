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
