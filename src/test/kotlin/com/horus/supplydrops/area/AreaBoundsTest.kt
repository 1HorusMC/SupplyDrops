package com.horus.supplydrops.area

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class AreaBoundsTest {

    @Test
    fun `randomXZ stays within normalized bounds even if min and max are swapped`() {
        val bounds = AreaBounds(minX = 100, maxX = -50, minZ = 20, maxZ = -10)
        val random = Random(99)

        repeat(100) {
            val (x, z) = bounds.randomXZ(random)
            assertTrue(x in -50..100, "x=$x out of bounds")
            assertTrue(z in -10..20, "z=$z out of bounds")
        }
    }
}
