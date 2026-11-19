package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

internal fun resolveRecaptchaSiteKey(context: Context): String = runCatching {
    context.getString(R.string.recaptcha_site_key).trim()
}.getOrDefault("")

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val siteKey = resolveRecaptchaSiteKey(context)
            if (siteKey.isNotBlank()) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
            } else {
                Log.e(TAG, "recaptcha_site_key resource string is blank.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

fun getLocalAppCheckDebugToken(context: Context): String? {
    if (!BuildConfig.DEBUG) return null
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        prefs.getString(DEBUG_SECRET_KEY, null)?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

suspend fun diagnoseAppCheckFailure(context: Context): String? {
    if (BuildConfig.DEBUG) return null
    val siteKey = resolveRecaptchaSiteKey(context)
    val keyPreview = if (siteKey.length >= 10) {
        "${siteKey.take(6)}…${siteKey.takeLast(4)}"
    } else if (siteKey.isNotBlank()) {
        siteKey
    } else {
        "отсутствует"
    }

    return try {
        val result = withTimeoutOrNull(8_000L) {
            FirebaseAppCheck.getInstance().getAppCheckToken(true).await()
        }
        if (result == null) {
            "Диагностика (key=$keyPreview): таймаут получения токена reCAPTCHA."
        } else if (result.token.isBlank()) {
            "Диагностика (key=$keyPreview): получен пустой App Check токен."
        } else {
            "Диагностика (key=$keyPreview): клиент получил токен, но Firebase отклонил его — проверьте, что в Firebase Console → App Check нажата кнопка Save и выбран этот же ключ."
        }
    } catch (e: Exception) {
        val detail = e.localizedMessage ?: e.message ?: e::class.java.simpleName
        "Диагностика (key=$keyPreview): ошибка получения токена — $detail"
    }
}
