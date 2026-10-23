package com.cocukkalkani.app

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent

/**
 * Ön plana gelen her uygulamayı izler. Ebeveynin kilitlediği bir uygulama
 * açılırsa üstüne LockActivity (PIN ekranı) getirir.
 */
class AppLockService : AccessibilityService() {

    companion object {
        /** PIN girilerek geçici olarak açılmış uygulama. Başka uygulamaya geçilince veya ekran kapanınca sıfırlanır. */
        @Volatile
        var unlockedPackage: String? = null

        fun allowOnce(pkg: String?) {
            unlockedPackage = pkg
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            unlockedPackage = null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        if (pkg == packageName) return               // kendi ekranlarımız
        if (!isActivity(pkg, cls)) return            // klavye, bildirim paneli vb. yok say

        // Başka bir uygulamaya geçildiyse önceki geçici izin biter
        if (pkg != unlockedPackage) unlockedPackage = null

        if (pkg in Prefs.blocked(this) && pkg != unlockedPackage) {
            val i = Intent(this, LockActivity::class.java)
                .putExtra(LockActivity.EXTRA_PKG, pkg)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            startActivity(i)
        }
    }

    private fun isActivity(pkg: String, cls: String): Boolean = try {
        packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
        true
    } catch (e: Exception) {
        false
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
