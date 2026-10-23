package com.cocukkalkani.app

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.VpnService
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.text.Collator
import java.util.Locale

/** Ebeveyn paneli. Her açılışta PIN ister. */
class MainActivity : Activity() {

    private data class AppItem(val pkg: String, val label: String, val icon: Drawable, val suggested: Boolean)

    private companion object {
        const val REQ_VPN = 10
        const val RELOCK_AFTER_MS = 60_000L
    }

    private lateinit var lockStatus: TextView
    private lateinit var dnsStatus: TextView
    private lateinit var dnsToggle: Button
    private lateinit var providerBtn: Button
    private lateinit var adminStatus: TextView
    private val adapter = AppAdapter()
    private val apps = mutableListOf<AppItem>()

    private var authed = false
    private var stoppedAt = 0L
    private var gateDialog: AlertDialog? = null

    // ------------------------------------------------------------------ yaşam döngüsü

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val list = ListView(this).apply { setPadding(0, 0, 0, dp(24)); clipToPadding = false }
        list.addHeaderView(buildHeader(), null, false)
        list.adapter = adapter
        setContentView(list)
        loadApps()
    }

    override fun onStart() {
        super.onStart()
        if (stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MS) authed = false
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        gate()
    }

    // ------------------------------------------------------------------ PIN kapısı

    private fun gate() {
        if (gateDialog?.isShowing == true) return
        when {
            !Prefs.hasPin(this) -> setupPin(firstTime = true)
            !authed -> askPin()
        }
    }

    private fun askPin() {
        gateDialog = pinDialog("Ebeveyn PIN'i", null, "Çık", { finish() }) { pin ->
            if (Prefs.checkPin(this, pin)) {
                authed = true
            } else {
                toast("Hatalı PIN")
                askPin()
            }
        }
    }

    private fun setupPin(firstTime: Boolean) {
        val neg = if (firstTime) "Çık" else "İptal"
        val onNeg: () -> Unit = { if (firstTime) finish() }
        gateDialog = pinDialog(
            if (firstTime) "Hoş geldiniz" else "PIN değiştir",
            "En az 4 haneli yeni bir ebeveyn PIN'i belirleyin. Çocuğunuzun bilmediği bir PIN seçin.",
            neg, onNeg
        ) { first ->
            if (first.length < 4) {
                toast("PIN en az 4 haneli olmalı")
                setupPin(firstTime); return@pinDialog
            }
            gateDialog = pinDialog("PIN'i tekrar girin", null, neg, onNeg) { second ->
                if (first != second) {
                    toast("PIN'ler eşleşmedi")
                    setupPin(firstTime)
                } else {
                    Prefs.setPin(this, first)
                    authed = true
                    toast("PIN kaydedildi")
                    if (firstTime) showSetupGuide()
                }
            }
        }
    }

    private fun showSetupGuide() {
        AlertDialog.Builder(this)
            .setTitle("Kurulum adımları")
            .setMessage(
                "1) Uygulama kilidini açın (Erişilebilirlik izni).\n" +
                    "2) Reklam/DNS korumasını açın.\n" +
                    "3) Silme korumasını etkinleştirin.\n" +
                    "4) Aşağıdan çocuğun açmasını istemediğiniz uygulamaları işaretleyin. " +
                    "Ayarlar ve Play Store'u kilitlemeniz, korumaların kapatılmasını önler."
            )
            .setPositiveButton("Anladım", null)
            .show()
    }

    // ------------------------------------------------------------------ üst panel

    private fun buildHeader(): View {
        lockStatus = text("", 15f, bold = true)
        dnsStatus = text("", 15f, bold = true)
        adminStatus = text("", 15f, bold = true)
        dnsToggle = button("") { toggleDns() }
        providerBtn = button("") { chooseProvider() }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(8))

            addView(text(getString(R.string.app_name), 24f, bold = true))

            addView(card(
                text("1 · Uygulama kilidi", 13f),
                lockStatus,
                text("Ayarlar › Erişilebilirlik › İndirilen uygulamalar › Çocuk Kalkanı → Aç", 13f),
                button("Erişilebilirlik ayarlarını aç") {
                    openTrusted(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            ))

            addView(card(
                text("2 · Reklam ve zararlı site koruması (DNS)", 13f),
                dnsStatus,
                providerBtn,
                dnsToggle,
                text("Kalıcı olsun: Ayarlar › VPN › Çocuk Kalkanı › \"Her zaman açık VPN\" açın. " +
                    "\"VPN olmadan bağlantıları engelle\" seçeneğini AÇMAYIN.", 13f),
                button("VPN ayarlarını aç") { openTrusted(Intent(Settings.ACTION_VPN_SETTINGS)) },
                button("Alternatif: Özel DNS (VPN'siz)") { showPrivateDnsHelp() }
            ))

            addView(card(
                text("3 · Silme koruması", 13f),
                adminStatus,
                button("Silme korumasını etkinleştir") { enableAdmin() }
            ))

            addView(card(
                text("Güvenlik", 13f),
                button("PIN değiştir") { setupPin(firstTime = false) }
            ))

            addView(text("Kilitlenecek uygulamalar", 18f, bold = true).apply {
                setPadding(0, dp(16), 0, 0)
            })
            addView(text("İşaretlediğiniz uygulamalar açılınca PIN istenir.", 13f))
        }
    }

    private fun card(vararg views: View): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.parseColor("#F1F8E9"))
        setPadding(dp(14), dp(10), dp(14), dp(10))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) }
        views.forEach { addView(it) }
    }

    private fun refreshStatus() {
        lockStatus.text = if (isAccessibilityOn()) "✅ Uygulama kilidi AÇIK"
        else "⚠️ Uygulama kilidi KAPALI"

        val p = Prefs.provider(this)
        val running = DnsVpnService.isRunning
        dnsStatus.text = if (running) "✅ DNS koruması AÇIK" else "⚠️ DNS koruması KAPALI"
        dnsToggle.text = if (running) "Korumayı kapat" else "Korumayı aç"
        providerBtn.text = "Filtre: ${p.name} (değiştir)"

        adminStatus.text = if (isAdminOn()) "✅ Silme koruması AÇIK" else "⚠️ Silme koruması KAPALI"
    }

    private fun refreshSoon() {
        window.decorView.postDelayed({ refreshStatus() }, 800)
    }

    // ------------------------------------------------------------------ uygulama kilidi

    private fun isAccessibilityOn(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val cn = ComponentName(this, AppLockService::class.java)
        return enabled.split(':').any {
            it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
        }
    }

    /** Ebeveynin buradan açtığı Ayarlar ekranı (kilitli olsa bile) bir kez PIN sormadan açılır. */
    private fun openTrusted(intent: Intent): Boolean = try {
        AppLockService.allowOnce(intent.resolveActivity(packageManager)?.packageName)
        startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    // ------------------------------------------------------------------ DNS

    private fun toggleDns() {
        if (DnsVpnService.isRunning) {
            Prefs.setDnsEnabled(this, false)
            startService(Intent(this, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_STOP))
            refreshSoon()
            return
        }
        val consent = VpnService.prepare(this)
        if (consent != null) {
            startActivityForResult(consent, REQ_VPN)
        } else {
            startDns()
        }
    }

    private fun startDns() {
        Prefs.setDnsEnabled(this, true)
        startService(Intent(this, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_START))
        refreshSoon()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN) {
            if (resultCode == RESULT_OK) startDns() else toast("VPN izni verilmedi")
        }
    }

    private fun chooseProvider() {
        val names = DNS_PROVIDERS.map { "${it.name}\n${it.desc}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("DNS filtresi")
            .setSingleChoiceItems(names, Prefs.providerIndex(this)) { d, which ->
                Prefs.setProviderIndex(this, which)
                d.dismiss()
                if (DnsVpnService.isRunning) startDns() // yeni sağlayıcıyla yeniden başlat
                refreshSoon()
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun showPrivateDnsHelp() {
        val host = Prefs.provider(this).privateHost
        AlertDialog.Builder(this)
            .setTitle("Özel DNS (Android 9+)")
            .setMessage(
                "VPN kullanmak istemezseniz Android'in kendi şifreli DNS ayarını kullanabilirsiniz:\n\n" +
                    "Ayarlar › Ağ ve internet › Özel DNS › \"Özel DNS sağlayıcı ana makine adı\" seçip şunu yazın:\n\n" +
                    "$host\n\n" +
                    "Bu yöntem pil dostudur ama çocuk Ayarlar'a girebilirse kapatabilir; bu yüzden Ayarlar'ı kilitleyin. " +
                    "Menü adları markaya göre biraz değişebilir."
            )
            .setPositiveButton("Adı kopyala ve Ayarlar'ı aç") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("DNS", host))
                toast("Kopyalandı: $host")
                if (!openTrusted(Intent("android.settings.PRIVATE_DNS_SETTINGS"))) {
                    openTrusted(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    // ------------------------------------------------------------------ silme koruması

    private fun adminComponent() = ComponentName(this, AdminReceiver::class.java)

    private fun isAdminOn(): Boolean =
        (getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .isAdminActive(adminComponent())

    private fun enableAdmin() {
        if (isAdminOn()) {
            toast("Zaten etkin")
            return
        }
        val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent())
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.admin_description)
            )
        openTrusted(i)
    }

    // ------------------------------------------------------------------ uygulama listesi

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val settingsPkg = Intent(Settings.ACTION_SETTINGS).resolveActivity(pm)?.packageName
            val suggested = setOfNotNull(settingsPkg, "com.android.vending")
            val collator = Collator.getInstance(Locale("tr", "TR"))

            val items = pm.queryIntentActivities(launcher, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != packageName }
                .mapNotNull { pkg ->
                    try {
                        val ai = pm.getApplicationInfo(pkg, 0)
                        AppItem(pkg, pm.getApplicationLabel(ai).toString(), pm.getApplicationIcon(ai), pkg in suggested)
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedWith(compareBy<AppItem> { !it.suggested }.thenComparator { a, b ->
                    collator.compare(a.label, b.label)
                })

            runOnUiThread {
                apps.clear()
                apps.addAll(items)
                adapter.notifyDataSetChanged()
            }
        }.start()
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(position: Int): Any = apps[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(8), dp(16), dp(8))
                addView(ImageView(context), LinearLayout.LayoutParams(dp(40), dp(40)))
                addView(TextView(context).apply {
                    textSize = 16f
                    setPadding(dp(12), 0, dp(8), 0)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(CheckBox(context))
            }
            val item = apps[position]
            val iv = row.getChildAt(0) as ImageView
            val tv = row.getChildAt(1) as TextView
            val cb = row.getChildAt(2) as CheckBox

            iv.setImageDrawable(item.icon)
            tv.text = if (item.suggested) "${item.label}  (kilitlenmesi önerilir)" else item.label
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = item.pkg in Prefs.blocked(this@MainActivity)
            cb.setOnCheckedChangeListener { _, checked ->
                Prefs.setBlocked(this@MainActivity, item.pkg, checked)
            }
            row.setOnClickListener { cb.isChecked = !cb.isChecked }
            return row
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
