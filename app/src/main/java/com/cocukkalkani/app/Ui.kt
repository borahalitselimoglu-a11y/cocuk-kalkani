package com.cocukkalkani.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Küçük arayüz yardımcıları (XML yerleşim yerine kodla). */
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.text(s: CharSequence, sizeSp: Float = 15f, bold: Boolean = false): TextView =
    TextView(this).apply {
        text = s
        textSize = sizeSp
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

fun Context.button(label: CharSequence, onClick: () -> Unit): Button =
    Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { onClick() }
    }

fun Context.pinField(hint: String = "PIN"): EditText =
    EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        gravity = Gravity.CENTER
        textSize = 22f
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
        setPadding(dp(24), dp(8), dp(24), 0)
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
