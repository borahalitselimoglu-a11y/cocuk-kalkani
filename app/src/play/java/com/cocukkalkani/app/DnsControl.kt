package com.cocukkalkani.app

import android.content.Context
import android.content.Intent

/** Play sürümü: uygulama DNS'e dokunmaz; panelde yalnızca Özel DNS ayarı için yazılı rehber gösterilir. */
object DnsControl {
    const val SUPPORTED = false

    fun isRunning(): Boolean = false
    fun consentIntent(c: Context): Intent? = null
    fun start(c: Context) {}
    fun stop(c: Context) {}
}
