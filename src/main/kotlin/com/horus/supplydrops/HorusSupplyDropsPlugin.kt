package com.horus.supplydrops

import com.horus.supplydrops.area.LocationFinder
import com.horus.supplydrops.command.SupplyDropCommand
import com.horus.supplydrops.config.PluginConfig
import com.horus.supplydrops.discord.DiscordWebhookClient
import com.horus.supplydrops.drop.SupplyDropManager
import com.horus.supplydrops.listener.SupplyDropListener
import com.horus.supplydrops.loot.LootManager
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main entrypoint for HorusSupplyDrops.
 */
class HorusSupplyDropsPlugin : JavaPlugin() {

    // Restricted MiniMessage: no <click>, <hover>, <insertion>, <key>... tags from config text.
    private val miniMessage: MiniMessage = MiniMessage.builder()
        .tags(
            TagResolver.resolver(
                StandardTags.color(),
                StandardTags.decorations(),
                StandardTags.gradient(),
                StandardTags.rainbow(),
                StandardTags.reset(),
                StandardTags.newline()
            )
        )
        .build()
    private lateinit var lootManager: LootManager
    private lateinit var supplyDropManager: SupplyDropManager

    override fun onEnable() {
        saveDefaultConfig()
        restrictConfigPermissions()

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
        // Worlds are loaded by the next tick: clean containers left by a previous crash.
        server.scheduler.runTask(this, Runnable { supplyDropManager.orphanStore.cleanup() })

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

    /** config.yml may contain the Discord webhook secret: make it owner-only where POSIX is supported. */
    private fun restrictConfigPermissions() {
        runCatching {
            java.nio.file.Files.setPosixFilePermissions(
                java.io.File(dataFolder, "config.yml").toPath(),
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")
            )
        }
    }

    fun reloadPluginConfiguration() {
        reloadConfig()
        lootManager.load()
        val updated = PluginConfig.fromFileConfiguration(config)
        supplyDropManager.updateConfig(updated)
    }
}
