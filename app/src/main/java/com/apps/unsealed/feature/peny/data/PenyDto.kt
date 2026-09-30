package com.apps.unsealed.feature.peny.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PenyStatusDto(
    @Json(name = "is_enabled") val isEnabled: Boolean,
)

/** Wire shape for one turn in `history` — see [PenyRepository.reply] for why
 * this stays a separate Moshi-annotated type instead of reusing
 * `ComposeUiState`'s `PenyTurn` directly over the network. */
@JsonClass(generateAdapter = true)
data class PenyTurnDto(
    val role: String,
    val text: String,
)

@JsonClass(generateAdapter = true)
data class PenyReplyRequest(
    val history: List<PenyTurnDto>,
    val message: String,
)

@JsonClass(generateAdapter = true)
data class PenyReplyResponseData(
    val reply: String,
    val source: String,
    @Json(name = "energy_spent") val energySpent: Int,
    @Json(name = "current_energy_balance") val currentEnergyBalance: Int,
)
