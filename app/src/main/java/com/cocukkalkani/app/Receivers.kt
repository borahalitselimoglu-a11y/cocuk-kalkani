package com.cocukkalkani.app

import android.app.admin.DeviceAdminReceiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

/** Telefon yeniden başlayınca DNS korumasını tekrar açar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs.dnsEnabled(context)) return
        if (VpnService.prepare(context) != null) return // VPN izni yok
        try {
            context.startService(
                Intent(context, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_START)
            )
        } catch (_: Exception) {
        }
    }
}

/** Cihaz yöneticisi: etkinken uygulama doğrudan silinemez. */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Bu korumayı kapatırsanız Çocuk Kalkanı silinebilir hale gelir."
}
