package com.example.metrogo

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.TextView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.lang.ref.WeakReference

object LanguageManager {


    private const val PREFS_NAME = "metrogo_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val CACHE_PREFS_NAME = "metrogo_translation_cache"
    private const val RETRY_DELAY_MS = 30_000L


    data class Language(val name: String, val nativeName: String, val code: String) {
        val isEnglish: Boolean get() = code == TranslateLanguage.ENGLISH
    }

    val languages = listOf(
        Language("English", "English", TranslateLanguage.ENGLISH),
        Language("Afrikaans", "Afrikaans", TranslateLanguage.AFRIKAANS),
        Language("French", "Fran\u00e7ais", TranslateLanguage.FRENCH),
        Language("Portuguese", "Portugu\u00eas", TranslateLanguage.PORTUGUESE),
        Language("Swahili", "Kiswahili", TranslateLanguage.SWAHILI)
    )

    private val SKIP_IDS = setOf(
        "tvWelcomeName", "tvLanguageValue", "tvFromStation", "tvToStation",
        "tvMatchingSubtitle", "tvFareHold"
    )

    private val NEVER_TRANSLATE = setOf("MetroGO")

    private class Slot(
        val get: (TextView) -> String?,
        val set: (TextView, String) -> Unit,
        val originalTag: Int,
        val translatedTag: Int
    )

    private val TEXT_SLOT = Slot(
        { it.text?.toString() },
        { v, s -> v.text = s },
        R.id.tag_original_text,
        R.id.tag_translated_text
    )

    private val HINT_SLOT = Slot(
        { it.hint?.toString() },
        { v, s -> v.hint = s },
        R.id.tag_original_hint,
        R.id.tag_translated_hint
    )

    private lateinit var appContext: Context
    private val handler = Handler(Looper.getMainLooper())
    private var currentActivity: WeakReference<Activity>? = null
    private val dialogs = mutableListOf<WeakReference<Dialog>>()
    private val translators = HashMap<String, Translator>()
    private val readyModels = HashSet<String>()
    private val downloadWaiters = HashMap<String, MutableList<(Boolean) -> Unit>>()
    private val inFlight = HashSet<String>()
    private val failed = HashSet<String>()
    private var downloadFailedAt = 0L
    private var hasTranslated = false
    private var passScheduled = false
    private val passRunnable = Runnable { runPass() }


