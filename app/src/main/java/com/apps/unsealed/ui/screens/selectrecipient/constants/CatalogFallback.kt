package com.apps.unsealed.ui.screens.selectrecipient.constants

import android.content.Context
import com.apps.unsealed.feature.catalog.data.CatalogItemDto

/** Bundled-asset stand-ins for [CatalogItemDto], used until the backend's
 * `GET /envelopes`/`GET /stamps` catalogs are reachable (offline, network
 * error, or — for envelopes — before that endpoint exists at all, see
 * `docs/backend-roadmap.md`). ids match the legacy [EnvelopeDesign]/
 * [StampDesign] enum names so a letter sent while offline still resolves
 * server-side once the CMS catalog seeds those same 5/6 ids. */
fun localEnvelopeCatalog(context: Context): List<CatalogItemDto> =
    EnvelopeDesign.entries.map { design ->
        CatalogItemDto(
            id = design.name,
            name = "Envelope " + design.name.removePrefix("ENVELOPE_"),
            imageUrl = "android.resource://${context.packageName}/${design.drawableRes}",
            isPremium = false,
        )
    }

/** Stamps have no bundled image asset at all (see [StampDesign]'s doc
 * comment) — [CatalogItemDto.imageUrl] is left blank here, and
 * [StampPickerOverlay] falls back to [StampSwatch]'s procedural drawing
 * whenever it's blank. */
fun localStampCatalog(context: Context): List<CatalogItemDto> =
    StampDesign.entries.map { design ->
        CatalogItemDto(
            id = design.name,
            name = context.getString(design.labelRes),
            imageUrl = "",
            isPremium = false,
        )
    }

fun localStickerCatalog(context: Context): List<CatalogItemDto> = listOf(
    CatalogItemDto(
        id = "STICKER_PENY_SWIM",
        name = "Peny Swim",
        imageUrl = "android.resource://${context.packageName}/${com.apps.unsealed.R.drawable.peny_swim}",
        isPremium = false,
    ),
    CatalogItemDto(
        id = "STICKER_PENY_WALK",
        name = "Peny Walk",
        imageUrl = "android.resource://${context.packageName}/${com.apps.unsealed.R.drawable.peny_walk}",
        isPremium = false,
    ),
    CatalogItemDto(
        id = "STICKER_FISH",
        name = "Little Fish",
        imageUrl = "android.resource://${context.packageName}/${com.apps.unsealed.R.drawable.ic_fish}",
        isPremium = false,
    ),
)

/** Resolves [id] (a letter's `envelope`/`stamp` field) against this catalog —
 * used by screens that *render* an already-sent letter (Inbox, Open Letter,
 * PenPals feed), not just the compose-time pickers. Falls back to this
 * list's first entry rather than silently mismatching to a hardcoded design
 * — callers should pass a catalog that's always non-empty (real network
 * result, or [localEnvelopeCatalog]/[localStampCatalog] while unavailable)
 * so an id from a CMS-added design that hasn't loaded yet at least degrades
 * to *a* placeholder instead of crashing on `.first()` of an empty list. */
fun List<CatalogItemDto>.resolveImageUrl(id: String): String =
    firstOrNull { it.id == id }?.imageUrl
        ?: if (id.startsWith("http://") || id.startsWith("https://")) id
        else firstOrNull()?.imageUrl.orEmpty()

/** Resolves [id] against this catalog, returning the full [CatalogItemDto]
 * instead of just its image URL — used by callers that hand the result
 * straight to [com.apps.unsealed.ui.screens.selectrecipient.widgets.StampImage]
 * (Mailbox, Mailbox Thread). Unlike [resolveImageUrl], a miss doesn't borrow
 * this list's first entry — it synthesizes an item with [id] and a blank
 * [CatalogItemDto.imageUrl], so [StampImage]'s own blank-url fallback draws
 * [StampSwatch] keyed off the real [id] (still correct for legacy ids like
 * `"OCEAN"`) instead of showing an unrelated design's art. */
fun List<CatalogItemDto>.resolveItem(id: String): CatalogItemDto =
    firstOrNull { it.id == id } ?: CatalogItemDto(
        id = id,
        name = if (id.startsWith("http://") || id.startsWith("https://")) "Creator Stamp" else id,
        imageUrl = if (id.startsWith("http://") || id.startsWith("https://")) id else "",
        isPremium = id.startsWith("http://") || id.startsWith("https://"),
    )
