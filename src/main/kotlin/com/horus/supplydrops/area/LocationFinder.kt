package com.horus.supplydrops.area

import com.horus.supplydrops.config.AreaMode
import com.horus.supplydrops.config.AreaSettings
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin

/**
 * Finds safe surface spawn locations inside either configured coordinates or a WorldGuard region.
 */
class LocationFinder(private val plugin: JavaPlugin) {

    fun findSpawnLocation(settings: AreaSettings): Location? {
        val world = Bukkit.getWorld(settings.worldName)
        if (world == null) {
            plugin.logger.warning("Target world '${settings.worldName}' is not loaded.")
            return null
        }

        val resolvedRegion = if (settings.mode == AreaMode.WORLDGUARD) {
            val wg = WorldGuardHook.resolveRegion(world, settings.worldGuardRegion)
            if (wg == null) {
                plugin.logger.warning(
                    "WorldGuard region '${settings.worldGuardRegion}' not found or WorldGuard is unavailable."
                )
                return null
            }
            wg
        } else {
            null
        }

        val bounds = resolvedRegion?.bounds ?: settings.bounds

        repeat(settings.maxLocationAttempts) {
            val (x, z) = bounds.randomXZ()
            val candidate = findSurfaceAt(world, x, z, settings) ?: return@repeat
            if (resolvedRegion == null || resolvedRegion.containsBlock(x, candidate.blockY, z)) {
                return candidate
            }
        }

        plugin.logger.warning(
            "Failed to find a safe supply drop location in '${world.name}' after ${settings.maxLocationAttempts} attempts."
        )
        return null
    }

    private fun findSurfaceAt(world: World, x: Int, z: Int, settings: AreaSettings): Location? {
        // Ensure chunk is loaded before inspecting blocks
        world.getChunkAt(x shr 4, z shr 4)

        val highestBlock = world.getHighestBlockAt(x, z)
        val groundType = highestBlock.type

        if (groundType.isAir || groundType == Material.VOID_AIR || groundType == Material.CAVE_AIR) {
            return null
        }

        if (settings.avoidLiquids && (highestBlock.isLiquid || groundType == Material.WATER || groundType == Material.LAVA)) {
            return null
        }

        if (settings.avoidLeaves && Tag.LEAVES.isTagged(groundType)) {
            return null
        }

        val spawnY = highestBlock.y + 1
        if (spawnY >= world.maxHeight - 1 || spawnY <= world.minHeight) {
            return null
        }

        val targetBlock = world.getBlockAt(x, spawnY, z)
        if (!targetBlock.type.isAir && !targetBlock.isPassable) {
            return null
        }

        return Location(world, x.toDouble(), spawnY.toDouble(), z.toDouble())
    }
}
