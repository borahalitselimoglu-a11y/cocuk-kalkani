package com.cocukkalkani.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.widget.FrameLayout
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
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

        setBarIcons(darkIcons = false)

        // Android 13+: geri hareketi/tuşu kilit ekranını kapatıp kilitli uygulamayı açığa çıkarmasın.
        // (API 36 hedefleyen uygulamalarda onBackPressed artık çağrılmıyor.)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_OVERLAY
            ) { goHome() }
        }

        icon = ImageView(this)
        title = text("", 22f, bold = true).apply { gravity = Gravity.CENTER }
        pin = pinField("🔑 Ebeveyn PIN'i").apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, _, _ -> tryUnlock(); true }
        }

        // Ortadaki beyaz kart
        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = round(Color.WHITE, 32)
            elevation = dp(8).toFloat()
            setPadding(dp(24), dp(20), dp(24), dp(24))

            addView(FrameLayout(context).apply {
                // Uygulama simgesi + köşesinde kilit rozeti
                addView(icon, FrameLayout.LayoutParams(dp(76), dp(76)).apply {
                    gravity = Gravity.CENTER
                })
                addView(TextView(context).apply {
                    text = "🔒"
                    textSize = 18f
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#FFE08A"))
                        setStroke(dp(3), Color.WHITE)
                    }
                }, FrameLayout.LayoutParams(dp(36), dp(36)).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                })
            }, LinearLayout.LayoutParams(dp(96), dp(96)))
            addView(title)
            addView(text("Bu uygulamayı açmak için annenden ya da babandan izin iste 😊", 15f,
                color = Palette.INK_SOFT).apply { gravity = Gravity.CENTER })
            addView(pin, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) })
            addView(button("🔓  Kilidi aç", Tones.PURPLE.accent) { tryUnlock() })
            addView(button("🏠  Ana ekrana dön", Tones.PURPLE.light, Tones.PURPLE.accent) { goHome() })
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = LayerDrawable(arrayOf(
                GradientDrawable(GradientDrawable.Orientation.TL_BR, Palette.HERO),
                DotsDrawable(intArrayOf(Color.WHITE), dp(30).toFloat(), dp(3).toFloat(), 55)
            ))
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(text("🎈  ⭐  🧸  🌈  🎨", 26f).apply { gravity = Gravity.CENTER })
            addView(cardView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) })
            fitSystemBars()
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
            title.text = "${packageManager.getApplicationLabel(ai)} kilitli 🙈"
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
