package com.example.metrogo

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/** Reads the prefs that PreferencesPage already saves ("text_size", "high_contrast"). */
object AccessibilitySettings {
    private const val PREFS = "metrogo_prefs"
    const val KEY_TEXT_SIZE = "text_size"
    const val KEY_HIGH_CONTRAST = "high_contrast"
    val textSizes = arrayOf("Small", "Medium", "Large")

    const val KEY_REDUCE_MOTION = "reduce_motion"

    /** True if the user turned it on in Preferences OR Android's "remove animations" is on. */
    fun isReduceMotion(c: Context): Boolean =
        prefs(c).getBoolean(KEY_REDUCE_MOTION, false) ||
                Settings.Global.getFloat(c.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    fun setReduceMotion(c: Context, on: Boolean) =
        prefs(c).edit().putBoolean(KEY_REDUCE_MOTION, on).apply()

    fun isReduceMotionPref(c: Context) = prefs(c).getBoolean(KEY_REDUCE_MOTION, false)
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun textSize(c: Context): String = prefs(c).getString(KEY_TEXT_SIZE, "Medium") ?: "Medium"
    fun setTextSize(c: Context, value: String) = prefs(c).edit().putString(KEY_TEXT_SIZE, value).apply()

    fun isHighContrast(c: Context) = prefs(c).getBoolean(KEY_HIGH_CONTRAST, false)
    fun setHighContrast(c: Context, on: Boolean) = prefs(c).edit().putBoolean(KEY_HIGH_CONTRAST, on).apply()

    private fun factor(c: Context) = when (textSize(c)) {
        "Small" -> 0.9f
        "Large" -> 1.25f
        else -> 1f
    }

    /** Multiplies the system font scale, so the user's Android setting still counts. */
    fun wrap(base: Context): Context {
        val f = factor(base)
        if (f == 1f) return base
        val cfg = Configuration(base.resources.configuration)
        cfg.fontScale = (cfg.fontScale * f).coerceAtMost(2f)
        return base.createConfigurationContext(cfg)
    }
}

/** Extend this instead of AppCompatActivity on any screen that should honour the text-size setting. */
open class AccessibleActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AccessibilitySettings.wrap(newBase))
    }
}

/** Tiny text-to-speech wrapper. */
class Speaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ready = true
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    fun speak(text: String) {
        if (!ready) { pending = text; return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "metrogo")
    }

    fun stop() { tts?.stop() }

    fun shutdown() {
        tts?.stop(); tts?.shutdown(); tts = null
    }
}