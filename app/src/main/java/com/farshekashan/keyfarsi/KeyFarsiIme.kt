package com.farshekashan.keyfarsi

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

class KeyFarsiIme : InputMethodService() {

    private var english = false
    private var numberMode = false
    private var clipboardMode = false
    private var symbolMode = false
    private var emojiMode = false
    private var uppercase = false
    private var emojiCategory = 0

    private var suggestionRow: LinearLayout? = null
    private var keyboardRoot: LinearLayout? = null

    private val handler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null

    private val prefs by lazy { getSharedPreferences("keyfarsi", MODE_PRIVATE) }

    private val clipboard by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    // ============================================================
    // ✅ مقادیر پیش‌فرض تنظیمات
    // ============================================================
    private val DEFAULT_KEYBOARD_WIDTH = 378
    private val DEFAULT_KEYBOARD_HEIGHT = 349
    private val DEFAULT_KEY_WIDTH = 34
    private val DEFAULT_KEY_HEIGHT = 48
    private val DEFAULT_KEY_GAP = 5

    // ============================================================
    // ✅ متن‌های ثابت کیبورد — مستقل از زبان سیستم
    // ============================================================
    private fun t(key: String): String {
        return when (key) {
            "key_space" -> "فاصله"
            "key_shift" -> "⇧"
            "key_backspace" -> "⌫"
            "key_enter" -> "↵"
            "key_comma" -> ","
            "key_dot" -> "."
            "key_globe" -> "🌐"
            "mode_letters" -> "حروف"
            "mode_numbers" -> "اعداد"
            "mode_symbols" -> "نمادها"
            "mode_emoji" -> "ایموجی"
            "mode_back" -> "بازگشت"
            "mode_back_to_letters" -> "بازگشت به حروف"
            "clipboard_empty" -> "هنوز متنی ذخیره نشده است"
            "clipboard_save_current" -> "ذخیره متن فعلی"
            "clipboard_pin" -> "📌"
            "clipboard_unpin" -> "☆"
            "clipboard_delete" -> "×"
            "clipboard_pinned_section" -> "پین‌شده‌ها"
            "clipboard_recent_section" -> "۱۰ مورد آخر کپی‌شده"
            "clipboard_pin_first" -> "ابتدا پین را بردارید"
            "clipboard_recent_empty" -> "(خالی)"
            "toolbar_brand" -> "KeyFarsi"
            "toolbar_numbers" -> "۱۲۳"
            "toolbar_symbols" -> "=\\<"
            "toolbar_emoji" -> "😊"
            "toolbar_clipboard" -> "📋"
            "toolbar_lang_fa" -> "فا"
            "toolbar_lang_en" -> "EN"
            else -> key
        }
    }

    // ============================================================
    // ✅ ردیف‌های حروف — «ژ» مستقل کنار «ز» در ردیف سوم
    // ============================================================
    private val persianRows = listOf(
        "ضصثقفغعهخحجچ",
        "شسیبلاتنمکگ",
        "ظطزرژذدپو"
    )

    private val englishRows = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    private val alternateChars: Map<String, List<String>> = mapOf(
        "ا" to listOf("آ", "أ", "إ", "ء", "ئ", "ؤ", "ۀ"),
        "ی" to listOf("ي", "ى", "ئ"),
        "ک" to listOf("ك"),
        "ه" to listOf("ة", "ۀ"),
        "و" to listOf("ؤ"),
        "ن" to listOf("ں"),
        "ت" to listOf("ٹ"),
        "ر" to listOf("ڑ"),
        "د" to listOf("ڈ"),
        "ز" to listOf("ذ"),
        "س" to listOf("ص", "ث"),
        "گ" to listOf("ګ"),
        "a" to listOf("à", "á", "â", "ä", "å", "æ", "ā"),
        "e" to listOf("è", "é", "ê", "ë", "ē"),
        "i" to listOf("ì", "í", "î", "ï", "ī"),
        "o" to listOf("ò", "ó", "ô", "ö", "ø", "ō"),
        "u" to listOf("ù", "ú", "û", "ü", "ū"),
        "c" to listOf("ç", "ć", "č"),
        "n" to listOf("ñ", "ń", "ň"),
        "s" to listOf("ś", "š", "ş"),
        "z" to listOf("ź", "ž", "ż"),
        "y" to listOf("ý", "ÿ")
    )

    private val allSymbols = listOf(
        "؟","،","؛",":","!","…",".",",","'","\"","(",")","[","]","{","}",
        "<",">","/","\\","|","_","-","+","=","*","%","@","#","$","^","~",
        "`","°","±","×","÷","√","≠","≈","≤","≥","∞","π","∑","∆","€","£",
        "¥","₹","¢","₽","₺","₿","&","§","©","®","™","✓","✔","✕","✖","★",
        "☆","•","●","○","٪","٫","٬","ـ","ٔ","‌","‍","ٓ","َ","ِ","ُ","ّ",
        "ْ","ً","ٍ","ٌ","«","»","‹","›","¿","¡","¶","†","‡","‰","‱","※",
        "℃","℉","←","→","↑","↓","↔","↕","⇐","⇒","⇑","⇓"
    )

