package com.farshekashan.keyfarsi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("keyfarsi", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 24)
            setBackgroundColor(0xFFF5F2FA.toInt())
        }
        val title = TextView(this).apply {
            text = "⌨️  کلید فارسی"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFF332B45.toInt())
            setPadding(0, 8, 0, 20)
        }
        outer.addView(title)
        val intro = TextView(this).apply {
            text = "کیبورد فارسی و انگلیسی با صفحه اعداد ماشین‌حسابی و کلیپ‌بورد"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        outer.addView(intro)

        val enable = Button(this).apply {
            text = "۱. فعال‌سازی کیبورد"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
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

        addNumberField(outer, "عرض کیبورد (dp)", "keyboard_width", 360)
        addNumberField(outer, "ارتفاع کل کیبورد (dp)", "keyboard_height", 270)
        addNumberField(outer, "عرض دکمه‌ها (dp)", "key_width", 0)
        addNumberField(outer, "ارتفاع دکمه‌ها (dp)", "key_height", 48)
        addNumberField(outer, "فاصله بین دکمه‌ها (dp)", "key_gap", 4)

        val dark = CheckBox(this).apply {
            text = "تم تیره"
            isChecked = prefs.getBoolean("dark", false)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("dark", checked).apply() }
        }
        outer.addView(dark)

        val note = TextView(this).apply {
            text = "تغییر اندازه پس از باز و بسته کردن کیبورد اعمال می‌شود. عرض ۰ برای عرض خودکار است."
            textSize = 12f
            setPadding(0, 12, 0, 0)
        }
        outer.addView(note)
        setContentView(ScrollView(this).apply { addView(outer) })
    }

    private fun match() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 8 }

    private fun addNumberField(parent: LinearLayout, label: String, key: String, default: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 4)
        }
        row.addView(TextView(this).apply { text = label; textSize = 14f })
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(prefs.getInt(key, default).toString())
            hint = "عدد دلخواه"
        }
        row.addView(input)
        val save = Button(this).apply {
            text = "ذخیره"
            setOnClickListener {
                val value = input.text.toString().toIntOrNull()
                if (value == null || value < 0) {
                    Toast.makeText(this@SettingsActivity, "یک عدد معتبر وارد کنید", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit().putInt(key, value).apply()
                    Toast.makeText(this@SettingsActivity, "ذخیره شد", Toast.LENGTH_SHORT).show()
                }
            }
        }
        row.addView(save)
        parent.addView(row)
    }
}
