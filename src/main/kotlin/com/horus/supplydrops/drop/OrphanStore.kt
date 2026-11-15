package com.horus.supplydrops.drop

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Persists active drop locations so containers left behind by a crash can be cleaned on next startup.
 */
class OrphanStore(private val plugin: JavaPlugin) {
    private val file = File(plugin.dataFolder, "active-drops.yml")
    private val entries = linkedSetOf<String>()

    private fun key(world: String, x: Int, y: Int, z: Int) = "$world;$x;$y;$z"

    @Synchronized
    fun add(world: String, x: Int, y: Int, z: Int) {
        entries.add(key(world, x, y, z))
        save()
    }

    @Synchronized
    fun remove(world: String, x: Int, y: Int, z: Int) {
        entries.remove(key(world, x, y, z))
        save()
    }

    /** Removes containers recorded by a previous run. Call after worlds are loaded. */
    @Synchronized
    fun cleanup() {
        if (!file.exists()) return
        val saved = YamlConfiguration.loadConfiguration(file).getStringList("drops")
        for (raw in saved) {
            val p = raw.split(';')
            if (p.size != 4) continue
            val world = Bukkit.getWorld(p[0]) ?: continue
            val x = p[1].toIntOrNull() ?: continue
            val y = p[2].toIntOrNull() ?: continue
            val z = p[3].toIntOrNull() ?: continue
            val block = world.getBlockAt(x, y, z)
            if (block.type == Material.CHEST || block.type == Material.BARREL) {
                (block.state as? Container)?.inventory?.clear()
                block.type = Material.AIR
                plugin.logger.info("Removed orphaned supply drop at $x,$y,$z in ${p[0]}.")
            }
        }
        entries.clear()
        save()
    }

    private fun save() {
        runCatching {
            plugin.dataFolder.mkdirs()
            val yaml = YamlConfiguration()
            yaml.set("drops", entries.toList())
            yaml.save(file)
        }.onFailure { plugin.logger.warning("Could not persist active drops: ${it.javaClass.simpleName}") }
    }
}
