package com.cocukkalkani.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/** Cihaz yöneticisi: etkinken uygulama doğrudan silinemez. */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Bu korumayı kapatırsanız Çocuk Kalkanı silinebilir hale gelir."
}
