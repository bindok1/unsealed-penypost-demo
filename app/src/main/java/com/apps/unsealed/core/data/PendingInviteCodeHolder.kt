package com.apps.unsealed.core.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory handoff for an invite code captured from an incoming App Link
 * (`https://peny-dashboard-xi.vercel.app/invite/{code}`, see
 * `docs/be_updet/invite_friend_api.md` §5.1.B). [MainActivity] writes the code here on a
 * deep-link launch; [com.apps.unsealed.ui.screens.profile.viewmodel.InviteViewModel] consumes
 * it once `ProfileInviteFriendsScreen` loads, to auto-redeem instead of requiring the user to
 * retype the code they just tapped a link for. Same single-instance cross-screen handoff shape
 * as [ComposeDraftHolder].
 */
@Singleton
class PendingInviteCodeHolder @Inject constructor() {
    private var code: String? = null

    fun set(code: String) {
        this.code = code
    }

    /** Returns the pending code (if any) and clears it — a redeem attempt is only ever fired once. */
    fun consume(): String? {
        val value = code
        code = null
        return value
    }
}
