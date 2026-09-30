package com.apps.unsealed.core.network

import com.squareup.moshi.JsonClass

/**
 * Envelope every backend endpoint under `/api/v1` uses:
 * `{"success": true, "data": {...}}` on success, `{"success": false, "error": "message"}` on failure.
 * See `docs/backend-roadmap.md` §M0.
 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
)

/** Unwraps [ApiResponse.data], surfacing the backend's [ApiResponse.error] message on failure. */
fun <T> ApiResponse<T>.unwrap(): T = data ?: error(error ?: "Empty response body")
