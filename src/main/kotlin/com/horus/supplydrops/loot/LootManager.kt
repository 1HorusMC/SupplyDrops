package com.horus.supplydrops.loot

import com.horus.supplydrops.config.LootSettings
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.Locale
import kotlin.random.Random

/**
 * Represents a parsed loot definition from `loot.yml`.
 */
data class LootEntry(
    val id: String,
    val material: Material,
    val minAmount: Int,
    val maxAmount: Int,
    val weight: Double,
    val chance: Double,
    val guaranteed: Boolean,
    val name: String?,
    val lore: List<String>,
    val customModelData: Int?,
    val unbreakable: Boolean,
    val glow: Boolean,
    val enchantments: Map<Enchantment, Int>
) {
    fun buildItemStack(miniMessage: MiniMessage, random: Random = Random.Default): ItemStack {
        val amount = if (minAmount >= maxAmount) {
            minAmount.coerceAtLeast(1)
        } else {
            random.nextInt(minAmount.coerceAtLeast(1), maxAmount + 1)
        }

        val item = ItemStack(material, amount)
        val meta = item.itemMeta ?: return item

        if (!name.isNullOrBlank()) {
            val component = miniMessage.deserialize(name)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            meta.displayName(component)
        }

        if (lore.isNotEmpty()) {
            val loreComponents = lore.map { line ->
                miniMessage.deserialize(line)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            }
            meta.lore(loreComponents)
        }

        if (customModelData != null) {
            meta.setCustomModelData(customModelData)
        }

        if (unbreakable) {
            meta.isUnbreakable = true
        }

        if (glow) {
            meta.setEnchantmentGlintOverride(true)
        }

        for ((enchantment, level) in enchantments) {
            meta.addEnchant(enchantment, level, true)
        }

        item.itemMeta = meta
        return item
    }
}

/**
 * Loads `loot.yml` and populates supply drop inventories.
 */
class LootManager(
    private val plugin: JavaPlugin,
    private val miniMessage: MiniMessage
) {
    private var entries: List<LootEntry> = emptyList()

    fun load() {
        val lootFile = File(plugin.dataFolder, "loot.yml")
        if (!lootFile.exists()) {
            plugin.saveResource("loot.yml", false)
        }

        val yaml = YamlConfiguration.loadConfiguration(lootFile)
        val itemsSection = yaml.getConfigurationSection("items")
        if (itemsSection == null) {
            plugin.logger.warning("No 'items' section found in loot.yml.")
            entries = emptyList()
            return
        }

        val loaded = mutableListOf<LootEntry>()
        for (key in itemsSection.getKeys(false)) {
            val section = itemsSection.getConfigurationSection(key) ?: continue
            parseEntry(key, section)?.let(loaded::add)
        }

        entries = loaded
        plugin.logger.info("Loaded ${entries.size} loot entries from loot.yml.")
    }

    fun populateInventory(inventory: Inventory, settings: LootSettings, random: Random = Random.Default) {
        if (entries.isEmpty()) return

        val candidates = entries.map { entry ->
            WeightedLootCandidate(
                id = entry.id,
                payload = entry,
                weight = entry.weight,
                chance = entry.chance,
                guaranteed = entry.guaranteed
            )
        }

        val selected = LootSelector.select(
            entries = candidates,
            minItems = settings.minItems,
            maxItems = settings.maxItems,
            allowDuplicates = settings.allowDuplicates,
            random = random
        )

        val availableSlots = (0 until inventory.size).shuffled(random)
        for ((index, entry) in selected.withIndex()) {
            if (index >= availableSlots.size) break
            inventory.setItem(availableSlots[index], entry.buildItemStack(miniMessage, random))
        }
    }

    private fun parseEntry(id: String, section: ConfigurationSection): LootEntry? {
        val materialName = section.getString("material")
        val material = materialName?.let { Material.matchMaterial(it.trim().uppercase(Locale.ROOT)) }
        if (material == null || material.isAir) {
            plugin.logger.warning("Invalid material '$materialName' for loot entry '$id'. Skipping.")
            return null
        }

        val fixedAmount = if (section.contains("amount")) section.getInt("amount", 1) else null
        val minAmount = (fixedAmount ?: section.getInt("min-amount", 1)).coerceAtLeast(1)
        val maxAmount = (fixedAmount ?: section.getInt("max-amount", minAmount)).coerceAtLeast(minAmount)

        val weight = section.getDouble("weight", 10.0).coerceAtLeast(0.0)
        val chance = section.getDouble("chance", 100.0).coerceIn(0.0, 100.0)
        val guaranteed = section.getBoolean("guaranteed", false)
        val name = section.getString("name")
        val lore = section.getStringList("lore")
        val customModelData = if (section.contains("custom-model-data")) {
            section.getInt("custom-model-data")
        } else {
            null
        }
        val unbreakable = section.getBoolean("unbreakable", false)
        val glow = section.getBoolean("glow", false)

        val enchantments = mutableMapOf<Enchantment, Int>()
        val enchSection = section.getConfigurationSection("enchantments")
        if (enchSection != null) {
            for (enchKey in enchSection.getKeys(false)) {
                val enchantment = resolveEnchantment(enchKey)
                if (enchantment == null) {
                    plugin.logger.warning("Unknown enchantment '$enchKey' in loot entry '$id'.")
                    continue
                }
                val level = enchSection.getInt(enchKey, 1).coerceAtLeast(1)
                enchantments[enchantment] = level
            }
        }

        return LootEntry(
            id = id,
            material = material,
            minAmount = minAmount,
            maxAmount = maxAmount,
            weight = weight,
            chance = chance,
            guaranteed = guaranteed,
            name = name,
            lore = lore,
            customModelData = customModelData,
            unbreakable = unbreakable,
            glow = glow,
            enchantments = enchantments
        )
    }

    private fun resolveEnchantment(rawKey: String): Enchantment? {
        val normalized = rawKey.trim().lowercase(Locale.ROOT)
        val key = if (normalized.contains(':')) {
            NamespacedKey.fromString(normalized)
        } else {
            NamespacedKey.minecraft(normalized)
        } ?: return null

        return Registry.ENCHANTMENT.get(key)
    }
}
