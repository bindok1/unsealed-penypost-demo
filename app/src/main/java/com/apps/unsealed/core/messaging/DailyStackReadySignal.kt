package com.apps.unsealed.core.messaging

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fan-out signal for the `daily_stack_ready` FCM event ([FcmEventDailyStackReady])
 * — bridges [UnsealedMessagingService] (a `Service`, not a ViewModel/Composable)
 * to `PenPalsViewModel` so a stack that finishes generating while the app is
 * already foregrounded refreshes the in-memory feed immediately, instead of
 * only picking up the new stack on the next app open or day-rollover check.
 * See `docs/be/daily_penpal_stack.md` §5.
 */
@Singleton
class DailyStackReadySignal @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notifyReady() {
        _events.tryEmit(Unit)
    }
}
