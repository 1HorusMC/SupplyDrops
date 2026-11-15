package com.horus.supplydrops

import com.horus.supplydrops.area.LocationFinder
import com.horus.supplydrops.command.SupplyDropCommand
import com.horus.supplydrops.config.PluginConfig
import com.horus.supplydrops.discord.DiscordWebhookClient
import com.horus.supplydrops.drop.SupplyDropManager
import com.horus.supplydrops.listener.SupplyDropListener
import com.horus.supplydrops.loot.LootManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main entrypoint for HorusSupplyDrops.
 */
class HorusSupplyDropsPlugin : JavaPlugin() {

    private val miniMessage: MiniMessage = MiniMessage.miniMessage()
    private lateinit var lootManager: LootManager
    private lateinit var supplyDropManager: SupplyDropManager

    override fun onEnable() {
        saveDefaultConfig()

        lootManager = LootManager(this, miniMessage)
        lootManager.load()

        val pluginConfig = PluginConfig.fromFileConfiguration(config)
        val locationFinder = LocationFinder(this)
        val discordClient = DiscordWebhookClient(this)

        supplyDropManager = SupplyDropManager(
            plugin = this,
            miniMessage = miniMessage,
            lootManager = lootManager,
            locationFinder = locationFinder,
            discordClient = discordClient,
            config = pluginConfig
        )
        supplyDropManager.startAutoScheduler()

        val commandHandler = SupplyDropCommand(this, supplyDropManager, miniMessage)
        getCommand("supplydrop")?.apply {
            setExecutor(commandHandler)
            tabCompleter = commandHandler
        }

        server.pluginManager.registerEvents(SupplyDropListener(this, supplyDropManager), this)
        logger.info("HorusSupplyDrops enabled.")
    }

    override fun onDisable() {
        if (::supplyDropManager.isInitialized) {
            supplyDropManager.stopAutoScheduler()
            supplyDropManager.stopAll()
        }
        logger.info("HorusSupplyDrops disabled.")
    }

    fun reloadPluginConfiguration() {
        reloadConfig()
        lootManager.load()
        val updated = PluginConfig.fromFileConfiguration(config)
        supplyDropManager.updateConfig(updated)
    }
}
