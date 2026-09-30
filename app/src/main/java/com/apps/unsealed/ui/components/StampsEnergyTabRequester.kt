package com.apps.unsealed.ui.components

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.apps.unsealed.core.data.StampsEnergyTabRequestHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Thin vehicle for pulling the [StampsEnergyTabRequestHolder] singleton into
 * a [Composable] scope via [hiltViewModel] — see [rememberStampsEnergyTabRequester].
 * Same shape as `WriteExitViewModel`. */
@HiltViewModel
internal class StampsEnergyTabRequestViewModel @Inject constructor(
    val holder: StampsEnergyTabRequestHolder,
) : ViewModel()

/**
 * Call [StampsEnergyTabRequestHolder.request] on this before navigating to
 * [com.apps.unsealed.navigation.Destinations.Stamps] from a "buy more Energy"
 * CTA anywhere else in the app, so `StampsViewModel` opens straight on the
 * Energi tab instead of the default Hadiah tab.
 */
@Composable
fun rememberStampsEnergyTabRequester(): StampsEnergyTabRequestHolder =
    hiltViewModel<StampsEnergyTabRequestViewModel>().holder
