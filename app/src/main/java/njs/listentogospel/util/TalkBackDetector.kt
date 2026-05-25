package njs.listentogospel.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

private val TALKBACK_PACKAGES = setOf(
    "com.google.android.marvin.talkback",
    "com.samsung.android.accessibility.talkback",
    "com.android.talkback"
)

/**
 * True only when a spoken-feedback accessibility service such as TalkBack is
 * enabled in system settings. Does not use [AccessibilityManager.isTouchExplorationEnabled],
 * which can stay true on some devices after TalkBack is turned off.
 */
fun Context.isTalkBackServiceEnabled(): Boolean {
    val manager = getSystemService(AccessibilityManager::class.java) ?: return false
    if (!manager.isEnabled) return false

    val spokenServices = manager.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_SPOKEN
    )
    if (spokenServices.any { service ->
            val pkg = service.resolveInfo.serviceInfo.packageName
            pkg in TALKBACK_PACKAGES || pkg.contains("talkback", ignoreCase = true)
        }
    ) {
        return true
    }

    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { service ->
            val pkg = service.resolveInfo.serviceInfo.packageName
            pkg in TALKBACK_PACKAGES || pkg.contains("talkback", ignoreCase = true)
        }
}
