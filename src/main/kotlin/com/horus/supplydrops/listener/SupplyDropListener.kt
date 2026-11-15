package com.horus.supplydrops.listener

import com.horus.supplydrops.drop.SupplyDropManager
import com.horus.supplydrops.visual.HologramDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Handles container looting/breaking and cleans up orphaned hologram entities.
 */
class SupplyDropListener(
    private val plugin: JavaPlugin,
    private val manager: SupplyDropManager
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!manager.getConfig().despawnWhenEmpty) return

        val location = event.inventory.location ?: return
        val drop = manager.findDropAt(location) ?: return

        if (event.inventory.isEmpty) {
            manager.despawnDrop(drop, expired = false, claimedBy = event.player.name)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val drop = manager.findDropAt(event.block.location) ?: return
        event.isDropItems = false
        manager.despawnDrop(drop, expired = false, claimedBy = event.player.name)
    }

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        for (entity in event.chunk.entities) {
            if (HologramDisplay.isTaggedHologram(plugin, entity) && !manager.ownsHologram(entity.uniqueId)) {
                entity.remove()
            }
        }
    }
}