    private val emojiCategories = listOf(
        "😀" to listOf(
            "😀","😃","🙏","😁","😁","😆","😅","😂","🤣",
            "😊","😇","🙂","🙃","😉","😌","😍","🥰",
            "😘","😗","😙","😚","😋","😛","😜","🤪",
            "😎","🤓","🧐","🤔","🤗","🤭","🤫","🤥",
            "😐","😑","😶","🙄","😏","😣","😥","😮",
            "🤐","😯","😪","😫","🥱","😴","😷","🤒",
            "🤕","🤢","🤮","🤧","🥵","🥶","🥴","😵",
            "🤯","🤠","🥳","🥸","😈","👿","💀","👻"
        ),
        "❤️" to listOf(
            "❤️","🧡","💛","💚","💙","💜","🖤","🤍",
            "🤎","💔","❣️","💕","💞","💓","💗","💖",
            "💘","💝","💟","♥️","💌","💋","💍","💎"
        ),
        "👋" to listOf(
            "👋","🤚","🖐️","✋","🖖","👌","🤌","🤏",
            "✌️","🤞","🫰","🤟","🤘","🤙","👈","👉",
            "👆","🖕","👇","☝️","👍","👎","✊","👊",
            "🤛","🤜","👏","🙌","👐","🤲","🤝","🙏",
            "✍️","💅","💪","🦾","🦿","🦵","🦶","👂",
            "👃","🧠","🫀","🫁","🦷","🦴","👀","👁️"
        ),
        "🐱" to listOf(
            "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼",
            "🐻‍❄️","🐨","🐯","🦁","🐮","🐷","🐸","🐵",
            "🐔","🐧","🐦","🐤","🦆","🦅","🦉","🦇",
            "🐺","🐗","🐴","🦄","🐝","🪱","🐛","🦋",
            "🐌","🐞","🐜","🪰","🕷️","🦂","🐢","🐍",
            "🦎","🦖","🦕","🐙","🦑","🦐","🦀","🐠"
        ),
        "🍎" to listOf(
            "🍎","🍏","🍊","🍋","🍌","🍉","🍇","🍓",
            "🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝",
            "🍅","🍆","🥑","🥦","🥬","🥒","🌶️","🫑",
            "🌽","🥕","🧄","🧅","🥔","🍠","🥐","🥯",
            "🍞","🥖","🥨","🧀","🥚","🍳","🧈","🥞",
            "🧇","🥓","🥩","🍗","🍖","🌭","🍔","🍟"
        ),
        "⚽" to listOf(
            "⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉",
            "🥏","🎱","🪀","🏓","🏸","🏒","🏑","🥍",
            "🏏","🪃","🥅","⛳","🪁","🏹","🎣","🤿",
            "🥊","🥋","🎽","🛹","🛼","🛷","⛸️","🥌",
            "🎿","⛷️","🏂","🏋️","🤼","🤸","⛹️","🤺"
        ),
        "🚗" to listOf(
            "🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑",
            "🚒","🚐","🛻","🚚","🚛","🚜","🛵","🏍️",
            "🚲","🛴","🚏","🛣️","🛤️","⛽","🚨","🚥",
            "🚦","🚧","⚓","⛵","🚤","🛳️","✈️","🚀",
            "🛸","🚁","🚂","🚆","🚇","🚊","🚉","🚄"
        ),
        "💡" to listOf(
            "💡","📱","💻","⌨️","🖥️","🖨️","🖱️","🖲️",
            "💾","💿","📀","📷","📸","📹","🎥","📞",
            "☎️","📺","📻","🎙️","🎚️","🎛️","🧭","⏱️",
            "⏰","⌚","🔋","🪫","🔌","💡","🔦","🕯️",
            "🧯","🛠️","🔧","🔨","⚙️","🧰","🪛","🔩"
        ),
        "🌸" to listOf(
            "🌸","🌹","🌺","🌻","🌼","🌷","🪻","💐",
            "🌱","🌿","☘️","🍀","🌵","🌴","🌳","🌲",
            "🌾","🌞","🌝","🌛","🌜","🌚","🌕","🌙",
            "⭐","🌟","✨","⚡","🔥","💧","🌊","☀️",
            "🌈","☁️","⛅","🌤️","🌧️","⛈️","❄️","☃️"
        ),
        "🎉" to listOf(
            "🎉","🎊","🎈","🎁","🎀","🎂","🍰","🧁",
            "🎄","🎃","🎆","🎇","🧨","✨","🎐","🎏",
            "🎎","🎍","🎋","🎑","🧧","🎗️","🎟️","🎫",
            "🏆","🥇","🥈","🥉","🏅","🎖️","🎯","🎮",
            "🕹️","🎲","♟️","🧩","🪅","🪩","🎨","🖼️"
        )
    )

