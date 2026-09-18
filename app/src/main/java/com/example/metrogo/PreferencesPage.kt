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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PreferencesPage : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    // Keeping the option lists here means adding a new language/size later
    // only requires editing this array, not the click-handling logic below.
    private val languages = arrayOf("English", "Afrikaans", "isiZulu", "isiXhosa", "Sesotho")
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

        // Changing the Applicatoin Language via utilising an API
        val tvLanguageValue = findViewById<TextView>(R.id.tvLanguageValue)
        tvLanguageValue.text = prefs.getString("app_language", "English")

        val rowLanguage = findViewById<LinearLayout>(R.id.rowLanguage)
        rowLanguage.setOnClickListener {
            val current = languages.indexOf(tvLanguageValue.text.toString()).coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle("App Language")
                .setSingleChoiceItems(languages, current) { dialog, which ->
                    val selected = languages[which]
                    tvLanguageValue.text = selected
                    prefs.edit().putString("app_language", selected).apply()
                    // TODO: actually apply the locale change (Configuration/AppCompatDelegate.setApplicationLocales)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
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
            val current = textSizes.indexOf(tvTextSizeValue.text.toString()).coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle("Text Size")
                .setSingleChoiceItems(textSizes, current) { dialog, which ->
                    val selected = textSizes[which]
                    tvTextSizeValue.text = selected
                    prefs.edit().putString("text_size", selected).apply()
                    // TODO: apply the chosen scale app-wide (e.g. via a custom font-scale wrapper)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
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

}