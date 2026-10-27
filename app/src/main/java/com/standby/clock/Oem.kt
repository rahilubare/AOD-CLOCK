package com.standby.clock

import android.os.Build

/** Phone-brand specific advice. Brands hide background limits in different menus, so we show the right words. */
class Oem(val name: String, val slug: String?, val steps: String) {
    companion object {
        /** null for phones without extra background limits (Google Pixel and similar). */
        fun detect(): Oem? {
            val m = Build.MANUFACTURER.lowercase()
            return when {
                m == "google" -> null
                m in listOf("xiaomi", "redmi", "poco") -> Oem("Xiaomi", "xiaomi",
                    "Open this app's permissions and allow: pop-up windows while running in the background, Show on lock screen and Autostart. Then lock the app in Recents.")
                m == "samsung" -> Oem("Samsung", "samsung",
                    "Settings > Battery > Background usage limits: add StandBy Clock to Never sleeping apps. Set the app's battery to Unrestricted.")
                m == "oppo" -> Oem("OPPO", "oppo", "In the app's Battery settings allow Auto launch and Background activity. Lock the app in Recents.")
                m == "realme" -> Oem("realme", "realme", "In the app's Battery settings allow Auto launch and Background activity. Lock the app in Recents.")
                m == "oneplus" -> Oem("OnePlus", "oneplus", "In the app's Battery settings allow Background activity and Auto launch. Lock the app in Recents.")
                m == "vivo" || m == "iqoo" -> Oem("vivo", "vivo", "In the app's Battery settings allow Autostart and High background power consumption.")
                m == "huawei" -> Oem("Huawei", "huawei", "Open App launch, turn off Manage automatically, and allow Auto-launch, Secondary launch and Run in background.")
                m == "honor" -> Oem("Honor", "honor", "Open App launch, turn off Manage automatically, and allow Auto-launch, Secondary launch and Run in background.")
                m == "motorola" -> Oem("Motorola", "motorola", "Set the app's battery to Unrestricted and allow background activity.")
                else -> Oem(Build.MANUFACTURER, null,
                    "If the clock stops opening, look for Autostart, Background activity or Unrestricted battery in this app's settings, and lock the app in Recents.")
            }
        }
    }
}
