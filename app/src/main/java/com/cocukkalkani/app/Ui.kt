package com.cocukkalkani.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.InputType
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Renk paleti */
object Palette {
    val PAGE = Color.parseColor("#FFF8EC")
    val INK = Color.parseColor("#2D2A4A")
    val INK_SOFT = Color.parseColor("#6B6890")
    val HERO = intArrayOf(
        Color.parseColor("#7C4DFF"), Color.parseColor("#E040FB"), Color.parseColor("#FF6E40")
    )
    val CONFETTI = intArrayOf(
        Color.parseColor("#FFB3C7"), Color.parseColor("#A8E6CF"),
        Color.parseColor("#B3D4FF"), Color.parseColor("#FFE08A"), Color.parseColor("#D7B8FF")
    )
    val ON_BG = Color.parseColor("#D7F5DD")
    val ON_FG = Color.parseColor("#1B7A35")
    val OFF_BG = Color.parseColor("#FFDCE0")
    val OFF_FG = Color.parseColor("#C62845")
}

/** Kartların tema rengi: açık zemin + koyu vurgu */
data class Tone(val light: Int, val accent: Int)

object Tones {
    val BLUE = Tone(Color.parseColor("#E3F0FF"), Color.parseColor("#2F7BEA"))
    val GREEN = Tone(Color.parseColor("#E2F8E6"), Color.parseColor("#2EA44F"))
    val ORANGE = Tone(Color.parseColor("#FFF0DE"), Color.parseColor("#F57C00"))
    val PURPLE = Tone(Color.parseColor("#F1E6FF"), Color.parseColor("#8E44EC"))
    val PINK = Tone(Color.parseColor("#FFE6F0"), Color.parseColor("#E91E63"))
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.round(color: Int, radiusDp: Int, strokeColor: Int? = null): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null) setStroke(dp(2), strokeColor)
    }

fun Context.gradient(colors: IntArray, radiusDp: Int): GradientDrawable =
    GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
        cornerRadius = dp(radiusDp).toFloat()
    }

/** Puantiyeli desen (konfeti noktaları). */
class DotsDrawable(
    private val colors: IntArray,
    private val spacing: Float,
    private val radius: Float,
    private val alphaValue: Int = 255
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val b = bounds
        var y = b.top + spacing / 2
        var row = 0
        while (y < b.bottom + spacing) {
            var x = b.left + if (row % 2 == 0) spacing / 2 else spacing
            var i = row
            while (x < b.right + spacing) {
                paint.color = colors[i % colors.size]
                paint.alpha = alphaValue
                canvas.drawCircle(x, y, radius, paint)
                x += spacing
                i++
            }
            y += spacing
            row++
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

fun Context.text(
    s: CharSequence,
    sizeSp: Float = 15f,
    bold: Boolean = false,
    color: Int = Palette.INK
): TextView = TextView(this).apply {
    text = s
    textSize = sizeSp
    setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
    setPadding(0, dp(4), 0, dp(4))
}

/** Yuvarlak hap şeklinde renkli düğme. */
fun Context.button(
    label: CharSequence,
    bg: Int = Tones.PURPLE.accent,
    fg: Int = Color.WHITE,
    onClick: () -> Unit
): Button = Button(this).apply {
    text = label
    isAllCaps = false
    textSize = 15f
    setTextColor(fg)
    setTypeface(typeface, Typeface.BOLD)
    stateListAnimator = null
    val shape = round(bg, 50)
    background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33000000")), shape, null)
    minHeight = dp(48)
    setPadding(dp(18), dp(10), dp(18), dp(10))
    layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(8) }
    setOnClickListener { onClick() }
}

/** Durum etiketi (✅ AÇIK / ⚠️ KAPALI). */
fun TextView.setChip(on: Boolean, onText: String, offText: String) {
    text = if (on) onText else offText
    setTextColor(if (on) Palette.ON_FG else Palette.OFF_FG)
    background = context.round(if (on) Palette.ON_BG else Palette.OFF_BG, 50)
}

fun Context.chip(): TextView = TextView(this).apply {
    textSize = 14f
    setTypeface(typeface, Typeface.BOLD)
    setPadding(dp(14), dp(6), dp(14), dp(6))
    layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(6); bottomMargin = dp(4) }
}

/** Emojili renkli kart. */
fun Context.card(emoji: String, title: String, tone: Tone, vararg views: View): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = round(tone.light, 24)
        setPadding(dp(16), dp(14), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(14) }

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                text = emoji
                textSize = 22f
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.WHITE)
                }
            }, LinearLayout.LayoutParams(dp(46), dp(46)))
            addView(text(title, 17f, bold = true, color = tone.accent).apply {
                setPadding(dp(12), 0, 0, 0)
            })
        })
        views.forEach { addView(it) }
    }

fun Context.pinField(hint: String = "PIN"): EditText =
    EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        gravity = Gravity.CENTER
        textSize = 24f
        setTextColor(Palette.INK)
        background = round(Color.parseColor("#F3EEFF"), 18, Color.parseColor("#D7C6FF"))
        setPadding(dp(16), dp(12), dp(16), dp(12))
    }

/**
 * Android 15+ (API 35/36) uygulamaları ekranın kenarına kadar çizer.
 * İçeriğin durum çubuğu, gezinme çubuğu ve klavye altında kalmaması için iç boşluk ekler.
 */
fun View.fitSystemBars() {
    if (Build.VERSION.SDK_INT < 35) return
    val l = paddingLeft
    val t = paddingTop
    val r = paddingRight
    val b = paddingBottom
    setOnApplyWindowInsetsListener { v, insets ->
        val i = insets.getInsets(
            WindowInsets.Type.systemBars() or WindowInsets.Type.ime() or WindowInsets.Type.displayCutout()
        )
        v.setPadding(l + i.left, t + i.top, r + i.right, b + i.bottom)
        insets
    }
    requestApplyInsets()
}

/** Durum/gezinme çubuğu simgeleri: açık zeminde koyu (dark=true), koyu zeminde beyaz. */
@Suppress("DEPRECATION")
fun Activity.setBarIcons(darkIcons: Boolean) {
    if (Build.VERSION.SDK_INT >= 30) {
        val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(if (darkIcons) mask else 0, mask)
    } else {
        window.decorView.systemUiVisibility =
            if (darkIcons) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
    }
}

/** PIN soran diyalog. */
fun Activity.pinDialog(
    title: String,
    message: String?,
    negativeLabel: String,
    onNegative: () -> Unit,
    onOk: (String) -> Unit
): AlertDialog {
    val field = pinField()
    val box = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(12), dp(24), 0)
        addView(field)
    }
    val d = AlertDialog.Builder(this)
        .setTitle(title)
        .setView(box)
        .setCancelable(false)
        .setPositiveButton("Tamam") { _, _ -> onOk(field.text.toString().trim()) }
        .setNegativeButton(negativeLabel) { _, _ -> onNegative() }
    if (message != null) d.setMessage(message)
    return d.show()
}
