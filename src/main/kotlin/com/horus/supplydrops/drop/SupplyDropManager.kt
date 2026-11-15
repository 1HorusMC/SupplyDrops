package com.horus.supplydrops.drop

import com.horus.supplydrops.area.LocationFinder
import com.horus.supplydrops.config.PluginConfig
import com.horus.supplydrops.discord.DiscordWebhookClient
import com.horus.supplydrops.loot.LootManager
import com.horus.supplydrops.util.TimeFormatter
import com.horus.supplydrops.visual.HologramDisplay
import com.horus.supplydrops.visual.VisualEffectsTask
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Represents a currently active supply drop in the world.
 */
class ActiveSupplyDrop(
    val id: UUID,
    val location: Location,
    var remainingSeconds: Int,
    val hologram: HologramDisplay,
    val visuals: VisualEffectsTask
) {
    var countdownTask: BukkitTask? = null

    fun isAt(blockLocation: Location): Boolean {
        return location.world?.uid == blockLocation.world?.uid &&
            location.blockX == blockLocation.blockX &&
            location.blockY == blockLocation.blockY &&
            location.blockZ == blockLocation.blockZ
    }
}

/**
 * Coordinates spawning, tracking, countdowns, visuals, broadcasts, and cleanup of supply drops.
 */
class SupplyDropManager(
    private val plugin: JavaPlugin,
    private val miniMessage: MiniMessage,
    private val lootManager: LootManager,
    private val locationFinder: LocationFinder,
    private val discordClient: DiscordWebhookClient,
    private var config: PluginConfig
) {
    private val activeDrops = mutableListOf<ActiveSupplyDrop>()
    private var autoSpawnTask: BukkitTask? = null
    val orphanStore = OrphanStore(plugin)

    fun updateConfig(newConfig: PluginConfig) {
        this.config = newConfig
        restartAutoScheduler()
    }

    fun startAutoScheduler() {
        stopAutoScheduler()
        if (config.intervalMinutes <= 0) return

        val intervalTicks = config.intervalMinutes * 60L * 20L
        autoSpawnTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable { spawnSupplyDrop() },
            intervalTicks,
            intervalTicks
        )
    }

    fun stopAutoScheduler() {
        autoSpawnTask?.cancel()
        autoSpawnTask = null
    }

    fun spawnSupplyDrop(): ActiveSupplyDrop? {
        if (!config.allowMultiple && activeDrops.isNotEmpty()) {
            return null
        }

        val spawnLoc = locationFinder.findSpawnLocation(config.area) ?: return null
        val world = spawnLoc.world ?: return null

        val block = spawnLoc.block
        block.type = config.containerType.material

        val container = block.state as? Container
        if (container != null) {
            lootManager.populateInventory(container.inventory, config.loot)
        } else {
            plugin.logger.warning("Placed block at ${spawnLoc.blockX},${spawnLoc.blockY},${spawnLoc.blockZ} is not a Container.")
        }

        if (config.visuals.lightningOnSpawn) {
            world.strikeLightningEffect(spawnLoc.clone().add(0.5, 0.0, 0.5))
        }

        val hologram = HologramDisplay(plugin, miniMessage, config.visuals.hologram, spawnLoc)
        hologram.spawn(config.durationSeconds)

        val visuals = VisualEffectsTask(plugin, config.visuals, spawnLoc)
        visuals.start()

        val drop = ActiveSupplyDrop(
            id = UUID.randomUUID(),
            location = spawnLoc,
            remainingSeconds = config.durationSeconds,
            hologram = hologram,
            visuals = visuals
        )

        drop.countdownTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable {
                drop.remainingSeconds -= 1
                if (drop.remainingSeconds <= 0) {
                    despawnDrop(drop, expired = true)
                } else {
                    drop.hologram.update(drop.remainingSeconds)
                }
            },
            20L,
            20L
        )

        activeDrops.add(drop)
        orphanStore.add(world.name, spawnLoc.blockX, spawnLoc.blockY, spawnLoc.blockZ)

        val formattedTime = TimeFormatter.formatDuration(config.durationSeconds)
        broadcastSpawn(world.name, spawnLoc.blockX, spawnLoc.blockY, spawnLoc.blockZ, formattedTime)
        discordClient.sendSpawnNotification(
            settings = config.discord,
            worldName = world.name,
            x = spawnLoc.blockX,
            y = spawnLoc.blockY,
            z = spawnLoc.blockZ,
            formattedTime = formattedTime
        )

        return drop
    }

    fun despawnDrop(drop: ActiveSupplyDrop, expired: Boolean = false, claimedBy: String? = null) {
        if (!activeDrops.remove(drop)) return
        orphanStore.remove(
            drop.location.world?.name ?: "",
            drop.location.blockX, drop.location.blockY, drop.location.blockZ
        )

        drop.countdownTask?.cancel()
        drop.countdownTask = null
        drop.visuals.stop()
        drop.hologram.remove()

        val block = drop.location.block
        val state = block.state
        if (state is Container) {
            state.inventory.clear()
        }
        if (block.type == Material.CHEST || block.type == Material.BARREL) {
            block.type = Material.AIR
        }

        if (expired && config.messages.expiredBroadcast.isNotBlank()) {
            broadcastMessage(
                applyPlaceholders(
                    config.messages.expiredBroadcast,
                    drop.location.world?.name ?: "world",
                    drop.location.blockX,
                    drop.location.blockY,
                    drop.location.blockZ,
                    "00:00",
                    null
                )
            )
        } else if (claimedBy != null && config.messages.claimedBroadcast.isNotBlank()) {
            broadcastMessage(
                applyPlaceholders(
                    config.messages.claimedBroadcast,
                    drop.location.world?.name ?: "world",
                    drop.location.blockX,
                    drop.location.blockY,
                    drop.location.blockZ,
                    TimeFormatter.formatDuration(drop.remainingSeconds),
                    claimedBy
                )
            )
        }
    }

    fun stopAll(): Int {
        val snapshot = activeDrops.toList()
        for (drop in snapshot) {
            despawnDrop(drop, expired = false, claimedBy = null)
        }
        return snapshot.size
    }

    fun findDropAt(location: Location): ActiveSupplyDrop? =
        activeDrops.firstOrNull { it.isAt(location) }

    fun ownsHologram(entityUuid: UUID): Boolean =
        activeDrops.any { it.hologram.ownsEntity(entityUuid) }

    fun getConfig(): PluginConfig = config

    private fun restartAutoScheduler() {
        startAutoScheduler()
    }

    private fun broadcastSpawn(worldName: String, x: Int, y: Int, z: Int, formattedTime: String) {
        for (line in config.messages.spawnBroadcast) {
            broadcastMessage(applyPlaceholders(line, worldName, x, y, z, formattedTime, null))
        }
    }

    private fun broadcastMessage(rawMiniMessage: String) {
        val component = miniMessage.deserialize(rawMiniMessage)
        Bukkit.getServer().sendMessage(component)
    }

    private fun applyPlaceholders(
        template: String,
        worldName: String,
        x: Int,
        y: Int,
        z: Int,
        formattedTime: String,
        playerName: String?
    ): String {
        var result = template
            .replace("{prefix}", config.messages.prefix)
            .replace("{world}", worldName)
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
            .replace("{z}", z.toString())
            .replace("{time}", formattedTime)
        if (playerName != null) {
            result = result.replace("{player}", MiniMessage.miniMessage().escapeTags(playerName))
        }
        return result
    }
}
