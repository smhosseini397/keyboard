package com.farshekashan.keyfarsi

import android.inputmethodservice.InputMethodService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class KeyFarsiIme : InputMethodService() {
    private var english = false
    private var numberMode = false
    private var clipboardMode = false
    private val prefs by lazy { getSharedPreferences("keyfarsi", MODE_PRIVATE) }
    private val clipboard by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (isInputViewShown && !clipboardMode) saveClipboardCurrent()
    }

    override fun onCreate() {
        super.onCreate()
        clipboard.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(clipboardListener)
        super.onDestroy()
    }

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val dark = prefs.getBoolean("dark", false)
        val bg = if (dark) 0xFF202127.toInt() else 0xFFE9E5EF.toInt()
        val keyBg = if (dark) 0xFF373640.toInt() else Color.WHITE
        val fg = if (dark) Color.WHITE else 0xFF25212C.toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(5))
            setBackgroundColor(bg)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("KeyFarsi", 14f, fg).apply { setTypeface(null, Typeface.BOLD) }, LinearLayout.LayoutParams(0, dp(36), 1f))
        top.addView(actionButton(if (numberMode) "حروف" else "۱۲۳", keyBg, fg) {
            numberMode = !numberMode; clipboardMode = false; setInputView(buildKeyboard())
        })
        top.addView(actionButton("📋", keyBg, fg) {
            clipboardMode = !clipboardMode; numberMode = false; setInputView(buildKeyboard())
        })
        top.addView(actionButton(if (english) "EN" else "فا", 0xFF6750A4.toInt(), Color.WHITE) {
            english = !english; numberMode = false; clipboardMode = false; setInputView(buildKeyboard())
        })
        root.addView(top)

        if (clipboardMode) {
            buildClipboard(root, keyBg, fg)
            return root
        }
        if (numberMode) {
            buildNumberPad(root, keyBg, fg)
            return root
        }

        val rows = if (english) listOf(
            "qwertyuiop", "asdfghjkl", "zxcvbnm"
        ) else listOf(
            "ضصثقفغعهخحج", "شسیبلاتنمکگ", "ظطزرذدپو"
        )
        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            if (index == 2) line.addView(actionButton("⇧", keyBg, fg) { })
            row.forEach { ch ->
                line.addView(keyButton(ch.toString(), keyBg, fg) { commit(ch.toString()) },
                    LinearLayout.LayoutParams(0, keyHeight(), 1f).apply { setMargins(dp(gap()), dp(2), dp(gap()), dp(2)) })
            }
            if (index == 2) line.addView(actionButton("⌫", 0xFFD6C7E8.toInt(), fg) { currentInputConnection?.deleteSurroundingText(1, 0) })
            root.addView(line)
        }

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        bottom.addView(actionButton("?123", keyBg, fg) { numberMode = true; setInputView(buildKeyboard()) })
        bottom.addView(actionButton(",", keyBg, fg) { commit(",") })
        bottom.addView(keyButton("🌐", keyBg, fg) { english = !english; setInputView(buildKeyboard()) })
        bottom.addView(keyButton("فاصله", keyBg, fg) { commit(" ") }, LinearLayout.LayoutParams(0, keyHeight(), 3f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        bottom.addView(actionButton(".", keyBg, fg) { commit(".") })
        bottom.addView(actionButton("↵", 0xFF6750A4.toInt(), Color.WHITE) { currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)) })
        root.addView(bottom)
        return root
    }

    private fun buildNumberPad(root: LinearLayout, keyBg: Int, fg: Int) {
        val rows = listOf(listOf("۷","۸","۹"), listOf("۴","۵","۶"), listOf("۱","۲","۳"), listOf("۰","۰۰","⌫"))
        rows.forEach { row ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            row.forEach { s ->
                line.addView(keyButton(s, keyBg, fg, 24f) {
                    when (s) {
                        "⌫" -> currentInputConnection?.deleteSurroundingText(1, 0)
                        else -> commit(if (english) toEnglishDigits(s) else s)
                    }
                }, LinearLayout.LayoutParams(0, keyHeight().coerceAtLeast(dp(58)), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
            root.addView(line)
        }
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(actionButton(if (english) "فارسی" else "English", keyBg, fg) { english = !english; setInputView(buildKeyboard()) })
        controls.addView(actionButton("−", keyBg, fg) { commit("-") })
        controls.addView(actionButton(".", keyBg, fg) { commit(".") })
        controls.addView(actionButton("↵", 0xFF6750A4.toInt(), Color.WHITE) { currentInputConnection?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE) })
        root.addView(controls)
    }

    private fun buildClipboard(root: LinearLayout, keyBg: Int, fg: Int) {
        val list = loadClipboard()
        val scroller = ScrollView(this)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (list.length() == 0) column.addView(label("هنوز متنی ذخیره نشده است", 15f, fg))
        for (i in list.length() - 1 downTo 0) {
            val item = list.optJSONObject(i) ?: continue
            val text = item.optString("text")
            val pinned = item.optBoolean("pinned")
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(keyButton((if (pinned) "📌 " else "") + text.take(45), keyBg, fg, 13f) { commit(text) },
                LinearLayout.LayoutParams(0, dp(52), 1f))
            row.addView(actionButton(if (pinned) "📌" else "☆", keyBg, fg) { item.put("pinned", !pinned); saveClipboard(list); setInputView(buildKeyboard()) })
            row.addView(actionButton("×", keyBg, fg) {
                if (!pinned) { list.remove(i); saveClipboard(list); setInputView(buildKeyboard()) }
                else Toast.makeText(this, "ابتدا پین را بردارید", Toast.LENGTH_SHORT).show()
            })
            column.addView(row)
        }
        scroller.addView(column)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton("ذخیره متن فعلی", keyBg, fg) { saveClipboardCurrent(); setInputView(buildKeyboard()) })
        actions.addView(actionButton("بازگشت", 0xFF6750A4.toInt(), Color.WHITE) { clipboardMode = false; setInputView(buildKeyboard()) })
        root.addView(actions)
    }

    private fun saveClipboardCurrent() {
        try {
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) return
            val value = clip.getItemAt(0).coerceToText(this)?.toString()?.trim().orEmpty()
            if (value.isBlank()) return
            val list = loadClipboard()
            if ((0 until list.length()).any { list.optJSONObject(it)?.optString("text") == value }) return
            list.put(JSONObject().put("text", value).put("pinned", false))
            while (list.length() > 100) {
                var remove = -1
                for (i in 0 until list.length()) if (!list.optJSONObject(i).optBoolean("pinned")) { remove = i; break }
                if (remove < 0) break
                list.remove(remove)
            }
            saveClipboard(list)
        } catch (_: Exception) { }
    }

    private fun loadClipboard(): JSONArray = try { JSONArray(prefs.getString("clipboard", "[]")) } catch (_: Exception) { JSONArray() }
    private fun saveClipboard(a: JSONArray) { prefs.edit().putString("clipboard", a.toString()).apply() }

    private fun commit(s: String) { currentInputConnection?.commitText(s, 1) }
    private fun keyHeight(): Int {
        val configured = prefs.getInt("key_height", 48)
        return dp(if (configured <= 0) 48 else configured.coerceIn(28, 120))
    }
    private fun gap() = prefs.getInt("key_gap", 4).coerceIn(0, 16)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL
    }
    private fun keyButton(text: String, bg: Int, fg: Int, size: Float = 18f, click: () -> Unit) =
        TextView(this).apply {
            this.text = text; textSize = size; setTextColor(fg); gravity = Gravity.CENTER
            setBackgroundColor(bg); setOnClickListener { click() }
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
    private fun actionButton(text: String, bg: Int, fg: Int, click: () -> Unit) =
        keyButton(text, bg, fg, 14f, click).apply {
            val lp = LinearLayout.LayoutParams(-2, keyHeight()).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
            layoutParams = lp
            minimumWidth = dp(42)
        }
    private fun toEnglishDigits(s: String) = s.map { c -> when (c) {
        '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
        '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'; else -> c
    } }.joinToString("")
}