    private val clipboardListener =
        ClipboardManager.OnPrimaryClipChangedListener {
            saveClipboardCurrent()
        }

    override fun onCreate() {
        super.onCreate()
        clipboard.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(clipboardListener)
        stopRepeat()
        super.onDestroy()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        saveClipboardCurrent()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        saveClipboardCurrent()
    }

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        if (!isInputViewShown) return

        val navBarHeight = getNavigationBarHeight()
        val keyboardHeight = keyboardRoot?.height ?: 0

        outInsets.contentTopInsets = outInsets.visibleTopInsets
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
        outInsets.touchableRegion.set(
            0,
            0,
            resources.displayMetrics.widthPixels,
            (keyboardHeight - navBarHeight).coerceAtLeast(0)
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isInputViewShown) setInputView(buildKeyboard())
    }

    private fun getNavigationBarHeight(): Int {
        val res = resources
        val id = res.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) res.getDimensionPixelSize(id) else 0
    }

    private fun buildKeyboard(): View {
        val dark = prefs.getBoolean("dark", false)
        val background = if (dark) Color.rgb(29, 29, 35) else Color.rgb(232, 228, 239)
        val normalKey = if (dark) Color.rgb(57, 55, 66) else Color.WHITE
        val foreground = if (dark) Color.WHITE else Color.rgb(38, 34, 47)
        val accent = Color.rgb(103, 80, 164)

        // ✅ پیش‌فرض‌های جدید
        val requestedWidth = prefs.getInt("keyboard_width", DEFAULT_KEYBOARD_WIDTH)
        val requestedHeight = prefs.getInt("keyboard_height", DEFAULT_KEYBOARD_HEIGHT)
        val screenWidth = resources.displayMetrics.widthPixels

        val actualWidth = if (requestedWidth > 0)
            dp(requestedWidth).coerceAtMost(screenWidth)
        else ViewGroup.LayoutParams.MATCH_PARENT

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(background)
            val navBar = getNavigationBarHeight()
            setPadding(dp(4), dp(4), dp(4), navBar + dp(4))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        keyboardRoot = root

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            // ✅ حداقل ارتفاع 349dp
            minimumHeight = dp(requestedHeight)
            layoutParams = LinearLayout.LayoutParams(
                actualWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(content)

        buildToolbar(content, normalKey, foreground, accent)

        when {
            clipboardMode -> buildClipboard(content, normalKey, foreground)
            emojiMode -> buildEmojiPage(content, normalKey, foreground)
            symbolMode -> buildSymbolPage(content, normalKey, foreground)
            numberMode -> buildNumberPad(content, normalKey, foreground)
            else -> {
                buildSuggestions(content, normalKey, foreground)
                buildLetterKeyboard(content, normalKey, foreground)
            }
        }
        return root
    }

    private fun buildToolbar(root: LinearLayout, keyBg: Int, fg: Int, accent: Int) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        bar.addView(label(t("toolbar_brand"), 13f, fg).apply {
            setTypeface(null, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, dp(34), 1f))

        bar.addView(actionButton(t("toolbar_numbers"), keyBg, fg) {
            toggleMode { numberMode = !numberMode }
        })
        bar.addView(actionButton(t("toolbar_symbols"), keyBg, fg) {
            toggleMode { symbolMode = !symbolMode }
        })
        bar.addView(actionButton(t("toolbar_emoji"), keyBg, fg) {
            toggleMode { emojiMode = !emojiMode }
        })
        // ✅ دکمه 📋 — مستقیم صفحه کلیپ‌بورد را باز می‌کند
        bar.addView(actionButton(t("toolbar_clipboard"), keyBg, fg) {
            numberMode = false
            symbolMode = false
            emojiMode = false
            clipboardMode = true
            refreshKeyboard()
        })
        bar.addView(actionButton(
            if (english) t("toolbar_lang_fa") else t("toolbar_lang_en"),
            accent, Color.WHITE
        ) {
            english = !english
            uppercase = false
            refreshKeyboard()
        })
        root.addView(bar)
    }

    private inline fun toggleMode(block: () -> Unit) {
        numberMode = false
        symbolMode = false
        emojiMode = false
        clipboardMode = false
        block()
        refreshKeyboard()
    }

    private fun buildSuggestions(root: LinearLayout, keyBg: Int, fg: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            minimumHeight = dp(38)
        }
        suggestionRow = row
        root.addView(row)
        refreshSuggestions(keyBg, fg)
    }

    private fun refreshSuggestions(keyBg: Int? = null, fg: Int? = null) {
        val row = suggestionRow ?: return
        row.removeAllViews()
        val dark = prefs.getBoolean("dark", false)
        val buttonBg = keyBg ?: if (dark) Color.rgb(57, 55, 66) else Color.WHITE
        val textColor = fg ?: if (dark) Color.WHITE else Color.rgb(38, 34, 47)

        val currentWord = getCurrentWord()
        val suggestions = if (currentWord.isBlank()) listOf(" ", " ", " ")
                          else findSuggestions(currentWord).take(3).let {
                              if (it.isEmpty()) listOf(currentWord, " ", " ") else it
                          }

        suggestions.forEach { word ->
            val button = TextView(this).apply {
                text = word
                textSize = 14f
                setTextColor(textColor)
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(dp(5), dp(3), dp(5), dp(3))
                background = createKeyBackground(buttonBg)
                isClickable = true
                setOnClickListener {
                    if (word.isNotBlank()) replaceCurrentWord(word)
                }
            }
            row.addView(button,
                LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                    setMargins(dp(2), dp(1), dp(2), dp(1))
                })
        }
    }

    private fun buildLetterKeyboard(root: LinearLayout, keyBg: Int, fg: Int) {
        val rows = if (english) englishRows else persianRows
        // ✅ پیش‌فرض عرض دکمه 34
        val configuredKeyWidth = prefs.getInt("key_width", DEFAULT_KEY_WIDTH)
        val gapPx = dp(gap())

        val screenWidthPx = resources.displayMetrics.widthPixels
        val usableWidthPx = screenWidthPx - dp(8)

        val columns: Int
        if (configuredKeyWidth > 0) {
            val manualKeyPx = dp(configuredKeyWidth)
            columns = ((usableWidthPx - gapPx * 2) / (manualKeyPx + gapPx * 2))
                .coerceIn(2, 12)
        } else {
            columns = rows.maxOf { it.length }
        }

        val keyWidthPx = ((usableWidthPx - columns * gapPx * 2) / columns)
            .coerceAtLeast(dp(24))

        val allChars = mutableListOf<Any>()
        rows.forEach { row -> row.forEach { ch -> allChars.add(ch) } }

        val chunks = allChars.chunked(columns)

        chunks.forEach { chunk ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val emptyCount = columns - chunk.size
            val emptyLeft = emptyCount / 2
            val emptyRight = emptyCount - emptyLeft

            repeat(emptyLeft) { line.addView(emptyFixedKey(keyWidthPx)) }

            chunk.forEach { item ->
                if (item is Char) {
                    val v = if (english && uppercase) item.uppercaseChar().toString()
                            else item.toString()
                    line.addView(letterKey(v, keyBg, fg, keyWidthPx) { commit(v) })
                }
            }

            repeat(emptyRight) { line.addView(emptyFixedKey(keyWidthPx)) }

            root.addView(line, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        // ✅ ردیف اختصاصی بک‌اسپیس — چپ‌چین
        val bsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        bsRow.addView(backspaceKey(
            Color.rgb(190, 155, 215), Color.WHITE, keyWidthPx * 2
        ))
        bsRow.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, keyHeight(), 1f)
        })
        root.addView(bsRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        buildBottomRow(root, keyBg, fg)
    }

    private fun letterKey(
        text: String,
        bg: Int,
        fg: Int,
        widthPx: Int,
        click: () -> Unit
    ): TextView {
        val key = keyButton(text, bg, fg) { click() }
        key.layoutParams = LinearLayout.LayoutParams(widthPx, keyHeight()).apply {
            setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
        }
        return key
    }

    private fun emptyFixedKey(widthPx: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, keyHeight()).apply {
                setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
            }
            alpha = 0f
        }
    }

    private fun backspaceKey(bg: Int, fg: Int, widthPx: Int): TextView {
        val key = TextView(this).apply {
            text = t("key_backspace")
            textSize = 14f
            setTextColor(fg)
            gravity = Gravity.CENTER
            maxLines = 1
            setPadding(dp(2), dp(2), dp(2), dp(5))
            background = createKeyBackground(bg)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(3).toFloat()
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            layoutParams = LinearLayout.LayoutParams(widthPx, keyHeight()).apply {
                setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
            }

            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.animate().translationY(dp(2).toFloat())
                            .setDuration(40).start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            view.elevation = dp(1).toFloat()
                        }
                        deleteOneChar()
                        startRepeat()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.animate().translationY(0f).setDuration(60).start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            view.animate().setDuration(60).withEndAction {
                                view.elevation = dp(3).toFloat()
                            }.start()
                        }
                        stopRepeat()
                        true
                    }
                    else -> false
                }
            }
        }
        return key
    }

    private fun startRepeat() {
        stopRepeat()
        repeatRunnable = Runnable {
            deleteOneChar()
            handler.postDelayed(repeatRunnable!!, 50L)
        }
        handler.postDelayed(repeatRunnable!!, 400L)
    }

    private fun stopRepeat() {
        repeatRunnable?.let { handler.removeCallbacks(it) }
        repeatRunnable = null
    }

    private fun deleteOneChar() {
    val ic = currentInputConnection ?: return
    // بررسی وجود متن انتخاب‌شده
    val selected = ic.getSelectedText(0)
    if (!selected.isNullOrEmpty()) {
        // اگر انتخاب وجود دارد، آن را با رشته خالی جایگزین کن (یعنی حذف)
        ic.commitText("", 1)
    } else {
        // اگر انتخابی وجود ندارد، کاراکتر قبل از مکان‌نما را حذف کن
        ic.deleteSurroundingText(1, 0)
    }
    refreshSuggestions()
}

    private fun buildBottomRow(root: LinearLayout, keyBg: Int, fg: Int) {
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        bottom.addView(actionButton("?123", keyBg, fg) {
            toggleMode { numberMode = true }
        })
        bottom.addView(actionButton(t("key_comma"), keyBg, fg) {
            commit(",")
        })
        bottom.addView(actionButton(t("key_globe"), keyBg, fg) {
            english = !english
            refreshKeyboard()
        })
        if (english) {
            bottom.addView(actionButton(t("key_shift"), keyBg, fg) {
                uppercase = !uppercase
                refreshKeyboard()
            })
        }
        bottom.addView(keyButton(t("key_space"), keyBg, fg) {
            commit(" ")
        }, LinearLayout.LayoutParams(0, keyHeight(), 3f).apply {
            setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
        })
        bottom.addView(actionButton(t("key_dot"), keyBg, fg) {
            commit(".")
        })
        bottom.addView(actionButton(
            t("key_enter"),
            Color.rgb(103, 80, 164), Color.WHITE
        ) { sendEnter() })
        root.addView(bottom)
    }

    private fun buildNumberPad(root: LinearLayout, keyBg: Int, fg: Int) {
        val rows = listOf(
            listOf("۱", "۲", "۳"),
            listOf("۴", "۵", "۶"),
            listOf("۷", "۸", "۹"),
            listOf("⌫", "۰")
        )
        rows.forEach { row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { value ->
                if (value == "⌫") {
                    line.addView(backspaceKey(keyBg, fg, (resources.displayMetrics.widthPixels - dp(8)) / 3))
                } else {
                    val display = if (english) toEnglishDigits(value) else value
                    line.addView(keyButton(display, keyBg, fg, 20f) {
                        commit(if (english) toEnglishDigits(value) else value)
                    }, LinearLayout.LayoutParams(0, keyHeight(), 1f).apply {
                        setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
                    })
                }
            }
            root.addView(line)
        }

        val symbolsRow = listOf("؟","%","@","×","*","(",")","!","+","_","=",".")
        val symLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        symbolsRow.forEach { s ->
            symLine.addView(keyButton(s, keyBg, fg, 16f) { commit(s) },
                LinearLayout.LayoutParams(0, keyHeight(), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
        }
        root.addView(symLine)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        controls.addView(actionButton(t("mode_letters"), keyBg, fg) {
            numberMode = false
            refreshKeyboard()
        })
        controls.addView(actionButton(t("mode_symbols"), keyBg, fg) {
            symbolMode = true
            numberMode = false
            refreshKeyboard()
        })
        controls.addView(actionButton(t("mode_emoji"), keyBg, fg) {
            emojiMode = true
            numberMode = false
            refreshKeyboard()
        })
        controls.addView(actionButton(
            t("key_enter"),
            Color.rgb(103, 80, 164), Color.WHITE
        ) { sendEnter() })
        root.addView(controls)
    }

    private fun buildSymbolPage(root: LinearLayout, keyBg: Int, fg: Int) {
        val scroller = ScrollView(this)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        allSymbols.chunked(8).forEach { row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { symbol ->
                line.addView(keyButton(symbol, keyBg, fg, 18f) { commit(symbol) },
                    LinearLayout.LayoutParams(0, keyHeight(), 1f).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    })
            }
            column.addView(line)
        }
        scroller.addView(column)
        root.addView(scroller, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        controls.addView(actionButton(t("mode_numbers"), keyBg, fg) {
            symbolMode = false
            numberMode = true
            refreshKeyboard()
        })
        controls.addView(actionButton(t("mode_emoji"), keyBg, fg) {
            symbolMode = false
            emojiMode = true
            refreshKeyboard()
        })
        controls.addView(actionButton(
            t("mode_letters"),
            Color.rgb(103, 80, 164), Color.WHITE
        ) {
            symbolMode = false
            refreshKeyboard()
        })
        root.addView(controls)
    }

    private fun buildEmojiPage(root: LinearLayout, keyBg: Int, fg: Int) {
        val categories = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        emojiCategories.forEachIndexed { index, category ->
            categories.addView(actionButton(category.first, keyBg, fg) {
                emojiCategory = index
                refreshKeyboard()
            })
        }
        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(categories)
        }
        root.addView(categoryScroll)

        val emojis = emojiCategories[emojiCategory].second
        emojis.chunked(8).forEach { row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { emoji ->
                line.addView(keyButton(emoji, keyBg, fg, 22f) { commit(emoji) },
                    LinearLayout.LayoutParams(0, keyHeight(), 1f).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    })
            }
            root.addView(line)
        }
        root.addView(actionButton(
            t("mode_back_to_letters"),
            Color.rgb(103, 80, 164), Color.WHITE
        ) {
            emojiMode = false
            refreshKeyboard()
        })
    }

    private fun buildClipboard(root: LinearLayout, keyBg: Int, fg: Int) {
        val list = loadClipboard()
        android.util.Log.d("KeyFarsi", "buildClipboard: total=${list.length()}")

        val scroller = ScrollView(this)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val pinnedItems = mutableListOf<Pair<Int, JSONObject>>()
        val recentItems = mutableListOf<Pair<Int, JSONObject>>()
        for (i in list.length() - 1 downTo 0) {
            val item = list.optJSONObject(i) ?: continue
            if (item.optBoolean("pinned")) pinnedItems.add(i to item)
            else recentItems.add(i to item)
        }
        android.util.Log.d("KeyFarsi", "pinned=${pinnedItems.size}, recent=${recentItems.size}")

        if (pinnedItems.isNotEmpty()) {
            column.addView(label("📌 ${t("clipboard_pinned_section")}", 13f, fg).apply {
                setPadding(dp(6), dp(8), dp(6), dp(4))
                setTypeface(null, Typeface.BOLD)
            })
            pinnedItems.forEach { (index, item) ->
                column.addView(buildClipboardRow(list, index, item, keyBg, fg))
            }
        }

        column.addView(label("🕐 ${t("clipboard_recent_section")}", 13f, fg).apply {
            setPadding(dp(6), dp(8), dp(6), dp(4))
            setTypeface(null, Typeface.BOLD)
        })
        val recent = recentItems.take(10)
        if (recent.isEmpty()) {
            column.addView(label(t("clipboard_recent_empty"), 12f, fg).apply {
                setPadding(dp(8), dp(4), dp(8), dp(4))
            })
        } else {
            recent.forEach { (index, item) ->
                column.addView(buildClipboardRow(list, index, item, keyBg, fg))
            }
        }

        if (pinnedItems.isEmpty() && recentItems.isEmpty()) {
            column.addView(label(t("clipboard_empty"), 15f, fg).apply {
                setPadding(dp(8), dp(16), dp(8), dp(16))
            })
        }

        scroller.addView(column)
        root.addView(scroller, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton(t("clipboard_save_current"), keyBg, fg) {
            saveClipboardCurrent()
            refreshKeyboard()
        })
        actions.addView(actionButton(
            t("mode_back"),
            Color.rgb(103, 80, 164), Color.WHITE
        ) {
            clipboardMode = false
            refreshKeyboard()
        })
        root.addView(actions)
    }

    private fun buildClipboardRow(
        list: JSONArray,
        index: Int,
        item: JSONObject,
        keyBg: Int,
        fg: Int
    ): LinearLayout {
        val text = item.optString("text")
        val pinned = item.optBoolean("pinned")
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(keyButton(
            (if (pinned) "📌 " else "") + text.take(40),
            keyBg, fg, 13f
        ) { commit(text) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            setMargins(dp(4), dp(2), dp(4), dp(2))
        })
        row.addView(actionButton(
            if (pinned) t("clipboard_pin") else t("clipboard_unpin"),
            keyBg, fg
        ) {
            item.put("pinned", !pinned)
            saveClipboard(list)
            refreshKeyboard()
        })
        row.addView(actionButton(t("clipboard_delete"), keyBg, fg) {
            list.remove(index)
            saveClipboard(list)
            refreshKeyboard()
        })
        return row
    }

    // ============================================================
    // ✅ مقادیر پیش‌فرض جدید
    // ============================================================
    private fun keyHeight(): Int {
        val configured = prefs.getInt("key_height", DEFAULT_KEY_HEIGHT)
        return dp(if (configured <= 0) DEFAULT_KEY_HEIGHT else configured)
    }

    private fun gap(): Int = prefs.getInt("key_gap", DEFAULT_KEY_GAP).coerceAtLeast(0)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun keyButton(
        text: String,
        bg: Int,
        fg: Int,
        size: Float = 18f,
        click: () -> Unit
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(fg)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT
            maxLines = 1
            setPadding(dp(2), dp(2), dp(2), dp(5))
            background = createKeyBackground(bg)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(3).toFloat()
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.animate().translationY(dp(2).toFloat())
                            .setDuration(40).start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            view.elevation = dp(1).toFloat()
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.animate().translationY(0f).setDuration(60).start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            view.animate().setDuration(60).withEndAction {
                                view.elevation = dp(3).toFloat()
                            }.start()
                        }
                    }
                }
                false
            }
            setOnClickListener { click() }

            val alternates = alternateChars[text.lowercase()] ?: alternateChars[text]
            if (!alternates.isNullOrEmpty()) {
                setOnLongClickListener { v ->
                    showAlternativesPopup(v, alternates)
                    true
                }
            }
        }
    }

    private fun showAlternativesPopup(anchor: View, alternatives: List<String>) {
        val popup = PopupWindow(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(Color.rgb(250, 250, 252))
        }

        alternatives.forEach { alt ->
            val btn = TextView(this).apply {
                text = alt
                textSize = 20f
                setTextColor(Color.rgb(38, 34, 47))
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = createKeyBackground(Color.WHITE)
                isClickable = true
                setOnClickListener {
                    commit(alt)
                    popup.dismiss()
                }
            }
            container.addView(btn, LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
        }

        popup.contentView = container
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.elevation = dp(6).toFloat()
        popup.showAsDropDown(anchor, 0, -anchor.height - dp(60))
    }

    private fun actionButton(
        text: String,
        bg: Int,
        fg: Int,
        click: () -> Unit
    ): TextView {
        return keyButton(text, bg, fg, 14f, click).apply {
            minimumWidth = dp(40)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                keyHeight()
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        }
    }

    private fun createKeyBackground(color: Int): LayerDrawable {
        val dark = prefs.getBoolean("dark", false)
        val topColor = if (dark) lighten(color, 18) else lighten(color, 8)
        val bottomColor = if (dark) darken(color, 18) else darken(color, 12)

        val main = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topColor, bottomColor)
        ).apply {
            cornerRadius = dp(9).toFloat()
            setStroke(
                dp(1),
                if (dark) 0xFF55515F.toInt() else 0xFFE0DCE7.toInt()
            )
        }

        val shadow = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(darken(bottomColor, 30), darken(bottomColor, 40))
        ).apply { cornerRadius = dp(9).toFloat() }

        return LayerDrawable(arrayOf(InsetDrawable(shadow, 0, 0, 0, dp(2)), main))
    }

    private fun lighten(color: Int, amount: Int): Int {
        val r = min(255, Color.red(color) + amount)
        val g = min(255, Color.green(color) + amount)
        val b = min(255, Color.blue(color) + amount)
        return Color.rgb(r, g, b)
    }

    private fun darken(color: Int, amount: Int): Int {
        val r = max(0, Color.red(color) - amount)
        val g = max(0, Color.green(color) - amount)
        val b = max(0, Color.blue(color) - amount)
        return Color.rgb(r, g, b)
    }

    private fun refreshKeyboard() {
        suggestionRow = null
        stopRepeat()
        setInputView(buildKeyboard())
    }

    private fun commit(value: String) {
        if (value == " " || value == "\n" ||
            value in listOf(".", ",", "!", "؟", "؛")
        ) {
            learnPreviousWord()
        }
        currentInputConnection?.commitText(value, 1)
        if (value == " ") uppercase = false
        refreshSuggestions()
    }

    private fun sendEnter() {
        val info = currentInputEditorInfo
        if (info == null) return

        val inputType = info.inputType and InputType.TYPE_MASK_CLASS
        val isMultiline = (info.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

        if (isMultiline || inputType == InputType.TYPE_CLASS_TEXT) {
            currentInputConnection?.commitText("\n", 1)
        } else {
            val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
            if (action != EditorInfo.IME_ACTION_NONE) {
                currentInputConnection?.performEditorAction(action)
            } else {
                currentInputConnection?.sendKeyEvent(
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                )
                currentInputConnection?.sendKeyEvent(
                    KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
                )
            }
        }
    }

    private fun getCurrentWord(): String {
        return try {
            val before = currentInputConnection
                ?.getTextBeforeCursor(100, 0)
                ?.toString()
                .orEmpty()
            Regex("[\\p{L}\\p{M}]+$").find(before)?.value.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun replaceCurrentWord(newWord: String) {
        val oldWord = getCurrentWord()
        if (oldWord.isNotEmpty()) {
            currentInputConnection?.deleteSurroundingText(oldWord.length, 0)
        }
        currentInputConnection?.commitText(newWord, 1)
        learnWord(newWord)
        refreshSuggestions()
    }

    private fun findSuggestions(word: String): List<String> {
        if (word.isBlank()) return emptyList()
        val normalized = word.lowercase()
        val learned = loadLearnedWords()

        val learnedMatches = learned.keys().asSequence()
            .filter {
                it.lowercase().startsWith(normalized) &&
                    it.lowercase() != normalized
            }
            .sortedByDescending { learned.optInt(it, 0) }
            .toList()

        val common = commonWords.filter {
            it.startsWith(normalized) && it != normalized
        }

        return (learnedMatches + common).distinct().take(3)
    }

    private fun learnPreviousWord() {
        learnWord(getCurrentWord().trim())
    }

    private fun learnWord(rawWord: String) {
        if (isSensitiveInput()) return
        val word = rawWord.trim()
        if (word.length < 2 || word.length > 40) return
        if (word.any { it.isDigit() }) return
        if (word.any { !it.isLetter() && it != '\u200c' }) return

        val words = loadLearnedWords()
        val currentCount = words.optInt(word, 0)
        words.put(word, (currentCount + 1).coerceAtMost(1000))

        prefs.edit()
            .putString("learned_words", words.toString())
            .apply()
    }

    private fun loadLearnedWords(): JSONObject {
        return try {
            JSONObject(prefs.getString("learned_words", "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun isSensitiveInput(): Boolean {
        val type = currentInputEditorInfo?.inputType ?: return false
        val variation = type and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    private val commonWords = listOf(
        "سلام","سلامتی","سلامت","خوب","خوبی","خوبه",
        "ممنون","مرسی","بله","خیر","نه","آره","باشه",
        "حتما","لطفا","خواهش","خواهشاً","دوست","دوستان",
        "دوستت","دارم","ندارم","هستم","هستیم","هستند",
        "امروز","فردا","دیروز","الان","بعدا","بعداً",
        "خونه","خانه","کار","کارت","زندگی","خانواده",
        "میشه","می‌شود","میتونم","می‌توانم","میخوام",
        "می‌خواهم","میکنم","می‌کنم","باید","نباید",
        "چرا","چطور","چگونه","کجا","کی","چی","چه",
        "برای","از","به","با","در","این","اون","آن",
        "همه","هیچ","خیلی","زیاد","کم","بیشتر",
        "فارسی","انگلیسی","کیبورد","گوشی","موبایل",
        "برنامه","پیام","متن","کلمه","حروف",
        "the","this","that","there","their","hello",
        "thanks","thank","you","your","good","great",
        "what","when","where","how","please","have",
        "with","from","will","would","could","should"
    )

    private fun saveClipboardCurrent() {
        try {
            val clip = clipboard.primaryClip ?: run {
                android.util.Log.d("KeyFarsi", "clipboard: primaryClip is null")
                return
            }
            if (clip.itemCount == 0) {
                android.util.Log.d("KeyFarsi", "clipboard: itemCount=0")
                return
            }
            val value = clip.getItemAt(0)
                .coerceToText(this)
                ?.toString()
                ?.trim()
                .orEmpty()
            if (value.isBlank()) {
                android.util.Log.d("KeyFarsi", "clipboard: value is blank")
                return
            }

            android.util.Log.d("KeyFarsi", "clipboard: saving '${value.take(30)}'")

            val list = loadClipboard()
            if ((0 until list.length()).any {
                    list.optJSONObject(it)?.optString("text") == value
                }
            ) {
                android.util.Log.d("KeyFarsi", "clipboard: already exists")
                return
            }

            list.put(JSONObject().put("text", value).put("pinned", false))

            var unpinnedCount = 0
            for (i in list.length() - 1 downTo 0) {
                val obj = list.optJSONObject(i) ?: continue
                if (!obj.optBoolean("pinned")) {
                    unpinnedCount++
                    if (unpinnedCount > 10) {
                        list.remove(i)
                        break
                    }
                }
            }
            saveClipboard(list)
            android.util.Log.d("KeyFarsi", "clipboard: saved, total=${list.length()}")
        } catch (e: Exception) {
            android.util.Log.e("KeyFarsi", "clipboard save error", e)
        }
    }

    private fun loadClipboard(): JSONArray {
        return try {
            JSONArray(prefs.getString("clipboard", "[]"))
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveClipboard(array: JSONArray) {
        prefs.edit().putString("clipboard", array.toString()).apply()
    }

    private fun toEnglishDigits(value: String): String {
        return value.map { char ->
            when (char) {
                '۰' -> '0'
                '۱' -> '1'
                '۲' -> '2'
                '۳' -> '3'
                '۴' -> '4'
                '۵' -> '5'
                '۶' -> '6'
                '۷' -> '7'
                '۸' -> '8'
                '۹' -> '9'
                else -> char
            }
        }.joinToString("")
    }
}
