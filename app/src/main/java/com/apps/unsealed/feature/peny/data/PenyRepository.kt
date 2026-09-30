package com.apps.unsealed.feature.peny.data

import android.content.Context
import com.apps.unsealed.R
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.ui.screens.compose.state.PenyRole
import com.apps.unsealed.ui.screens.compose.state.PenyTurn
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import retrofit2.Retrofit

/** Result of [PenyRepository.reply] — a dedicated type (rather than reusing
 * [AuthResult]) because the caller (`ComposeViewModel`) must react
 * differently to "out of Energy" than to any other failure: a warm
 * in-character message instead of a generic error, and the just-typed turn
 * is *not* appended to `penyHistory` (docs/be_updet/peny_mode_mobile_integration.md
 * §3). [AuthResult.Error]'s plain `message: String` can't carry that
 * distinction without string-matching the localized copy. */
sealed class PenyReplyResult {
    data class Success(val data: PenyReplyResponseData) : PenyReplyResult()
    data object InsufficientEnergy : PenyReplyResult()
    data class Error(val message: String) : PenyReplyResult()
}

/** Mode Peny's only network touchpoint (peny_mode_mobile_integration.md §1) —
 * the absorb animation, glow states, and 3-layer threshold logic living in
 * `ComposeViewModel` never call anything here except [reply], and only once
 * a turn clears the client-side threshold. [getStatus] is the one other
 * call, made once per compose session at gating time. */
@Singleton
class PenyRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: PenyApi by lazy { retrofit.create(PenyApi::class.java) }

    /** `is_enabled` toggle from webadmin (peny_config) — used to be combined
     * client-side with `isPremium` into a single availability gate, but the
     * backend dropped subscription gating for Mode Peny entirely, so
     * `ComposeViewModel.init` now gates on this alone (integration doc §2). */
    suspend fun getStatus(): AuthResult<PenyStatusDto> =
        runCatching { api.getStatus().unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
            },
        )

    /** [history] must NOT include the turn being submitted as [message] — the
     * caller appends both the user's message and the reply to its own
     * history afterward, once this call succeeds (integration doc §2 steps
     * 4-5). */
    suspend fun reply(history: List<PenyTurn>, message: String): PenyReplyResult =
        runCatching {
            api.reply(
                PenyReplyRequest(
                    history = history.map { PenyTurnDto(role = it.role.wireValue, text = it.text) },
                    message = message,
                ),
            ).unwrap()
        }.fold(
            onSuccess = { PenyReplyResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                if (it.isInsufficientEnergyError()) {
                    PenyReplyResult.InsufficientEnergy
                } else {
                    PenyReplyResult.Error(it.toUserFacingMessage(context, R.string.error_generic))
                }
            },
        )

    /** Same detection approach as `PenpalsRepository.isInsufficientEnergyError`
     * — the `INSUFFICIENT_ENERGY` case comes back as `{"error": {"code": ...}}`,
     * which doesn't fit the rest of the app's `error: String` envelope
     * ([com.apps.unsealed.core.network.ApiResponse]), so it's detected from
     * the raw error body rather than via the normal [unwrap] path. */
    private fun Throwable.isInsufficientEnergyError(): Boolean {
        if (this !is HttpException) return false
        val rawBody = runCatching { response()?.errorBody()?.string() }.getOrNull() ?: return false
        return rawBody.contains("INSUFFICIENT_ENERGY")
    }
}

private val PenyRole.wireValue: String
    get() = when (this) {
        PenyRole.USER -> "user"
        PenyRole.PENY -> "peny"
    }
