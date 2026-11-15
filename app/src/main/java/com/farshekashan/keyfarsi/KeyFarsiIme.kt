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

    private val prefs by lazy { getSharedPreferences("keyfarsi", MODE_PRIVATE) }

    private val clipboard by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    // ✅ چیدمان فارسی: فقط ۳۲ حرف پایه — بقیه با نگه داشتن
    private val persianRows = listOf(
        "ضصثقفغعهخحجچپ",
        "شسیبلاتنمکگک",
        "ظطزرذدپو"
    )

    private val englishRows = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    // ✅ نگاشت کاراکترهای جایگزین (با نگه داشتن)
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
        "ز" to listOf("ژ", "ذ"),
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
            "😀","😃","😄","😁","😆","😅","😂","🤣",
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

        val requestedWidth = prefs.getInt("keyboard_width", 0)
        val requestedHeight = prefs.getInt("keyboard_height", 280)
        val screenWidth = resources.displayMetrics.widthPixels

        val actualWidth = if (requestedWidth > 0)
            dp(requestedWidth).coerceAtMost(screenWidth * 2)
        else ViewGroup.LayoutParams.MATCH_PARENT

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(background)
            val navBar = getNavigationBarHeight()
            setPadding(dp(4), dp(4), dp(4), navBar + dp(4))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (requestedHeight > 0) dp(requestedHeight)
                else ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        keyboardRoot = root

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
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
        bar.addView(label(getString(R.string.toolbar_brand), 13f, fg).apply {
            setTypeface(null, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, dp(34), 1f))

        bar.addView(actionButton(getString(R.string.toolbar_numbers), keyBg, fg) {
            toggleMode { numberMode = !numberMode }
        })
        bar.addView(actionButton(getString(R.string.toolbar_symbols), keyBg, fg) {
            toggleMode { symbolMode = !symbolMode }
        })
        bar.addView(actionButton(getString(R.string.toolbar_emoji), keyBg, fg) {
            toggleMode { emojiMode = !emojiMode }
        })
        bar.addView(actionButton(getString(R.string.toolbar_clipboard), keyBg, fg) {
            toggleMode { clipboardMode = !clipboardMode }
        })
        bar.addView(actionButton(
            if (english) getString(R.string.toolbar_lang_fa)
            else getString(R.string.toolbar_lang_en),
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

    // ✅ تراز و بدون اسکرول: همه دکمه‌ها عرض ثابت
    private fun buildLetterKeyboard(root: LinearLayout, keyBg: Int, fg: Int) {
        val rows = if (english) englishRows else persianRows

        // ✅ محاسبه عرض دکمه‌ها: اگر key_width صفر باشه، از عرض صفحه محاسبه می‌شه
        val configuredKeyWidth = prefs.getInt("key_width", 0)
        val gapDp = prefs.getInt("key_gap", 4).coerceAtLeast(0)
        val screenWidthDp = (resources.displayMetrics.widthPixels /
                resources.displayMetrics.density).toInt()

        // حداکثر تعداد کلید در هر خط
        val maxKeysPerRow: Int = if (configuredKeyWidth > 0) {
            val usableWidth = screenWidthDp - 8
            val perKey = configuredKeyWidth + (gapDp * 2)
            (usableWidth / perKey).coerceAtLeast(1)
        } else {
            // حالت خودکار: بزرگ‌ترین ردیف (۱۱ حرف) کل صفحه رو پر می‌کنه
            val maxChars = rows.maxOf { it.length }
            maxChars.coerceAtLeast(1)
        }

        // ✅ عرض ثابت هر دکمه (برای تراز بودن)
        val fixedKeyWidthPx: Int = if (configuredKeyWidth > 0) {
            dp(configuredKeyWidth)
        } else {
            // عرض = (عرض صفحه - فاصله‌ها) / تعداد کلید در بزرگ‌ترین ردیف
            val usableWidthPx = resources.displayMetrics.widthPixels - dp(8)
            val maxCount = maxKeysPerRow
            val totalGapPx = (maxCount + 1) * (dp(gapDp) + dp(2))
            ((usableWidthPx - totalGapPx) / maxCount).coerceAtLeast(dp(30))
        }

        rows.forEachIndexed { rowIndex, row ->
            val chunks = row.chunked(maxKeysPerRow)
            chunks.forEachIndexed { chunkIndex, chunk ->
                val line = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                }

                // Shift فقط در اولین خط از آخرین ردیف
                if (rowIndex == rows.size - 1 && chunkIndex == 0) {
                    line.addView(fixedWidthActionButton(
                        getString(R.string.key_shift), keyBg, fg, fixedKeyWidthPx
                    ) {
                        uppercase = !uppercase
                        refreshKeyboard()
                    })
                }

                chunk.forEach { character ->
                    val value = if (english && uppercase)
                        character.uppercaseChar().toString()
                    else character.toString()
                    // ✅ دکمه با عرض ثابت و پشتیبانی از نگه داشتن
                    line.addView(fixedWidthKeyButton(value, keyBg, fg, fixedKeyWidthPx) {
                        commit(value)
                    }, LinearLayout.LayoutParams(fixedKeyWidthPx, keyHeight()).apply {
                        setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
                    })
                }

                // Backspace فقط در آخرین خط از آخرین ردیف
                if (rowIndex == rows.size - 1 && chunkIndex == chunks.size - 1) {
                    line.addView(fixedWidthActionButton(
                        getString(R.string.key_backspace),
                        Color.rgb(190, 155, 215), Color.WHITE, fixedKeyWidthPx
                    ) {
                        currentInputConnection?.deleteSurroundingText(1, 0)
                        refreshSuggestions()
                    })
                }

                root.addView(line, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
            }
        }
        buildBottomRow(root, keyBg, fg)
    }

    // ✅ دکمه با عرض ثابت (برای تراز بودن)
    private fun fixedWidthKeyButton(
        text: String,
        bg: Int,
        fg: Int,
        widthPx: Int,
        click: () -> Unit
    ): TextView {
        return keyButton(text, bg, fg) { click() }.apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, keyHeight())
        }
    }

    private fun fixedWidthActionButton(
        text: String,
        bg: Int,
        fg: Int,
        widthPx: Int,
        click: () -> Unit
    ): TextView {
        return keyButton(text, bg, fg, 14f) { click() }.apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, keyHeight()).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        }
    }

    private fun buildBottomRow(root: LinearLayout, keyBg: Int, fg: Int) {
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        bottom.addView(actionButton("?123", keyBg, fg) {
            toggleMode { numberMode = true }
        })
        bottom.addView(actionButton(getString(R.string.key_comma), keyBg, fg) {
            commit(",")
        })
        bottom.addView(actionButton(getString(R.string.key_globe), keyBg, fg) {
            english = !english
            refreshKeyboard()
        })
        bottom.addView(keyButton(getString(R.string.key_space), keyBg, fg) {
            commit(" ")
        }, LinearLayout.LayoutParams(0, keyHeight(), 3f).apply {
            setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
        })
        bottom.addView(actionButton(getString(R.string.key_dot), keyBg, fg) {
            commit(".")
        })
        bottom.addView(actionButton(
            getString(R.string.key_enter),
            Color.rgb(103, 80, 164), Color.WHITE
        ) { sendEnter() })
        root.addView(bottom)
    }

    private fun buildNumberPad(root: LinearLayout, keyBg: Int, fg: Int) {
        val rows = listOf(
            listOf("۷","۸","۹","⌫"),
            listOf("۴","۵","۶","+"),
            listOf("۱","۲","۳","−"),
            listOf("۰","۰۰","×","÷")
        )
        rows.forEach { row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { value ->
                line.addView(keyButton(value, keyBg, fg, 20f) {
                    when (value) {
                        "⌫" -> {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                            refreshSuggestions()
                        }
                        else -> commit(if (english) toEnglishDigits(value) else value)
                    }
                }, LinearLayout.LayoutParams(0, keyHeight(), 1f).apply {
                    setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
                })
            }
            root.addView(line)
        }

        val symbolsRow = listOf("؟","%","@","&","*","(",")","!","+","_","=",".")
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
        controls.addView(actionButton(getString(R.string.mode_letters), keyBg, fg) {
            numberMode = false
            refreshKeyboard()
        })
        controls.addView(actionButton(getString(R.string.mode_symbols), keyBg, fg) {
            symbolMode = true
            numberMode = false
            refreshKeyboard()
        })
        controls.addView(actionButton(getString(R.string.mode_emoji), keyBg, fg) {
            emojiMode = true
            numberMode = false
            refreshKeyboard()
        })
        controls.addView(actionButton(
            getString(R.string.key_enter),
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
        controls.addView(actionButton(getString(R.string.mode_numbers), keyBg, fg) {
            symbolMode = false
            numberMode = true
            refreshKeyboard()
        })
        controls.addView(actionButton(getString(R.string.mode_emoji), keyBg, fg) {
            symbolMode = false
            emojiMode = true
            refreshKeyboard()
        })
        controls.addView(actionButton(
            getString(R.string.mode_letters),
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
            getString(R.string.mode_back_to_letters),
            Color.rgb(103, 80, 164), Color.WHITE
        ) {
            emojiMode = false
            refreshKeyboard()
        })
    }

    private fun buildClipboard(root: LinearLayout, keyBg: Int, fg: Int) {
        val list = loadClipboard()
        val scroller = ScrollView(this)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (list.length() == 0) {
            column.addView(label(getString(R.string.clipboard_empty), 15f, fg))
        }
        for (i in list.length() - 1 downTo 0) {
            val item = list.optJSONObject(i) ?: continue
            val text = item.optString("text")
            val pinned = item.optBoolean("pinned")
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(keyButton(
                (if (pinned) getString(R.string.clipboard_pinned_prefix) else "") +
                    text.take(45),
                keyBg, fg, 13f
            ) { commit(text) }, LinearLayout.LayoutParams(0, dp(52), 1f))
            row.addView(actionButton(
                if (pinned) getString(R.string.clipboard_pin)
                else getString(R.string.clipboard_unpin),
                keyBg, fg
            ) {
                item.put("pinned", !pinned)
                saveClipboard(list)
                refreshKeyboard()
            })
            row.addView(actionButton(getString(R.string.clipboard_delete), keyBg, fg) {
                if (!pinned) {
                    list.remove(i)
                    saveClipboard(list)
                    refreshKeyboard()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.clipboard_pin_first),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
            column.addView(row)
        }
        scroller.addView(column)
        root.addView(scroller, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton(
            getString(R.string.clipboard_save_current), keyBg, fg
        ) {
            saveClipboardCurrent()
            refreshKeyboard()
        })
        actions.addView(actionButton(
            getString(R.string.mode_back),
            Color.rgb(103, 80, 164), Color.WHITE
        ) {
            clipboardMode = false
            refreshKeyboard()
        })
        root.addView(actions)
    }

    private fun addKeyboardRow(root: LinearLayout, line: LinearLayout) {
        root.addView(line, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun keyLayoutParams(rowLength: Int): LinearLayout.LayoutParams {
        val configuredWidth = prefs.getInt("key_width", 0)
        return if (configuredWidth > 0) {
            LinearLayout.LayoutParams(dp(configuredWidth), keyHeight()).apply {
                setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
            }
        } else {
            LinearLayout.LayoutParams(0, keyHeight(), 1f).apply {
                setMargins(dp(gap()), dp(2), dp(gap()), dp(2))
            }
        }
    }

    private fun keyHeight(): Int {
        val configured = prefs.getInt("key_height", 48)
        return dp(if (configured <= 0) 48 else configured)
    }

    private fun gap(): Int = prefs.getInt("key_gap", 4).coerceAtLeast(0)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER_VERTICAL
    }

    // ✅ دکمه با قابلیت نگه داشتن برای کاراکترهای جایگزین
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

            // ✅ نگه داشتن برای کاراکترهای جایگزین
            val alternates = alternateChars[text.lowercase()] ?: alternateChars[text]
            if (!alternates.isNullOrEmpty()) {
                setOnLongClickListener { v ->
                    showAlternativesPopup(v, alternates)
                    true
                }
            }
        }
    }

    // ✅ نمایش پاپ‌آپ کاراکترهای جایگزین
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
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_NONE
        val handled = currentInputConnection?.performEditorAction(action) ?: false
        if (!handled) {
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
            )
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
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) return
            val value = clip.getItemAt(0)
                .coerceToText(this)
                ?.toString()
                ?.trim()
                .orEmpty()
            if (value.isBlank()) return

            val list = loadClipboard()
            if ((0 until list.length()).any {
                    list.optJSONObject(it)?.optString("text") == value
                }
            ) return

            list.put(JSONObject().put("text", value).put("pinned", false))

            while (list.length() > 100) {
                var removeIndex = -1
                for (i in 0 until list.length()) {
                    if (!list.optJSONObject(i).optBoolean("pinned")) {
                        removeIndex = i
                        break
                    }
                }
                if (removeIndex < 0) break
                list.remove(removeIndex)
            }
            saveClipboard(list)
        } catch (_: Exception) {
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
