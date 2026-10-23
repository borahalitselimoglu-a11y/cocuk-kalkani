package com.cocukkalkani.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

/** Tam sürüm: DNS kalkanı uygulama içinden (yerel VPN ile) açılıp kapatılır. */
object DnsControl {
    const val SUPPORTED = true

    fun isRunning(): Boolean = DnsVpnService.isRunning

    /** null değilse önce kullanıcıdan VPN izni alınmalı. */
    fun consentIntent(c: Context): Intent? = VpnService.prepare(c)

    fun start(c: Context) {
        Prefs.setDnsEnabled(c, true)
        c.startService(Intent(c, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_START))
    }

    fun stop(c: Context) {
        Prefs.setDnsEnabled(c, false)
        c.startService(Intent(c, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_STOP))
    }
}

/** Telefon yeniden başlayınca DNS kalkanını tekrar açar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs.dnsEnabled(context)) return
        if (VpnService.prepare(context) != null) return // VPN izni yok
        try {
            DnsControl.start(context)
        } catch (_: Exception) {
        }
    }
}
