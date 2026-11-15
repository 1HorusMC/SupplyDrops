package com.horus.supplydrops.visual

import com.horus.supplydrops.config.HologramSettings
import com.horus.supplydrops.util.TimeFormatter
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * Manages a native 1.21+ [TextDisplay] hologram above a supply drop container.
 */
class HologramDisplay(
    private val plugin: JavaPlugin,
    private val miniMessage: MiniMessage,
    private val settings: HologramSettings,
    private val containerLocation: Location
) {
    private var entityUuid: UUID? = null

    fun spawn(remainingSeconds: Int) {
        if (!settings.enabled) return
        val world = containerLocation.world ?: return

        val displayLocation = containerLocation.clone().add(0.5, settings.heightOffset, 0.5)
        val display = world.spawn(displayLocation, TextDisplay::class.java) { entity ->
            entity.billboard = Display.Billboard.CENTER
            entity.alignment = TextDisplay.TextAlignment.CENTER
            entity.isSeeThrough = settings.seeThrough
            entity.isShadowed = settings.shadow
            entity.isPersistent = false
            entity.backgroundColor = settings.backgroundColor ?: Color.fromARGB(0, 0, 0, 0)
            entity.persistentDataContainer.set(hologramKey(plugin), PersistentDataType.BYTE, 1.toByte())
            entity.text(renderText(remainingSeconds))
        }
        entityUuid = display.uniqueId
    }

    fun update(remainingSeconds: Int) {
        val entity = findEntity() ?: return
        entity.text(renderText(remainingSeconds))
    }

    fun remove() {
        findEntity()?.remove()
        entityUuid = null
    }

    fun ownsEntity(uuid: UUID): Boolean = entityUuid == uuid

    private fun findEntity(): TextDisplay? {
        val uuid = entityUuid ?: return null
        val world = containerLocation.world ?: return null
        return world.getEntity(uuid) as? TextDisplay
    }

    private fun renderText(remainingSeconds: Int) = miniMessage.deserialize(
        settings.lines.joinToString("\n") { line ->
            line.replace("{time}", TimeFormatter.formatDuration(remainingSeconds))
        }
    )

    companion object {
        fun hologramKey(plugin: JavaPlugin): NamespacedKey = NamespacedKey(plugin, "supply_drop_hologram")

        fun isTaggedHologram(plugin: JavaPlugin, entity: Entity): Boolean {
            return entity is TextDisplay &&
                entity.persistentDataContainer.has(hologramKey(plugin), PersistentDataType.BYTE)
        }
    }
}
