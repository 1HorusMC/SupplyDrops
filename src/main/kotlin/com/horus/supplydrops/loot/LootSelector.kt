package com.horus.supplydrops.loot

import kotlin.random.Random

/**
 * Pure descriptor for a loot pool entry used by [LootSelector].
 */
data class WeightedLootCandidate<T>(
    val id: String,
    val payload: T,
    val weight: Double,
    val chance: Double = 100.0,
    val guaranteed: Boolean = false
)

/**
 * Selects loot entries using both independent probability (`chance` 0..100) and weighted random selection (`weight`).
 */
object LootSelector {

    /**
     * Selects between [minItems] and [maxItems] items from [entries].
     *
     * Guaranteed items that pass their [WeightedLootCandidate.chance] roll are included first,
     * and the remaining slots up to the rolled target count are filled via weighted random selection
     * from eligible candidates (entries whose `chance` roll succeeds and `weight > 0`).
     */
    fun <T> select(
        entries: List<WeightedLootCandidate<T>>,
        minItems: Int,
        maxItems: Int,
        allowDuplicates: Boolean,
        random: Random = Random.Default
    ): List<T> {
        if (entries.isEmpty() || maxItems <= 0) return emptyList()

        val safeMin = minItems.coerceAtLeast(0)
        val safeMax = maxItems.coerceAtLeast(safeMin)
        val targetCount = if (safeMin == safeMax) safeMin else random.nextInt(safeMin, safeMax + 1)
        if (targetCount == 0) return emptyList()

        val selected = mutableListOf<T>()
        val selectedIds = mutableSetOf<String>()

        // 1. Process guaranteed items that pass their chance roll
        for (entry in entries) {
            if (entry.guaranteed && rollChance(entry.chance, random)) {
                selected.add(entry.payload)
                selectedIds.add(entry.id)
                if (selected.size >= targetCount) {
                    return selected
                }
            }
        }

        // 2. Filter non-guaranteed (or all, if duplicates allowed) eligible candidates by chance roll
        val pool = entries.filter { entry ->
            entry.weight > 0.0 &&
                (allowDuplicates || entry.id !in selectedIds) &&
                (!entry.guaranteed || allowDuplicates) &&
                rollChance(entry.chance, random)
        }.toMutableList()

        // 3. Weighted random pick for remaining slots
        while (selected.size < targetCount && pool.isNotEmpty()) {
            val pickedIndex = pickWeightedIndex(pool, random) ?: break
            val picked = pool[pickedIndex]
            selected.add(picked.payload)
            if (!allowDuplicates) {
                pool.removeAt(pickedIndex)
            }
        }

        return selected
    }

    internal fun rollChance(chance: Double, random: Random): Boolean {
        if (chance >= 100.0) return true
        if (chance <= 0.0) return false
        return random.nextDouble(0.0, 100.0) <= chance
    }

    internal fun <T> pickWeightedIndex(
        pool: List<WeightedLootCandidate<T>>,
        random: Random
    ): Int? {
        val totalWeight = pool.sumOf { it.weight.coerceAtLeast(0.0) }
        if (totalWeight <= 0.0) return null

        var roll = random.nextDouble(0.0, totalWeight)
        for (i in pool.indices) {
            roll -= pool[i].weight.coerceAtLeast(0.0)
            if (roll <= 0.0) {
                return i
            }
        }
        return pool.lastIndex
    }
}
