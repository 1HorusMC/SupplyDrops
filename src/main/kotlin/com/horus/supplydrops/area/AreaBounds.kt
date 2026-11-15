package com.horus.supplydrops.area

import kotlin.random.Random

/**
 * Rectangular 2D coordinate bounds for supply drop spawning.
 */
data class AreaBounds(
    val minX: Int,
    val maxX: Int,
    val minZ: Int,
    val maxZ: Int
) {
    val normalizedMinX: Int get() = minOf(minX, maxX)
    val normalizedMaxX: Int get() = maxOf(minX, maxX)
    val normalizedMinZ: Int get() = minOf(minZ, maxZ)
    val normalizedMaxZ: Int get() = maxOf(minZ, maxZ)

    /**
     * Picks a random `(x, z)` pair within the normalized bounds (inclusive).
     */
    fun randomXZ(random: Random = Random.Default): Pair<Int, Int> {
        val x = random.nextInt(normalizedMinX, normalizedMaxX + 1)
        val z = random.nextInt(normalizedMinZ, normalizedMaxZ + 1)
        return x to z
    }
}
