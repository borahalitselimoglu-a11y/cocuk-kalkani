package com.cocukkalkani.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Kilitli bir uygulama açıldığında üste gelen PIN ekranı. */
class LockActivity : Activity() {

    companion object {
        const val EXTRA_PKG = "pkg"
        private const val MAX_FAILS = 5
        private const val LOCKOUT_MS = 30_000L
    }

    private var target: String? = null
    private lateinit var icon: ImageView
    private lateinit var title: TextView
    private lateinit var pin: EditText
    private var fails = 0
    private var lockedUntil = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        icon = ImageView(this)
        title = text("", 20f, bold = true).apply { gravity = Gravity.CENTER }
        pin = pinField("Ebeveyn PIN'i").apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, _, _ -> tryUnlock(); true }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F1F8E9"))
            setPadding(dp(32), dp(32), dp(32), dp(32))
            addView(icon, LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            })
            addView(title)
            addView(text("Bu uygulama ebeveyn tarafından kilitlendi.").apply {
                gravity = Gravity.CENTER
            })
            addView(pin)
            addView(button("Kilidi aç") { tryUnlock() })
            addView(button("Ana ekrana dön") { goHome() })
        }
        setContentView(root)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent) {
        target = i.getStringExtra(EXTRA_PKG)
        pin.setText("")
        val pkg = target ?: return
        try {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            title.text = "${packageManager.getApplicationLabel(ai)} kilitli"
            icon.setImageDrawable(packageManager.getApplicationIcon(ai))
        } catch (_: Exception) {
            title.text = "Uygulama kilitli"
        }
        pin.requestFocus()
    }

    private fun tryUnlock() {
        val now = SystemClock.elapsedRealtime()
        if (now < lockedUntil) {
            val s = (lockedUntil - now) / 1000 + 1
            toast("Çok fazla hatalı deneme. $s sn bekleyin.")
            return
        }
        if (Prefs.checkPin(this, pin.text.toString().trim())) {
            fails = 0
            AppLockService.allowOnce(target)
            finish()
            overridePendingTransition(0, 0)
        } else {
            fails++
            pin.setText("")
            if (fails >= MAX_FAILS) {
                fails = 0
                lockedUntil = now + LOCKOUT_MS
                toast("Hatalı PIN. 30 saniye beklemelisiniz.")
            } else {
                toast("Hatalı PIN")
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goHome()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
