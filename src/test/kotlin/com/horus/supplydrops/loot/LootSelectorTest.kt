package com.horus.supplydrops.loot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class LootSelectorTest {

    @Test
    fun `selects within min and max bounds without duplicates`() {
        val entries = (1..5).map { i ->
            WeightedLootCandidate(
                id = "item_$i",
                payload = "item_$i",
                weight = i * 10.0,
                chance = 100.0,
                guaranteed = false
            )
        }

        val random = Random(42)
        val picked = LootSelector.select(
            entries = entries,
            minItems = 2,
            maxItems = 4,
            allowDuplicates = false,
            random = random
        )

        assertTrue(picked.size in 2..4)
        assertEquals(picked.toSet().size, picked.size)
    }

    @Test
    fun `includes guaranteed items before weighted pool`() {
        val entries = listOf(
            WeightedLootCandidate("guaranteed", "guaranteed", weight = 1.0, chance = 100.0, guaranteed = true),
            WeightedLootCandidate("normal_1", "normal_1", weight = 10.0, chance = 100.0, guaranteed = false),
            WeightedLootCandidate("normal_2", "normal_2", weight = 10.0, chance = 100.0, guaranteed = false)
        )

        val picked = LootSelector.select(
            entries = entries,
            minItems = 2,
            maxItems = 2,
            allowDuplicates = false,
            random = Random(7)
        )

        assertEquals(2, picked.size)
        assertTrue("guaranteed" in picked)
    }

    @Test
    fun `respects zero chance by excluding item`() {
        val entries = listOf(
            WeightedLootCandidate("never", "never", weight = 100.0, chance = 0.0),
            WeightedLootCandidate("always", "always", weight = 10.0, chance = 100.0)
        )

        val picked = LootSelector.select(
            entries = entries,
            minItems = 1,
            maxItems = 2,
            allowDuplicates = false,
            random = Random(123)
        )

        assertEquals(listOf("always"), picked)
    }

    @Test
    fun `allows duplicates when configured`() {
        val entries = listOf(
            WeightedLootCandidate("only", "only", weight = 10.0, chance = 100.0)
        )

        val picked = LootSelector.select(
            entries = entries,
            minItems = 3,
            maxItems = 3,
            allowDuplicates = true,
            random = Random(1)
        )

        assertEquals(listOf("only", "only", "only"), picked)
    }
}
