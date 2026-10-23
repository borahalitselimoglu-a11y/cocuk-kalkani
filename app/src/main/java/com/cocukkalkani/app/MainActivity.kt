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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.VpnService
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.text.Editable
import android.text.TextWatcher
import android.widget.FrameLayout
import android.widget.EditText
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
    private val apps = mutableListOf<AppItem>()      // tüm uygulamalar
    private val shown = mutableListOf<AppItem>()     // aramaya göre görünenler
    private var query = ""
    private lateinit var list: ListView
    private lateinit var countText: TextView

    private var authed = false
    private var stoppedAt = 0L
    private var gateDialog: AlertDialog? = null

    // ------------------------------------------------------------------ yaşam döngüsü

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Palette.PAGE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        list = ListView(this).apply {
            setPadding(0, 0, 0, dp(32))
            clipToPadding = false
            divider = null
            dividerHeight = 0
            selector = ColorDrawable(Color.TRANSPARENT)
            cacheColorHint = Color.TRANSPARENT
        }
        list.addHeaderView(buildHeader(), null, false)
        list.adapter = adapter

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Sayfa zemini: krem rengi + soluk konfeti noktaları
            background = LayerDrawable(arrayOf(
                ColorDrawable(Palette.PAGE),
                DotsDrawable(Palette.CONFETTI, dp(34).toFloat(), dp(3).toFloat(), 150)
            ))
            addView(buildHero(), LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(12), dp(10), dp(12), dp(4)) })
            addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        })
        loadApps()
    }

    /** Üstte sabit duran renkli başlık + arama kutusu (uygulamanın adı yalnızca burada). */
    private fun buildHero(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = LayerDrawable(arrayOf(
            gradient(Palette.HERO, 28),
            DotsDrawable(intArrayOf(Color.WHITE), dp(26).toFloat(), dp(2).toFloat(), 60)
        ))
        clipToOutline = true
        setPadding(dp(18), dp(14), dp(18), dp(16))

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(text("🛡️", 34f))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)
                addView(text(getString(R.string.app_name), 24f, bold = true, color = Color.WHITE).apply {
                    setPadding(0, 0, 0, 0)
                })
                addView(text("Güvenli ve eğlenceli telefon 🌈✨", 13f, color = Color.parseColor("#F3E9FF")).apply {
                    setPadding(0, 0, 0, 0)
                })
            })
        })

        val search = EditText(context).apply {
            hint = "🔍  Uygulama ara… (ör. youtube, oyun)"
            setHintTextColor(Palette.INK_SOFT)
            setTextColor(Palette.INK)
            isSingleLine = true
            textSize = 16f
            background = round(Color.WHITE, 50)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(e: Editable?) {
                    query = e?.toString().orEmpty().trim()
                    applyFilter()
                    if (query.isNotEmpty()) list.setSelection(list.headerViewsCount)
                }
            })
            setOnFocusChangeListener { _, has -> if (has) list.setSelection(list.headerViewsCount) }
        }
        addView(search, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })
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
                    "4) Tüm uygulamalar başta KİLİTLİDİR. Listeden çocuğun serbestçe kullanabileceği " +
                    "uygulamaların işaretini kaldırın. Ayarlar ve Play Store kilitli kalmalı."
            )
            .setPositiveButton("Anladım", null)
            .show()
    }

    // ------------------------------------------------------------------ üst panel

    private fun buildHeader(): View {
        lockStatus = chip()
        dnsStatus = chip()
        adminStatus = chip()
        dnsToggle = button("", Tones.GREEN.accent) { toggleDns() }
        providerBtn = button("", Color.WHITE, Tones.GREEN.accent) { chooseProvider() }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(12), dp(8))

            addView(card("🔒", "Uygulama kilidi", Tones.BLUE,
                lockStatus,
                text("Ayarlar › Erişilebilirlik › İndirilen uygulamalar › Çocuk Kalkanı → Aç", 13f, color = Palette.INK_SOFT),
                button("⚙️  Erişilebilirlik ayarlarını aç", Tones.BLUE.accent) {
                    openTrusted(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            ))

            addView(card("🧹", "Reklam ve zararlı site kalkanı", Tones.GREEN,
                dnsStatus,
                providerBtn,
                dnsToggle,
                text("💡 Kalıcı olsun: Ayarlar › VPN › Çocuk Kalkanı › \"Her zaman açık VPN\" açın. " +
                    "\"VPN olmadan bağlantıları engelle\" seçeneğini AÇMAYIN.", 13f, color = Palette.INK_SOFT),
                button("🌐  VPN ayarlarını aç", Color.WHITE, Tones.GREEN.accent) {
                    openTrusted(Intent(Settings.ACTION_VPN_SETTINGS))
                },
                button("🔁  Alternatif: Özel DNS (VPN'siz)", Color.WHITE, Tones.GREEN.accent) { showPrivateDnsHelp() }
            ))

            addView(card("🧷", "Silme koruması", Tones.ORANGE,
                adminStatus,
                button("🛡️  Silme korumasını etkinleştir", Tones.ORANGE.accent) { enableAdmin() }
            ))

            addView(card("🔑", "Ebeveyn PIN'i", Tones.PURPLE,
                button("✏️  PIN değiştir", Tones.PURPLE.accent) { setupPin(firstTime = false) }
            ))

            countText = chip().apply {
                setTextColor(Tones.PINK.accent)
                background = round(Color.WHITE, 50)
            }
            addView(card("📱", "Uygulamalar", Tones.PINK,
                text(
                    "Tüm uygulamalar başta 🔒 kilitli gelir. Çocuğun PIN'siz açabilmesini istediklerinize " +
                        "dokunup 🔓 serbest bırakın. Sonradan yüklenen uygulamalar da kilitli gelir.",
                    13f, color = Palette.INK_SOFT
                ),
                countText,
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(button("🔒 Tümünü kilitle", Tones.PINK.accent) { setAll(locked = true) },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) })
                    addView(button("🔓 Tümü serbest", Color.WHITE, Tones.PINK.accent) { setAll(locked = false) },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) })
                }
            ))
            addView(View(this@MainActivity), LinearLayout.LayoutParams(1, dp(6)))
        }
    }

    private fun refreshStatus() {
        lockStatus.setChip(isAccessibilityOn(), "✅ Kilit AÇIK", "⚠️ Kilit KAPALI")
        val running = DnsVpnService.isRunning
        dnsStatus.setChip(running, "✅ Kalkan AÇIK", "⚠️ Kalkan KAPALI")
        dnsToggle.text = if (running) "⏸️  Kalkanı kapat" else "▶️  Kalkanı aç"
        providerBtn.text = "🎛️  Filtre: ${Prefs.provider(this).name}"
        adminStatus.setChip(isAdminOn(), "✅ Koruma AÇIK", "⚠️ Koruma KAPALI")
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

    private fun setAll(locked: Boolean) {
        val title = if (locked) "Tüm uygulamalar kilitlensin mi?" else "Tüm uygulamalar serbest bırakılsın mı?"
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(if (locked) null else "Ayarlar ve Play Store da serbest kalır; çocuk korumaları kapatabilir.")
            .setPositiveButton("Evet") { _, _ ->
                Prefs.setAllowedAll(this, if (locked) emptySet() else apps.map { it.pkg }.toSet())
                applyFilter()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun applyFilter() {
        val q = Apps.fold(query)
        shown.clear()
        shown.addAll(if (q.isEmpty()) apps else apps.filter {
            Apps.fold(it.label).contains(q) || it.pkg.contains(q)
        })
        updateCount()
        adapter.notifyDataSetChanged()
    }

    private fun updateCount() {
        val n = apps.count { Prefs.isLocked(this, it.pkg) }
        countText.text = "🔒 $n kilitli   🔓 ${apps.size - n} serbest" +
            if (query.isEmpty()) "" else "   🔍 ${shown.size} sonuç"
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val settingsPkg = Intent(Settings.ACTION_SETTINGS).resolveActivity(pm)?.packageName
            val suggested = setOfNotNull(settingsPkg, "com.android.vending")
            val collator = Collator.getInstance(Locale("tr", "TR"))

            val items = Apps.lockablePackages(this)
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
                applyFilter()
            }
        }.start()
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int): Any = shown[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val ctx = this@MainActivity
            val outer = (convertView as? FrameLayout) ?: FrameLayout(ctx).apply {
                setPadding(dp(14), dp(4), dp(14), dp(4))
                addView(LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    addView(ImageView(ctx), LinearLayout.LayoutParams(dp(44), dp(44)))
                    addView(LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(12), 0, dp(8), 0)
                        addView(text("", 16f, bold = true))
                        addView(text("", 12f, color = Palette.INK_SOFT).apply { setPadding(0, 0, 0, 0) })
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(chip().apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 0 })
                })
            }
            val row = outer.getChildAt(0) as LinearLayout
            val iv = row.getChildAt(0) as ImageView
            val texts = row.getChildAt(1) as LinearLayout
            val name = texts.getChildAt(0) as TextView
            val note = texts.getChildAt(1) as TextView
            val toggle = row.getChildAt(2) as TextView

            val item = shown[position]
            iv.setImageDrawable(item.icon)
            name.text = item.label

            fun paint(locked: Boolean) {
                row.background = if (locked) round(Color.WHITE, 20, Color.parseColor("#FFD3E0"))
                else round(Color.parseColor("#EFFFF2"), 20, Color.parseColor("#B8EBC4"))
                toggle.setChip(!locked, "🔓 Serbest", "🔒 Kilitli")
                note.text = when {
                    item.suggested && !locked -> "⚠️ Kilitli kalması önerilir"
                    item.suggested -> "⭐ Kilitli kalmalı"
                    locked -> "Açmak için PIN gerekir"
                    else -> "Çocuk serbestçe açabilir"
                }
            }
            paint(Prefs.isLocked(ctx, item.pkg))

            outer.setOnClickListener {
                val nowLocked = !Prefs.isLocked(ctx, item.pkg)
                Prefs.setLocked(ctx, item.pkg, nowLocked)
                paint(nowLocked)
                updateCount()
            }
            return outer
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
