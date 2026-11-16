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

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
            setBackgroundColor(0xFFF5F2FA.toInt())
            clipToPadding = false
            fitsSystemWindows = false
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

        val intro = TextView(this).apply {
            text = "کیبورد فارسی و انگلیسی با صفحه اعداد ماشین‌حسابی و کلیپ‌بورد"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
            setTextColor(0xFF6B6480.toInt())
        }
        outer.addView(intro)

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

        outer.addView(sectionTitle("تنظیمات اندازه (حالت عمودی)"))

        addNumberField(outer, "عرض کیبورد (dp)", "keyboard_width", 360, 0, 1200)
        addNumberField(outer, "ارتفاع کل کیبورد (dp)", "keyboard_height", 270, 150, 800)
        addNumberField(outer, "عرض دکمه‌ها (dp) - ۰ برای خودکار", "key_width", 0, 0, 200)
        addNumberField(outer, "ارتفاع دکمه‌ها (dp)", "key_height", 48, 30, 120)
        addNumberField(outer, "فاصله بین دکمه‌ها (dp)", "key_gap", 4, 0, 20)

        outer.addView(sectionTitle("تنظیمات اندازه (حالت افقی)"))

        // ✅ تنظیمات عرض و ارتفاع در حالت افقی
        addNumberField(outer, "عرض کیبورد در حالت افقی (dp) - ۰ برای خودکار", "keyboard_width_landscape", 0, 0, 1200)
        addNumberField(outer, "ارتفاع کیبورد در حالت افقی (dp) - ۰ برای خودکار", "keyboard_height_landscape", 0, 0, 800)

        outer.addView(sectionTitle("تنظیمات کیبورد استاندارد"))

        // ✅ تنظیمات اختصاصی برای کیبورد استاندارد
        addNumberField(outer, "عرض دکمه استاندارد (dp) - ۰ برای خودکار", "standard_key_width", 0, 0, 200)
        addNumberField(outer, "ارتفاع دکمه استاندارد (dp) - ۰ برای خودکار", "standard_key_height", 0, 0, 120)
        addNumberField(outer, "مقیاس کیبورد استاندارد (٪)", "standard_scale", 100, 50, 150)

        outer.addView(sectionTitle("ظاهر و تم"))

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

        val standardBig = CheckBox(this).apply {
            text = "کیبورد بزرگ استاندارد (حروف سر جای استاندارد با فاصله بیشتر)"
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

        val reset = Button(this).apply {
            text = "بازنشانی همه تنظیمات به پیش‌فرض"
            setOnClickListener {
                prefs.edit()
                    .putInt("keyboard_width", 360)
                    .putInt("keyboard_height", 270)
                    .putInt("keyboard_width_landscape", 0)
                    .putInt("keyboard_height_landscape", 0)
                    .putInt("key_width", 0)
                    .putInt("key_height", 48)
                    .putInt("key_gap", 4)
                    .putInt("standard_key_width", 0)
                    .putInt("standard_key_height", 0)
                    .putInt("standard_scale", 100)
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
            text = "تغییر اندازه پس از باز و بسته کردن کیبورد اعمال می‌شود.\n" +
                "مقدار ۰ در هر فیلد به معنای «خودکار» است.\n" +
                "برای کیبورد استاندارد، اگر عرض/ارتفاع دکمه را ۰ بگذارید، به صورت خودکار محاسبه می‌شود."
            textSize = 12f
            setPadding(0, dp(12), 0, dp(8))
            setTextColor(0xFF8A8399.toInt())
        }
        outer.addView(note)

        val scroller = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            fitsSystemWindows = false
            addView(outer)
        }
        setContentView(scroller)

        ViewCompat.setOnApplyWindowInsetsListener(scroller) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

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
