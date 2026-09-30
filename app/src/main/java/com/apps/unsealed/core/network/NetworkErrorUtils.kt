package com.apps.unsealed.core.network

import android.content.Context
import androidx.annotation.StringRes
import com.apps.unsealed.R
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import retrofit2.HttpException
import java.io.IOException

/**
 * Checks whether this [Throwable] represents a network or connectivity issue,
 * including Firebase SDK network errors, socket failures, DNS timeouts, and unreachable hosts.
 */
fun Throwable.isNetworkOrConnectionIssue(): Boolean {
    if (this is IOException) return true
    if (this is FirebaseNetworkException) return true
    if (this is FirebaseException) {
        val msg = message?.lowercase().orEmpty()
        if (msg.contains("network error") ||
            msg.contains("failed to connect") ||
            msg.contains("timeout") ||
            msg.contains("unreachable host") ||
            msg.contains("unreachable") ||
            msg.contains("connection closed") ||
            msg.contains("connection reset") ||
            msg.contains("handshake") ||
            msg.contains("securetoken.googleapis.com") ||
            msg.contains("identitytoolkit.googleapis.com")
        ) {
            return true
        }
        var cause = this.cause
        while (cause != null) {
            if (cause is IOException || cause is FirebaseNetworkException) return true
            cause = cause.cause
        }
    }
    return false
}

/**
 * Converts any network/API [Throwable] into a clean, localized, human-readable error message,
 * resolving String resource IDs via [context] in accordance with `docs/string-resources.md`.
 */
fun Throwable.toUserFacingMessage(
    context: Context,
    @StringRes defaultResId: Int = R.string.error_generic,
): String {
    val resId = toUserFacingResId(defaultResId)
    return context.getString(resId)
}

/**
 * Maps a network/API [Throwable] to an appropriate `@StringRes` ID.
 */
@StringRes
fun Throwable.toUserFacingResId(@StringRes defaultResId: Int = R.string.error_generic): Int {
    if (this is HttpException) {
        val httpCode = code()
        val rawBody = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val parsedError = extractErrorFromRawJson(rawBody) ?: message()

        return mapErrorStringToResId(parsedError, httpCode, defaultResId)
    }

    if (this.isNetworkOrConnectionIssue()) {
        return R.string.error_network
    }

    val msg = message ?: return defaultResId
    return mapErrorStringToResId(msg, null, defaultResId)
}

private fun extractErrorFromRawJson(json: String?): String? {
    if (json.isNullOrBlank()) return null
    val errorMatch = Regex(""""error"\s*:\s*"([^"]+)"""").find(json)
    if (errorMatch != null) return errorMatch.groupValues[1]

    val messageMatch = Regex(""""message"\s*:\s*"([^"]+)"""").find(json)
    if (messageMatch != null) return messageMatch.groupValues[1]

    return null
}

@StringRes
private fun mapErrorStringToResId(
    rawMessage: String,
    httpCode: Int?,
    @StringRes fallbackResId: Int,
): Int {
    val lower = rawMessage.lowercase()

    // Handle network / connection error phrases (e.g. Firebase Auth endpoint failure)
    if (lower.contains("failed to connect") ||
        lower.contains("network error") ||
        lower.contains("unreachable host") ||
        lower.contains("timeout") ||
        lower.contains("connection refused") ||
        lower.contains("connection reset")
    ) {
        return R.string.error_network
    }

    // Handle 401 / Unauthorized (guest mode, missing token, or expired session)
    if (httpCode == 401 || lower.contains("unauthorized") || lower.contains("401")) {
        return R.string.error_unauthorized
    }

    // Handle 403 / Forbidden
    if (httpCode == 403 || lower.contains("forbidden") || lower.contains("403")) {
        return R.string.error_forbidden
    }

    // Handle 404 / Not Found
    if (httpCode == 404 || lower.contains("not found") || lower.contains("404")) {
        return R.string.error_not_found
    }

    // Handle 500+ / Server error
    if ((httpCode != null && httpCode >= 500) || lower.contains("500") || lower.contains("internal server error")) {
        return R.string.error_server
    }

    // Filter out ugly raw HTTP exception strings like "HTTP 401 ", "HTTP 500 "
    if (lower.startsWith("http ")) {
        return fallbackResId
    }

    return fallbackResId
}
