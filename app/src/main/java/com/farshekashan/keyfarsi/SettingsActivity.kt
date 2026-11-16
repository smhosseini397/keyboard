package com.farshekashan.keyfarsi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("keyfarsi", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F2FA.toInt())
        }

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(8), dp(4), 0)
        }

        val tabContent = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val generalScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(createGeneralTab())
        }
        val standardScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(createStandardTab())
            visibility = View.GONE
        }
        val englishScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(createEnglishTab())
            visibility = View.GONE
        }
        val themeScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(createThemeTab())
            visibility = View.GONE
        }

        tabContent.addView(generalScroll)
        tabContent.addView(standardScroll)
        tabContent.addView(englishScroll)
        tabContent.addView(themeScroll)

        val btnGeneral = Button(this).apply {
            text = "عمومی"
            setOnClickListener {
                generalScroll.visibility = View.VISIBLE
                standardScroll.visibility = View.GONE
                englishScroll.visibility = View.GONE
                themeScroll.visibility = View.GONE
            }
        }
        val btnStandard = Button(this).apply {
            text = "استاندارد"
            setOnClickListener {
                generalScroll.visibility = View.GONE
                standardScroll.visibility = View.VISIBLE
                englishScroll.visibility = View.GONE
                themeScroll.visibility = View.GONE
            }
        }
        val btnEnglish = Button(this).apply {
            text = "انگلیسی"
            setOnClickListener {
                generalScroll.visibility = View.GONE
                standardScroll.visibility = View.GONE
                englishScroll.visibility = View.VISIBLE
                themeScroll.visibility = View.GONE
            }
        }
        val btnTheme = Button(this).apply {
            text = "تم و ظاهر"
            setOnClickListener {
                generalScroll.visibility = View.GONE
                standardScroll.visibility = View.GONE
                englishScroll.visibility = View.GONE
                themeScroll.visibility = View.VISIBLE
            }
        }

        tabBar.addView(btnGeneral, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        tabBar.addView(btnStandard, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        tabBar.addView(btnEnglish, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        tabBar.addView(btnTheme, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })

        mainLayout.addView(tabBar)
        mainLayout.addView(tabContent)
        setContentView(mainLayout)

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun createGeneralTab(): LinearLayout {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(16), dp(22), dp(24))
        }

        val title = TextView(this).apply {
            text = "⌨️  کلید فارسی"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFF332B45.toInt())
            setPadding(0, dp(8), 0, dp(20))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        outer.addView(title)

        val enable = Button(this).apply {
            text = "۱. فعال‌سازی کیبورد"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }
        outer.addView(enable, match())

        val choose = Button(this).apply {
            text = "۲. انتخاب کلید فارسی"
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        }
        outer.addView(choose, match())

        outer.addView(sectionTitle("تنظیمات عمومی (حالت عمودی)"))
        addNumberField(outer, "عرض کیبورد فارسی (dp)", "fa_keyboard_width", 360, 0, 1200)
        addNumberField(outer, "ارتفاع کل کیبورد فارسی (dp)", "fa_keyboard_height", 270, 150, 800)
        addNumberField(outer, "عرض دکمه‌ها (dp) - ۰ برای خودکار", "fa_key_width", 0, 0, 200)
        addNumberField(outer, "ارتفاع دکمه‌ها (dp)", "fa_key_height", 48, 30, 120)
        addNumberField(outer, "فاصله بین دکمه‌ها (dp)", "fa_key_gap", 4, 0, 20)

        // ✅ فیلد جدید: حاشیه امن (۱ تا ۵ پیکسل)
        addNumberField(outer, "حاشیه امن (پیکسل) - برای جلوگیری از بیرون‌زدگی دکمه‌ها", "keyboard_padding", 2, 1, 5)

        outer.addView(sectionTitle("تنظیمات عمومی (حالت افقی)"))
        addNumberField(outer, "عرض کیبورد در حالت افقی (dp) - ۰ برای خودکار", "keyboard_width_landscape", 0, 0, 1200)
        addNumberField(outer, "ارتفاع کیبورد در حالت افقی (dp) - ۰ برای خودکار", "keyboard_height_landscape", 0, 0, 800)

        return outer
    }

    private fun createStandardTab(): LinearLayout {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(16), dp(22), dp(24))
        }

        val title = TextView(this).apply {
            text = "کیبورد استاندارد"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(0xFF332B45.toInt())
            setPadding(0, dp(8), 0, dp(20))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        outer.addView(title)

        val standardBig = CheckBox(this).apply {
            text = "فعال‌سازی کیبورد استاندارد"
            isChecked = prefs.getBoolean("standard_big_keyboard", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("standard_big_keyboard", checked).apply()
                Toast.makeText(
                    this@SettingsActivity,
                    "برای اعمال تغییرات، کیبورد را ببندید و دوباره باز کنید",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        outer.addView(standardBig)

        outer.addView(sectionTitle("تنظیمات اندازه کیبورد استاندارد"))
        addNumberField(outer, "عرض کیبورد استاندارد (dp) - ۰ برای خودکار", "standard_keyboard_width", 0, 0, 1200)
        addNumberField(outer, "ارتفاع کیبورد استاندارد (dp) - ۰ برای خودکار", "standard_keyboard_height", 0, 0, 800)
        addNumberField(outer, "عرض دکمه‌های استاندارد (dp) - ۰ برای خودکار", "standard_key_width", 0, 0, 200)
        addNumberField(outer, "ارتفاع دکمه‌های استاندارد (dp) - ۰ برای خودکار", "standard_key_height", 0, 0, 120)
        addNumberField(outer, "فاصله بین دکمه‌ها (dp) - ۰ برای پیش‌فرض", "standard_key_gap", -1, -1, 20)
        addNumberField(outer, "مقیاس کیبورد استاندارد (٪)", "standard_scale", 100, 50, 150)

        return outer
    }

    private fun createEnglishTab(): LinearLayout {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(16), dp(22), dp(24))
        }

        val title = TextView(this).apply {
            text = "کیبورد انگلیسی"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(0xFF332B45.toInt())
            setPadding(0, dp(8), 0, dp(20))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        outer.addView(title)

        outer.addView(sectionTitle("تنظیمات انگلیسی (عمودی)"))
        addNumberField(outer, "عرض کیبورد انگلیسی (dp)", "en_keyboard_width", 360, 0, 1200)
        addNumberField(outer, "ارتفاع کل کیبورد انگلیسی (dp)", "en_keyboard_height", 270, 150, 800)
        addNumberField(outer, "عرض دکمه‌ها (dp) - ۰ برای خودکار", "en_key_width", 0, 0, 200)
        addNumberField(outer, "ارتفاع دکمه‌ها (dp)", "en_key_height", 48, 30, 120)
        addNumberField(outer, "فاصله بین دکمه‌ها (dp)", "en_key_gap", 4, 0, 20)

        return outer
    }

    private fun createThemeTab(): LinearLayout {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(16), dp(22), dp(24))
        }

        val title = TextView(this).apply {
            text = "ظاهر و تم"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(0xFF332B45.toInt())
            setPadding(0, dp(8), 0, dp(20))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        outer.addView(title)

        val dark = CheckBox(this).apply {
            text = "تم تیره (برای تم پیش‌فرض)"
            isChecked = prefs.getBoolean("dark", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("dark", checked).apply()
            }
        }
        outer.addView(dark)

        val themeLabel = TextView(this).apply {
            text = "انتخاب تم کیبورد:"
            textSize = 14f
            setTextColor(0xFF332B45.toInt())
            setPadding(0, dp(12), 0, dp(4))
        }
        outer.addView(themeLabel)

        val themes = arrayOf("default" to "پیش‌فرض", "retro" to "رترو (کیبورد قدیمی)", "neon" to "نئون", "glass" to "شیشه‌ای")
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                themes.map { it.second }
            )
            val currentTheme = prefs.getString("theme", "default")
            setSelection(themes.indexOfFirst { it.first == currentTheme }.coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    prefs.edit().putString("theme", themes[position].first).apply()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        outer.addView(spinner, match())

        val reset = Button(this).apply {
            text = "بازنشانی همه تنظیمات به پیش‌فرض"
            setOnClickListener {
                prefs.edit()
                    .putInt("fa_keyboard_width", 360)
                    .putInt("fa_keyboard_height", 270)
                    .putInt("en_keyboard_width", 360)
                    .putInt("en_keyboard_height", 270)
                    .putInt("keyboard_width_landscape", 0)
                    .putInt("keyboard_height_landscape", 0)
                    .putInt("fa_key_width", 0)
                    .putInt("fa_key_height", 48)
                    .putInt("fa_key_gap", 4)
                    .putInt("en_key_width", 0)
                    .putInt("en_key_height", 48)
                    .putInt("en_key_gap", 4)
                    .putInt("standard_keyboard_width", 0)
                    .putInt("standard_keyboard_height", 0)
                    .putInt("standard_key_width", 0)
                    .putInt("standard_key_height", 0)
                    .putInt("standard_key_gap", -1)
                    .putInt("standard_scale", 100)
                    .putInt("keyboard_padding", 2) // ✅ بازنشانی حاشیه امن
                    .putBoolean("dark", false)
                    .putString("theme", "default")
                    .putBoolean("standard_big_keyboard", false)
                    .apply()
                Toast.makeText(this@SettingsActivity, "تنظیمات بازنشانی شد", Toast.LENGTH_SHORT).show()
                recreate()
            }
        }
        outer.addView(reset, match())

        val note = TextView(this).apply {
            text = "مقدار ۰ در هر فیلد به معنای «خودکار» است."
            textSize = 12f
            setPadding(0, dp(12), 0, dp(8))
            setTextColor(0xFF8A8399.toInt())
        }
        outer.addView(note)

        return outer
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun match() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(8) }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(0xFF332B45.toInt())
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun addNumberField(
        parent: LinearLayout,
        label: String,
        key: String,
        default: Int,
        minValue: Int,
        maxValue: Int
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(4))
        }

        val labelView = TextView(this).apply {
            text = "$label  (${minValue} تا ${maxValue})"
            textSize = 14f
            setTextColor(0xFF332B45.toInt())
        }
        row.addView(labelView)

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(prefs.getInt(key, default).toString())
            hint = "عدد دلخواه"
        }
        row.addView(input)

        val seek = SeekBar(this).apply {
            this.max = maxValue - minValue
            progress = (prefs.getInt(key, default) - minValue).coerceIn(0, this.max)
        }
        row.addView(seek)

        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val v = s?.toString()?.toIntOrNull()
                if (v != null && v in minValue..maxValue) {
                    seek.progress = v - minValue
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    input.setText((progress + minValue).toString())
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val save = Button(this).apply {
            text = "ذخیره"
            setOnClickListener {
                val value = input.text.toString().toIntOrNull()
                when {
                    value == null -> Toast.makeText(
                        this@SettingsActivity, "یک عدد معتبر وارد کنید", Toast.LENGTH_SHORT
                    ).show()
                    value < minValue -> Toast.makeText(
                        this@SettingsActivity, "حداقل مقدار $minValue است", Toast.LENGTH_SHORT
                    ).show()
                    value > maxValue -> Toast.makeText(
                        this@SettingsActivity, "حداکثر مقدار $maxValue است", Toast.LENGTH_SHORT
                    ).show()
                    else -> {
                        prefs.edit().putInt(key, value).apply()
                        Toast.makeText(this@SettingsActivity, "ذخیره شد", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        row.addView(save)

        parent.addView(row)
    }
}
