package com.apps.unsealed.feature.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Semua endpoint yang return user (GET /me, POST /register, PATCH /me, PUT /interests)
 * membungkus data-nya dalam envelope `ApiResponse<UserDto>` (lihat `core/network/ApiResponse.kt`).
 */
@Serializable
@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val nickname: String,
    val continent: String,
    /** Spent via `POST /letters/{id}/unlock` to instant-deliver an in-transit
     * letter — see `docs/be/letters_api.md`. Default 100, no top-up path
     * (daily claim/IAP/regen) decided yet. */
    val energy: Int,
    val gender: String?,
    val birthday: String?,
    /** Spoken languages for pen pal matching — **not** the app's UI locale,
     * which the backend doesn't track at all (see `docs/be/profile_api.md`,
     * "Fresh user defaults" note, 2026-08-17). Replaces the old single
     * `language: String` field. */
    val languages: List<LanguageProficiencyDto>,
    @SerialName("photo_url") @Json(name = "photo_url") val photoUrl: String?,
    val bio: String,
    /** IDs only — the backend returns interests as a plain string array here,
     *  not the full [InterestDto] objects returned by the interests catalog. */
    val interests: List<String>,
    /** ISO 8601 timestamp set when user accepted TOS; null = not yet accepted.
     *  Backend gate returns 403 on Send and penpal match until this is set.
     *  Migration only backfilled seed users — real existing users start as null. */
    @SerialName("tos_accepted_at") @Json(name = "tos_accepted_at") val tosAcceptedAt: String? = null,
    /** One of `"morning"` / `"afternoon"` / `"evening"` — maps to a fixed UTC
     *  hour (not the user's real timezone) for the "daily stack ready" push
     *  notification. Defaults to `"morning"` server-side. See
     *  `docs/be/daily_penpal_stack.md` §3. */
    @SerialName("notify_hour_pref") @Json(name = "notify_hour_pref") val notifyHourPref: String? = null,
    /** Signed minutes east of UTC (e.g. WIB/+07:00 -> 420). See `docs/be/daily_penpal_stack.md` §3. */
    @SerialName("utc_offset_minutes") @Json(name = "utc_offset_minutes") val utcOffsetMinutes: Int? = null,
)

/**
 * Public user profile returned by `GET /api/v1/users/:id`.
 *
 * Intentionally excludes private fields (gender, birthday, fcm_token) and
 * internal fields (tos_accepted_at, moderation status) — only exposes what
 * other users are permitted to see.
 *
 * - [isOnline]     — server-computed boolean (threshold: last_active_at < 5 min ago),
 *                    same definition used in `GET /penpals/feed`.
 * - [lastActiveAt] — raw RFC 3339 timestamp; the client is responsible for rendering
 *                    it as relative time ("3 min ago"), same contract as `GET /auth/me`.
 * - 404 is returned for both "user not found" and "block relationship exists",
 *   so callers cannot distinguish between the two — by design.
 *
 * No longer carries `is_premium` — the backend dropped the subscription
 * concept in favor of the Energy currency (see [UserDto.energy]); the
 * "Verified" badge that used to read it is gone from
 * `PublicProfileBottomSheet` too, not just defaulted client-side.
 */
@JsonClass(generateAdapter = true)
data class PublicProfileDto(
    val id: String,
    val nickname: String,
    val continent: String,
    @Json(name = "photo_url")      val photoUrl: String?,
    val bio: String?,
    val interests: List<String>,
    @Json(name = "languages")      val languages: List<LanguageProficiencyDto>? = null,
    @Json(name = "is_online")      val isOnline: Boolean,
    @Json(name = "last_active_at") val lastActiveAt: String?,
)

@Serializable
@JsonClass(generateAdapter = true)
data class InterestDto(
    val id: String,
    val name: String,
    val emoji: String,
)

@Serializable
@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val nickname: String,
    val continent: String,
    @SerialName("utc_offset_minutes") @Json(name = "utc_offset_minutes") val utcOffsetMinutes: Int? = null,
)

@Serializable
@JsonClass(generateAdapter = true)
data class PatchMeRequest(
    val nickname: String? = null,
    val continent: String? = null,
    @SerialName("fcm_token") @Json(name = "fcm_token") val fcmToken: String? = null,
    val gender: String? = null,
    val birthday: String? = null,
    @SerialName("photo_url") @Json(name = "photo_url") val photoUrl: String? = null,
    val bio: String? = null,
    /** ISO 8601 timestamp set when the user agrees to TOS — null means not yet accepted.
     *  Backend gate: LetterService.Send and penpal match service return 403 when null. */
    @SerialName("tos_accepted_at") @Json(name = "tos_accepted_at") val tosAcceptedAt: String? = null,
    /** One of `"morning"` / `"afternoon"` / `"evening"` — see [UserDto.notifyHourPref]. */
    @SerialName("notify_hour_pref") @Json(name = "notify_hour_pref") val notifyHourPref: String? = null,
    /** Signed minutes east of UTC (e.g. WIB/+07:00 -> 420). See `docs/be/daily_penpal_stack.md` §3. */
    @SerialName("utc_offset_minutes") @Json(name = "utc_offset_minutes") val utcOffsetMinutes: Int? = null,
)

@Serializable
@JsonClass(generateAdapter = true)
data class InterestsRequest(
    @SerialName("interest_ids") @Json(name = "interest_ids") val interestIds: List<String>,
)

/** `code` is free-form (not a curated catalog — see `docs/be/profile_api.md`
 * `PUT /auth/me/languages`), `level` is `1`–`5` (matches the level-dots UI). */
@Serializable
@JsonClass(generateAdapter = true)
data class LanguageProficiencyDto(
    val code: String,
    val level: Int,
)

@Serializable
@JsonClass(generateAdapter = true)
data class LanguagesRequest(
    val languages: List<LanguageProficiencyDto>,
)
