package com.example.metrogo

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PreferencesPage : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private val textSizes = arrayOf("Small", "Medium", "Large")

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. ALWAYS call super.onCreate() first
        super.onCreate(savedInstanceState)

        // 2. Setup preferences
        prefs = getSharedPreferences("metrogo_prefs", MODE_PRIVATE)

        // 3. Apply the saved theme preference BEFORE setting the content view
        val isDark = prefs.getBoolean("dark_theme", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )


        enableEdgeToEdge()
        setContentView(R.layout.activity_preferences_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Top bar navigation
        val circleSettings = findViewById<FrameLayout>(R.id.circleSettings)
        circleSettings.setOnClickListener {
            startActivity(Intent(this, SettingsPage::class.java))
        }

        val circleProfile = findViewById<FrameLayout>(R.id.circleProfile)
        circleProfile.setOnClickListener {
            startActivity(Intent(this, ProfileManagement::class.java))
        }

        val circleNotification = findViewById<FrameLayout>(R.id.circleNotification)
        circleNotification.setOnClickListener {
            startActivity(Intent(this, NotificationPage::class.java))
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // App language -- translated on-device with Google's ML Kit Translation API
        // (see LanguageManager). The choice is saved and applied to every screen.
        val tvLanguageValue = findViewById<TextView>(R.id.tvLanguageValue)
        tvLanguageValue.text = LanguageManager.getSelected().nativeName

        val rowLanguage = findViewById<LinearLayout>(R.id.rowLanguage)
        rowLanguage.setOnClickListener {
            val options = LanguageManager.languages
            val current = options
                .indexOfFirst { it.code == LanguageManager.getSelected().code }
                .coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle(LanguageManager.tr("App Language"))
                .setSingleChoiceItems(
                    options.map { it.nativeName }.toTypedArray(), current
                ) { dialog, which ->
                    dialog.dismiss()
                    changeLanguage(options[which], tvLanguageValue)
                }
                .setNegativeButton(LanguageManager.tr("Cancel"), null)
                .show()
        }

        // 7. App Theme (Light / Dark)
        val switchTheme = findViewById<Switch>(R.id.switchTheme)
        val ivThemeIcon = findViewById<ImageView>(R.id.ivThemeIcon)
        switchTheme.isChecked = isDark
        ivThemeIcon.setImageResource(if (isDark) R.drawable.ic_moon else R.drawable.ic_sun)

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // Save the preference FIRST
            prefs.edit().putBoolean("dark_theme", isChecked).apply()

            ivThemeIcon.setImageResource(if (isChecked) R.drawable.ic_moon else R.drawable.ic_sun)

            // Then apply the theme change (this will recreate the Activity)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // 8. Text Size
        val tvTextSizeValue = findViewById<TextView>(R.id.tvTextSizeValue)
        tvTextSizeValue.text = prefs.getString("text_size", "Medium")

        val rowTextSize = findViewById<LinearLayout>(R.id.rowTextSize)
        rowTextSize.setOnClickListener {
            // Read the saved value (not the label on screen, which may be translated).
            val current = textSizes.indexOf(prefs.getString("text_size", "Medium")).coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle(LanguageManager.tr("Text Size"))
                .setSingleChoiceItems(
                    textSizes.map { LanguageManager.tr(it) }.toTypedArray(), current
                ) { dialog, which ->
                    val selected = textSizes[which]
                    tvTextSizeValue.text = selected
                    prefs.edit().putString("text_size", selected).apply()
                    // TODO: apply the chosen scale app-wide (e.g. via a custom font-scale wrapper)
                    dialog.dismiss()
                }
                .setNegativeButton(LanguageManager.tr("Cancel"), null)
                .show()
        }

        // 9. High Contrast Mode
        val switchHighContrast = findViewById<Switch>(R.id.switchHighContrast)
        switchHighContrast.isChecked = prefs.getBoolean("high_contrast", false)
        switchHighContrast.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("high_contrast", isChecked).apply()
            // TODO: apply a high-contrast color scheme once one is defined
        }


    }

    /**
     * Downloads the language model if needed, then saves the choice and switches the app.
     * The choice is only saved once the language is actually usable.
     */
    private fun changeLanguage(language: LanguageManager.Language, label: TextView) {
        if (language.code == LanguageManager.getSelected().code) return

        if (!language.isEnglish) {
            Toast.makeText(
                this, "Preparing ${language.name} language pack\u2026", Toast.LENGTH_SHORT
            ).show()
        }

        LanguageManager.prepare(language) { ok ->
            if (ok) {
                LanguageManager.setSelected(language)
                label.text = language.nativeName
                LanguageManager.onLanguageChanged()
                Toast.makeText(
                    this, "Language changed to ${language.nativeName}", Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Couldn't download the ${language.name} language pack. " +
                            "Check your internet connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}