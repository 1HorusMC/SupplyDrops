package com.horus.supplydrops.listener

import com.horus.supplydrops.drop.SupplyDropManager
import com.horus.supplydrops.visual.HologramDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
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

    @EventHandler(ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf { manager.findDropAt(it.location) != null }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { manager.findDropAt(it.location) != null }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { manager.findDropAt(it.location) != null }) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { manager.findDropAt(it.location) != null }) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onHopperMove(event: InventoryMoveItemEvent) {
        val src = event.source.location
        val dst = event.destination.location
        if ((src != null && manager.findDropAt(src) != null) ||
            (dst != null && manager.findDropAt(dst) != null)
        ) {
            event.isCancelled = true
        }
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
