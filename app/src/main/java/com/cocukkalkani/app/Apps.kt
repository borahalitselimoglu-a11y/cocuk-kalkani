package com.cocukkalkani.app

import android.content.Context
import android.content.Intent
import java.text.Normalizer
import java.util.Locale

object Apps {

    /**
     * Kilitlenebilir uygulamalar: ana ekranda simgesi olan her uygulama.
     * Ana ekran (launcher) ve Çocuk Kalkanı'nın kendisi asla kilitlenmez.
     */
    fun lockablePackages(c: Context): Set<String> {
        val pm = c.packageManager
        val launcher = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.activityInfo.packageName }.toSet()
        val home = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        ).map { it.activityInfo.packageName }.toSet()
        return launcher - home - c.packageName
    }

    /** Gelen arama ve çalan alarm ekranlarının üstüne kilit çıkmasın. */
    private val EXEMPT_SCREENS = listOf("incall", "alarmalert", "alarmactivity", "emergency")

    fun isExemptScreen(cls: String): Boolean {
        val c = cls.lowercase(Locale.ROOT)
        return EXEMPT_SCREENS.any { it in c }
    }

    /** Aramada Türkçe karakter farkını yok say: "Çocuk" = "cocuk", "Instagram" = "ınstagram". */
    fun fold(s: String): String =
        Normalizer.normalize(s.lowercase(Locale("tr", "TR")).replace('ı', 'i'), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
