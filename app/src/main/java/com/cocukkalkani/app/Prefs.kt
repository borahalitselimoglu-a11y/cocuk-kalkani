package com.cocukkalkani.app

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

data class DnsProvider(
    val name: String,
    val primary: String,
    val secondary: String,
    val privateHost: String,
    val desc: String
)

/** Filtreli DNS seçenekleri. İlki (AdGuard Aile) varsayılan. */
val DNS_PROVIDERS = listOf(
    DnsProvider(
        "AdGuard Aile", "94.140.14.15", "94.140.15.16", "family.adguard-dns.com",
        "Reklam + izleyici + yetişkin içerik + zararlı siteler engellenir, güvenli arama zorlanır. Çocuk için önerilen."
    ),
    DnsProvider(
        "AdGuard Standart", "94.140.14.14", "94.140.15.15", "dns.adguard-dns.com",
        "Yalnızca reklam, izleyici ve kötü amaçlı siteler engellenir."
    ),
    DnsProvider(
        "Cloudflare Aile", "1.1.1.3", "1.0.0.3", "family.cloudflare-dns.com",
        "Zararlı yazılım + yetişkin içerik engellenir. Reklamları engellemez."
    )
)

object Prefs {
    private fun sp(c: Context) =
        c.applicationContext.getSharedPreferences("kalkan", Context.MODE_PRIVATE)

    // ---------- PIN (tuzlanmış SHA-256, düz metin saklanmaz) ----------
    fun hasPin(c: Context): Boolean = sp(c).contains("pin_hash")

    fun setPin(c: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp(c).edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", hash(salt, pin))
            .apply()
    }

    fun checkPin(c: Context, pin: String): Boolean {
        val saltStr = sp(c).getString("pin_salt", null) ?: return false
        val stored = sp(c).getString("pin_hash", null) ?: return false
        val salt = Base64.decode(saltStr, Base64.NO_WRAP)
        return MessageDigest.isEqual(hash(salt, pin).toByteArray(), stored.toByteArray())
    }

    private fun hash(salt: ByteArray, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        var out = salt + pin.toByteArray(Charsets.UTF_8)
        repeat(5000) {
            md.reset()
            md.update(salt)
            out = md.digest(out)
        }
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    // ---------- Uygulama izinleri ----------
    // Varsayılan: her uygulama KİLİTLİ. Yalnızca ebeveynin serbest bıraktıkları burada tutulur.
    // Sonradan yüklenen yeni uygulamalar da otomatik kilitli gelir.
    fun allowed(c: Context): Set<String> =
        sp(c).getStringSet("allowed", emptySet())?.toSet() ?: emptySet()

    fun isLocked(c: Context, pkg: String): Boolean = pkg !in allowed(c)

    fun setLocked(c: Context, pkg: String, locked: Boolean) {
        val s = allowed(c).toMutableSet()
        if (locked) s.remove(pkg) else s.add(pkg)
        sp(c).edit().putStringSet("allowed", s).apply()
    }

    fun setAllowedAll(c: Context, pkgs: Set<String>) =
        sp(c).edit().putStringSet("allowed", pkgs.toSet()).apply()

    // ---------- DNS ----------
    fun dnsEnabled(c: Context): Boolean = sp(c).getBoolean("dns_enabled", false)
    fun setDnsEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("dns_enabled", v).apply()

    fun providerIndex(c: Context): Int =
        sp(c).getInt("dns_provider", 0).coerceIn(0, DNS_PROVIDERS.size - 1)

    fun setProviderIndex(c: Context, i: Int) = sp(c).edit().putInt("dns_provider", i).apply()

    fun provider(c: Context): DnsProvider = DNS_PROVIDERS[providerIndex(c)]
}
