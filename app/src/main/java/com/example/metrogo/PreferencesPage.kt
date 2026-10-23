package com.example.metrogo

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.DecimalFormat

class PreferencesPage : AccessibleActivity() {

    private lateinit var prefs: SharedPreferences
    private var speaker: Speaker? = null

    private val textSizes = AccessibilitySettings.textSizes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("metrogo_prefs", MODE_PRIVATE)

        // Apply the saved theme preference BEFORE setting the content view
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
        findViewById<FrameLayout>(R.id.circleSettings).setOnClickListener {
            startActivity(Intent(this, SettingsPage::class.java))
        }
        findViewById<FrameLayout>(R.id.circleProfile).setOnClickListener {
            startActivity(Intent(this, ProfileManagement::class.java))
        }
        findViewById<FrameLayout>(R.id.circleNotification).setOnClickListener {
            startActivity(Intent(this, NotificationPage::class.java))
        }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // ---- Language ----
        val tvLanguageValue = findViewById<TextView>(R.id.tvLanguageValue)
        tvLanguageValue.text = LanguageManager.getSelected().nativeName

        findViewById<LinearLayout>(R.id.rowLanguage).setOnClickListener {
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

        // ---- App Theme (Light / Dark) ----
        val switchTheme = findViewById<Switch>(R.id.switchTheme)
        val ivThemeIcon = findViewById<ImageView>(R.id.ivThemeIcon)
        switchTheme.isChecked = isDark
        ivThemeIcon.setImageResource(if (isDark) R.drawable.ic_moon else R.drawable.ic_sun)

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            ivThemeIcon.setImageResource(if (isChecked) R.drawable.ic_moon else R.drawable.ic_sun)
            // This recreates the Activity
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // ---- Text Size (now actually applied) ----
        val tvTextSizeValue = findViewById<TextView>(R.id.tvTextSizeValue)
        tvTextSizeValue.text = AccessibilitySettings.textSize(this)

        findViewById<LinearLayout>(R.id.rowTextSize).setOnClickListener {
            val current = textSizes.indexOf(AccessibilitySettings.textSize(this)).coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle(LanguageManager.tr("Text Size"))
                .setSingleChoiceItems(
                    textSizes.map { LanguageManager.tr(it) }.toTypedArray(), current
                ) { dialog, which ->
                    val selected = textSizes[which]
                    dialog.dismiss()
                    if (selected != AccessibilitySettings.textSize(this)) {
                        AccessibilitySettings.setTextSize(this, selected)
                        recreate()   // re-applies the font scale to this screen immediately
                    }
                }
                .setNegativeButton(LanguageManager.tr("Cancel"), null)
                .show()
        }

        // ---- High Contrast (now actually applied) ----
        val switchHighContrast = findViewById<Switch>(R.id.switchHighContrast)
        switchHighContrast.isChecked = AccessibilitySettings.isHighContrast(this)   // BEFORE the listener
        switchHighContrast.setOnCheckedChangeListener { _, isChecked ->
            AccessibilitySettings.setHighContrast(this, isChecked)
            recreate()
        }

        // ---- Reduce Motion ----
        val switchReduceMotion = findViewById<Switch>(R.id.switchReduceMotion)
        switchReduceMotion.isChecked = AccessibilitySettings.isReduceMotionPref(this)
        switchReduceMotion.setOnCheckedChangeListener { _, isChecked ->
            AccessibilitySettings.setReduceMotion(this, isChecked)
        }

        // ---- Read Aloud ----
        findViewById<View>(R.id.rowReadAloud).setOnClickListener { readSummaryAloud() }

        if (AccessibilitySettings.isHighContrast(this)) applyHighContrast()
    }

    private fun readSummaryAloud() {
        val balance = TransportCardManager.getBalance(this)
        val ticket = TicketStore.getActiveTicket(this)
        val details = ticket?.let { TransportRouteRepository.scheduleDetails(it.scheduleId) }
        val text = buildString {
            append("Your balance is ${DecimalFormat("#,##0.00").format(balance)} rand. ")
            if (details != null) {
                append(
                    "Active boarding pass: ${details.route.routeName}, from ${details.originStop.stopName} " +
                            "to ${details.destinationStop.stopName}. Departs ${details.schedule.departureTime}, " +
                            "bus ${details.schedule.busRegistration}."
                )
            } else {
                append("You have no active boarding pass.")
            }
        }
        (speaker ?: Speaker(this).also { speaker = it }).speak(text)
    }

    /** Heavier borders and pure black/white text. Toggling the switch recreates the screen, so no "undo" is needed. */
    private fun applyHighContrast() {
        listOf(R.id.cardPreferences, R.id.cardAccessibility)
            .forEach { findViewById<View>(it).setBackgroundResource(R.drawable.bg_card_hc) }

        val black = ContextCompat.getColor(this, R.color.black)
        listOf(
            R.id.tvSectionDisplay, R.id.tvSectionAccessibility,
            R.id.tvLanguageValue, R.id.tvTextSizeValue, R.id.tvReduceMotionHint
        ).forEach { findViewById<TextView>(it).setTextColor(black) }
    }

    override fun onStop() {
        speaker?.stop()
        super.onStop()
    }

    override fun onDestroy() {
        speaker?.shutdown(); speaker = null
        super.onDestroy()
    }

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