    fun init(app: Application) {
        appContext = app.applicationContext
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                currentActivity = WeakReference(activity)
                attachLayoutListener(activity)
                schedulePass(0)
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity?.get() === activity) currentActivity = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }

    private fun attachLayoutListener(activity: Activity) {
        val decor = activity.window.decorView
        if (decor.getTag(R.id.tag_layout_listener) != null) return
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            if (currentActivity?.get() === activity) schedulePass(100)
        }
        decor.viewTreeObserver.addOnGlobalLayoutListener(listener)
        decor.setTag(R.id.tag_layout_listener, listener)
    }

    private fun prefs(): SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun cachePrefs(): SharedPreferences =
        appContext.getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelected(): Language {
        val name = prefs().getString(KEY_LANGUAGE, "English")
        return languages.firstOrNull { it.name == name } ?: languages.first()
    }

    fun setSelected(language: Language) {
        prefs().edit().putString(KEY_LANGUAGE, language.name).apply()
    }

     fun onLanguageChanged() = schedulePass(0)

    fun tr(text: String): String {
        val language = getSelected()
        if (language.isEnglish) return text
        return cachePrefs().getString(key(language.code, text), null) ?: text
    }

    private fun translatorFor(code: String): Translator = translators.getOrPut(code) {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(code)
                .build()
        )
    }


    fun prepare(language: Language, onResult: (Boolean) -> Unit) {
        val code = language.code
        if (language.isEnglish || code in readyModels) {
            onResult(true)
            return
        }
        val waiters = downloadWaiters[code]
        if (waiters != null) {
            waiters.add(onResult)
            return
        }
        downloadWaiters[code] = mutableListOf(onResult)
        translatorFor(code)
            .downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                readyModels.add(code)
                downloadWaiters.remove(code)?.forEach { it(true) }
                schedulePass(0)
            }
            .addOnFailureListener {
                downloadFailedAt = System.currentTimeMillis()
                downloadWaiters.remove(code)?.forEach { it(false) }
            }
    }

    fun translateDialog(dialog: Dialog) {
        dialogs.add(WeakReference(dialog))
        val language = getSelected()
        if (language.isEnglish && !hasTranslated) return
        val missing = LinkedHashSet<String>()
        dialog.window?.decorView?.let { walk(it, language, missing) }
        if (missing.isNotEmpty()) request(language, missing)
    }

    private fun schedulePass(delayMs: Long) {
        if (passScheduled && delayMs > 0) return
        handler.removeCallbacks(passRunnable)
        passScheduled = true
        handler.postDelayed(passRunnable, delayMs)
    }

    private fun runPass() {
        passScheduled = false
        val language = getSelected()
        if (language.isEnglish && !hasTranslated) return

        val missing = LinkedHashSet<String>()
        currentActivity?.get()?.let { walk(it.window.decorView, language, missing) }
        dialogs.removeAll { it.get()?.isShowing != true }
        dialogs.forEach { ref ->
            ref.get()?.window?.decorView?.let { walk(it, language, missing) }
        }
        if (missing.isNotEmpty()) request(language, missing)
    }

    private fun walk(view: View, language: Language, missing: MutableSet<String>) {
        if (view is TextView && !isSkipped(view)) {
            if (view !is EditText) processSlot(view, TEXT_SLOT, language, missing)
            processSlot(view, HINT_SLOT, language, missing)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) walk(view.getChildAt(i), language, missing)
        }
    }

    private fun isSkipped(view: View): Boolean {
        if (view.id == View.NO_ID) return false
        return try {
            view.resources.getResourceEntryName(view.id) in SKIP_IDS
        } catch (e: Exception) {
            false
        }
    }

    private fun processSlot(v: TextView, slot: Slot, language: Language, missing: MutableSet<String>) {
        val current = slot.get(v)
        if (current.isNullOrEmpty()) return

        val lastTranslated = v.getTag(slot.translatedTag) as? String
        val storedOriginal = v.getTag(slot.originalTag) as? String
        val original =
            if (lastTranslated != null && storedOriginal != null && current == lastTranslated) storedOriginal
            else current

        if (language.isEnglish) {
            if (original != current) slot.set(v, original)
            v.setTag(slot.originalTag, null)
            v.setTag(slot.translatedTag, null)
            return
        }

        val resolved = resolve(language, original, missing) ?: return

        if (resolved == original) {
             if (current != original) slot.set(v, original)
            v.setTag(slot.originalTag, null)
            v.setTag(slot.translatedTag, null)
            return
        }

        if (current != resolved) slot.set(v, resolved)
        v.setTag(slot.originalTag, original)
        v.setTag(slot.translatedTag, resolved)
        hasTranslated = true
    }

    private fun resolve(language: Language, original: String, missing: MutableSet<String>): String? {
        var complete = true
        val lines = original.split("\n").map { line ->
            val trimmed = line.trim()
            if (!shouldTranslate(trimmed)) {
                line
            } else {
                val cached = cachePrefs().getString(key(language.code, trimmed), null)
                if (cached != null) {
                    line.replace(trimmed, cached)
                } else {
                    val k = key(language.code, trimmed)
                    if (k !in failed) missing.add(trimmed)
                    complete = false
                    line
                }
            }
        }
        return if (complete) lines.joinToString("\n") else null
    }

    private fun shouldTranslate(text: String): Boolean {
        if (text in NEVER_TRANSLATE) return false
        if (text.length > 500) return false
        if (text.count { it.isLetter() } < 2) return false // "R 100.00", "%"
        if (text.contains('@')) return false        // email
        if (text.none { it.isWhitespace() } && text.any { it.isDigit() }) return false // ids, times
        return true
    }

    private fun request(language: Language, texts: Set<String>) {
        if (System.currentTimeMillis() - downloadFailedAt < RETRY_DELAY_MS) return
        val toTranslate = texts.filter {
            val k = key(language.code, it)
            k !in failed && k !in inFlight
        }
        if (toTranslate.isEmpty()) return

        prepare(language) { ok ->
            if (!ok) return@prepare
            val translator = translatorFor(language.code)
            for (text in toTranslate) {
                val k = key(language.code, text)
                if (!inFlight.add(k)) continue
                translator.translate(text)
                    .addOnSuccessListener { result ->
                        inFlight.remove(k)
                        cachePrefs().edit().putString(k, result).apply()
                        schedulePass(50)
                    }
                    .addOnFailureListener {
                        inFlight.remove(k)
                        failed.add(k)
                    }
            }
        }
    }

    private fun key(code: String, text: String) = "$code|$text"
}