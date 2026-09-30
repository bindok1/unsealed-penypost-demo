package com.apps.unsealed.feature.letters.data

import android.content.Context
import com.apps.unsealed.R
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import retrofit2.Retrofit

/** Result of [LettersRepository.unlockLetter] — a dedicated type (rather than
 * reusing [AuthResult]) because the caller (`DeliveryTrackingViewModel`) must
 * react differently to "not enough Energy" than to any other failure: a
 * proactive "want to buy more Energy?" dialog instead of a plain error toast
 * (the "jemput bola" upsell — see `docs/copywriting/cp.md`). Same shape as
 * `PenyRepository.PenyReplyResult`, whose doc comment explains why
 * [AuthResult.Error]'s plain `message: String` can't carry this distinction
 * without string-matching localized copy. */
sealed class UnlockLetterResult {
    data class Success(val data: UnlockLetterResponse) : UnlockLetterResult()
    data class InsufficientEnergy(val required: Int, val available: Int) : UnlockLetterResult()
    data class Error(val message: String) : UnlockLetterResult()
}

@Singleton
class LettersRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
    private val moshi: Moshi,
) {
    private val api: LettersApi by lazy { retrofit.create(LettersApi::class.java) }

    suspend fun sendLetter(request: SendLetterRequest): AuthResult<SendLetterResponse> =
        runCatching { api.sendLetter(request).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_letters_send), it)
            },
        )

    suspend fun getInbox(
        status: String? = null,
        cursor: String? = null,
        limit: Int? = null,
    ): AuthResult<LetterListResponse> =
        runCatching { api.getInbox(status, cursor, limit).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_letters_inbox), it)
            },
        )

    suspend fun getLetter(id: String): AuthResult<LetterDetailDto> =
        runCatching { api.getLetter(id).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_letters_detail), it)
            },
        )

    suspend fun unlockLetter(id: String): UnlockLetterResult =
        runCatching { api.unlockLetter(id).unwrap() }.fold(
            onSuccess = { UnlockLetterResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                val meta = it.unlockInsufficientEnergyMeta()
                if (meta != null) {
                    UnlockLetterResult.InsufficientEnergy(meta.required, meta.available)
                } else {
                    UnlockLetterResult.Error(it.toUserFacingMessage(context, R.string.error_letters_unlock))
                }
            },
        )

    suspend fun getUserShowcase(userId: String, cursor: String? = null): AuthResult<ShowcaseListResponse> =
        runCatching { api.getUserShowcase(userId, cursor).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_letters_showcase), it)
            },
        )

    suspend fun getMyShowcase(
        status: String = "all",
        cursor: String? = null,
    ): AuthResult<ShowcaseListResponse> =
        runCatching { api.getMyShowcase(status, cursor).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_letters_showcase), it)
            },
        )

    suspend fun updateShowcaseStatus(id: String, status: ShowcaseStatus): AuthResult<ShowcaseItemDto> =
        runCatching { api.updateShowcaseStatus(id, ShowcaseStatusRequest(status.apiValue)).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_letters_showcase_update), it)
            },
        )

    /** `POST /letters/{id}/unlock`'s `400 "not enough energy"` case carries a
     * `meta.{required,available}` the generic [toUserFacingMessage] doesn't
     * know about — parsed out so the caller can react to it distinctly
     * (see [UnlockLetterResult.InsufficientEnergy]) instead of just reading
     * the numbers back in a canned error string. */
    private fun Throwable.unlockInsufficientEnergyMeta(): UnlockErrorMeta? {
        if (this !is HttpException || code() != 400) return null
        val rawBody = runCatching { response()?.errorBody()?.string() }.getOrNull() ?: return null
        return runCatching { moshi.adapter(UnlockErrorBody::class.java).fromJson(rawBody) }.getOrNull()?.meta
    }
}
