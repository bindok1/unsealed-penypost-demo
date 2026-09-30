package com.apps.unsealed.feature.reports.data

import android.content.Context
import com.apps.unsealed.R
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Retrofit

@Singleton
class ReportsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: ReportsApi by lazy { retrofit.create(ReportsApi::class.java) }

    /** Reports a specific letter (`target_type = "letter"`) — see [ReportsApi.createReport]. */
    suspend fun reportLetter(letterId: String, reason: String, note: String?): AuthResult<ReportResponseData> =
        runCatching {
            api.createReport(
                ReportRequest(
                    targetType = "letter",
                    targetId = letterId,
                    reason = reason,
                    letterId = letterId,
                    note = note?.takeIf { it.isNotBlank() },
                ),
            ).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_report_letter), it)
            },
        )

    /** Reports a user's profile (`target_type = "user"`) — see [ReportsApi.createReport]. */
    suspend fun reportUser(userId: String, reason: String, note: String?): AuthResult<ReportResponseData> =
        runCatching {
            api.createReport(
                ReportRequest(
                    targetType = "user",
                    targetId = userId,
                    reason = reason,
                    letterId = null,
                    note = note?.takeIf { it.isNotBlank() },
                ),
            ).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_report_user), it)
            },
        )
}
