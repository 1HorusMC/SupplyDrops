package com.horus.supplydrops.area

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.World

/**
 * Result of resolving a WorldGuard region: its 2D bounding box and exact point-in-region predicate.
 */
data class ResolvedWorldGuardRegion(
    val bounds: AreaBounds,
    val containsBlock: (x: Int, y: Int, z: Int) -> Boolean
)

/**
 * Optional soft-dependency integration with WorldGuard 7.
 */
object WorldGuardHook {

    fun isAvailable(): Boolean = Bukkit.getPluginManager().isPluginEnabled("WorldGuard")

    fun resolveRegion(world: World, regionName: String): ResolvedWorldGuardRegion? {
        if (!isAvailable()) return null

        val container = WorldGuard.getInstance().platform.regionContainer
        val manager = container.get(BukkitAdapter.adapt(world)) ?: return null
        val region: ProtectedRegion = manager.getRegion(regionName) ?: return null

        val min = region.minimumPoint
        val max = region.maximumPoint
        val bounds = AreaBounds(
            minX = min.x(),
            maxX = max.x(),
            minZ = min.z(),
            maxZ = max.z()
        )

        return ResolvedWorldGuardRegion(
            bounds = bounds,
            containsBlock = { x, y, z -> region.contains(x, y, z) }
        )
    }
}
