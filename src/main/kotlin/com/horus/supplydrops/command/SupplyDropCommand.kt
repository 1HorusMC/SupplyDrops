package com.horus.supplydrops.command

import com.horus.supplydrops.HorusSupplyDropsPlugin
import com.horus.supplydrops.drop.SupplyDropManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * Handles `/supplydrop <start|stop|reload>` with permission `horussupplydrops.admin`.
 */
class SupplyDropCommand(
    private val plugin: HorusSupplyDropsPlugin,
    private val manager: SupplyDropManager,
    private val miniMessage: MiniMessage
) : CommandExecutor, TabCompleter {

    companion object {
        const val ADMIN_PERMISSION = "horussupplydrops.admin"
        private val SUBCOMMANDS = listOf("start", "stop", "reload")
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val messages = manager.getConfig().messages
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(miniMessage.deserialize(messages.commandNoPermission.replace("{prefix}", messages.prefix)))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(miniMessage.deserialize(messages.commandUsage.replace("{prefix}", messages.prefix)))
            return true
        }

        when (args[0].lowercase()) {
            "start" -> {
                val drop = manager.spawnSupplyDrop()
                if (drop != null) {
                    val loc = drop.location
                    val msg = messages.commandStarted
                        .replace("{prefix}", messages.prefix)
                        .replace("{world}", loc.world?.name ?: "world")
                        .replace("{x}", loc.blockX.toString())
                        .replace("{y}", loc.blockY.toString())
                        .replace("{z}", loc.blockZ.toString())
                    sender.sendMessage(miniMessage.deserialize(msg))
                } else {
                    sender.sendMessage(
                        miniMessage.deserialize(messages.commandStartFailed.replace("{prefix}", messages.prefix))
                    )
                }
            }

            "stop" -> {
                val stopped = manager.stopAll()
                val raw = if (stopped > 0) {
                    messages.commandStopped.replace("{count}", stopped.toString())
                } else {
                    messages.commandNoneActive
                }
                sender.sendMessage(miniMessage.deserialize(raw.replace("{prefix}", messages.prefix)))
            }

            "reload" -> {
                plugin.reloadPluginConfiguration()
                val updatedMessages = manager.getConfig().messages
                sender.sendMessage(
                    miniMessage.deserialize(updatedMessages.commandReloaded.replace("{prefix}", updatedMessages.prefix))
                )
            }

            else -> {
                sender.sendMessage(miniMessage.deserialize(messages.commandUsage.replace("{prefix}", messages.prefix)))
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission(ADMIN_PERMISSION)) return emptyList()
        if (args.size == 1) {
            val prefix = args[0].lowercase()
            return SUBCOMMANDS.filter { it.startsWith(prefix) }
        }
        return emptyList()
    }
}
