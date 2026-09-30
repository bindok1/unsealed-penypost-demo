package com.apps.unsealed.ui.screens.tracking.state

import android.content.Context
import org.json.JSONObject

/**
 * Spatial lookup for world countries using point-in-polygon ray casting over
 * `assets/world_countries.geojson`.
 *
 * This runs 100% in pure Kotlin/JVM memory and never calls GPU/MapLibre JNI
 * renderer queries, completely preventing SIGSEGV crashes while accurately
 * identifying country names and land vs ocean transitions.
 */
data class CountryBoundingPolygon(
    val name: String,
    val minLng: Double,
    val minLat: Double,
    val maxLng: Double,
    val maxLat: Double,
    val rings: List<List<DoubleArray>>,
)

object WorldCountryLookup {
    @Volatile
    private var countries: List<CountryBoundingPolygon>? = null

    fun initialize(context: Context) {
        if (countries != null) return
        synchronized(this) {
            if (countries != null) return
            countries = loadCountries(context)
        }
    }

    /**
     * Finds country name (e.g. "Indonesia", "Japan") at coordinate ([lat], [lng]),
     * or returns `null` if the coordinate is in the ocean / international waters.
     */
    fun findCountry(lat: Double, lng: Double, context: Context? = null): String? {
        if (countries == null && context != null) {
            initialize(context)
        }
        val list = countries ?: return null

        for (country in list) {
            // Fast Bounding Box Pre-Check
            if (lng < country.minLng || lng > country.maxLng || lat < country.minLat || lat > country.maxLat) {
                continue
            }
            // Point-in-polygon Ray Casting
            for (ring in country.rings) {
                if (isPointInPolygon(lng, lat, ring)) {
                    return country.name
                }
            }
        }
        return null
    }

    private fun isPointInPolygon(x: Double, y: Double, ring: List<DoubleArray>): Boolean {
        var inside = false
        val n = ring.size
        if (n < 3) return false
        var p1 = ring[0]
        for (i in 1..n) {
            val p2 = ring[i % n]
            val p1x = p1[0]
            val p1y = p1[1]
            val p2x = p2[0]
            val p2y = p2[1]

            if ((p1y <= y && p2y > y) || (p2y <= y && p1y > y)) {
                val xInters = p1x + (y - p1y) / (p2y - p1y) * (p2x - p1x)
                if (xInters > x) {
                    inside = !inside
                }
            }
            p1 = p2
        }
        return inside
    }

    private fun loadCountries(context: Context): List<CountryBoundingPolygon> {
        val result = mutableListOf<CountryBoundingPolygon>()
        try {
            val jsonStr = context.assets.open("world_countries.geojson").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val features = root.getJSONArray("features")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.optJSONObject("properties") ?: continue
                val name = properties.optString("ADMIN", properties.optString("NAME", ""))
                if (name.isBlank()) continue

                val geometry = feature.optJSONObject("geometry") ?: continue
                val type = geometry.optString("type")
                val coordinates = geometry.optJSONArray("coordinates") ?: continue

                val rings = mutableListOf<List<DoubleArray>>()
                var minLng = Double.MAX_VALUE
                var minLat = Double.MAX_VALUE
                var maxLng = -Double.MAX_VALUE
                var maxLat = -Double.MAX_VALUE

                if (type == "Polygon") {
                    for (r in 0 until coordinates.length()) {
                        val ringJson = coordinates.getJSONArray(r)
                        val ring = parseRing(ringJson)
                        if (ring.isNotEmpty()) {
                            rings.add(ring)
                            for (pt in ring) {
                                if (pt[0] < minLng) minLng = pt[0]
                                if (pt[0] > maxLng) maxLng = pt[0]
                                if (pt[1] < minLat) minLat = pt[1]
                                if (pt[1] > maxLat) maxLat = pt[1]
                            }
                        }
                    }
                } else if (type == "MultiPolygon") {
                    for (p in 0 until coordinates.length()) {
                        val polyJson = coordinates.getJSONArray(p)
                        for (r in 0 until polyJson.length()) {
                            val ringJson = polyJson.getJSONArray(r)
                            val ring = parseRing(ringJson)
                            if (ring.isNotEmpty()) {
                                rings.add(ring)
                                for (pt in ring) {
                                    if (pt[0] < minLng) minLng = pt[0]
                                    if (pt[0] > maxLng) maxLng = pt[0]
                                    if (pt[1] < minLat) minLat = pt[1]
                                    if (pt[1] > maxLat) maxLat = pt[1]
                                }
                            }
                        }
                    }
                }

                if (rings.isNotEmpty()) {
                    result.add(
                        CountryBoundingPolygon(
                            name = name,
                            minLng = minLng,
                            minLat = minLat,
                            maxLng = maxLng,
                            maxLat = maxLat,
                            rings = rings,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun parseRing(ringJson: org.json.JSONArray): List<DoubleArray> {
        val ring = ArrayList<DoubleArray>(ringJson.length())
        for (i in 0 until ringJson.length()) {
            val pt = ringJson.getJSONArray(i)
            ring.add(doubleArrayOf(pt.getDouble(0), pt.getDouble(1)))
        }
        return ring
    }
}
