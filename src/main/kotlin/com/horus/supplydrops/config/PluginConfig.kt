package com.horus.supplydrops.config

import com.horus.supplydrops.area.AreaBounds
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.configuration.file.FileConfiguration

enum class AreaMode {
    COORDINATES,
    WORLDGUARD;

    companion object {
        fun fromString(value: String?): AreaMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: COORDINATES
    }
}

enum class ContainerType(val material: Material) {
    CHEST(Material.CHEST),
    BARREL(Material.BARREL);

    companion object {
        fun fromString(value: String?): ContainerType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CHEST
    }
}

data class LootSettings(
    val minItems: Int,
    val maxItems: Int,
    val allowDuplicates: Boolean
)

data class AreaSettings(
    val mode: AreaMode,
    val worldName: String,
    val bounds: AreaBounds,
    val worldGuardRegion: String,
    val maxLocationAttempts: Int,
    val avoidLiquids: Boolean,
    val avoidLeaves: Boolean
)

data class ParticleSettings(
    val enabled: Boolean,
    val type: Particle,
    val count: Int,
    val intervalTicks: Long,
    val radius: Double
)

data class BeamSettings(
    val enabled: Boolean,
    val particle: Particle,
    val height: Int,
    val step: Double,
    val intervalTicks: Long
)

data class HologramSettings(
    val enabled: Boolean,
    val heightOffset: Double,
    val seeThrough: Boolean,
    val shadow: Boolean,
    val backgroundColor: Color?,
    val lines: List<String>
)

data class VisualSettings(
    val lightningOnSpawn: Boolean,
    val particles: ParticleSettings,
    val beam: BeamSettings,
    val hologram: HologramSettings
)

data class MessageSettings(
    val prefix: String,
    val spawnBroadcast: List<String>,
    val claimedBroadcast: String,
    val expiredBroadcast: String,
    val commandNoPermission: String,
    val commandUsage: String,
    val commandStarted: String,
    val commandStartFailed: String,
    val commandStopped: String,
    val commandNoneActive: String,
    val commandReloaded: String
)

data class DiscordSettings(
    val enabled: Boolean,
    val webhookUrl: String,
    val username: String,
    val avatarUrl: String,
    val embedTitle: String,
    val embedDescription: String,
    val embedColor: Int,
    val embedFooter: String
)

data class PluginConfig(
    val intervalMinutes: Int,
    val durationSeconds: Int,
    val allowMultiple: Boolean,
    val despawnWhenEmpty: Boolean,
    val containerType: ContainerType,
    val loot: LootSettings,
    val area: AreaSettings,
    val visuals: VisualSettings,
    val messages: MessageSettings,
    val discord: DiscordSettings
) {
    companion object {
        fun fromFileConfiguration(config: FileConfiguration): PluginConfig {
            val intervalMinutes = config.getInt("interval-minutes", 30)
            val durationSeconds = config.getInt("duration-seconds", 300).coerceAtLeast(5)
            val allowMultiple = config.getBoolean("allow-multiple", false)
            val despawnWhenEmpty = config.getBoolean("despawn-when-empty", true)
            val containerType = ContainerType.fromString(config.getString("container-type"))

            val loot = LootSettings(
                minItems = config.getInt("loot.min-items", 3).coerceAtLeast(0),
                maxItems = config.getInt("loot.max-items", 6).coerceIn(1, 54),
                allowDuplicates = config.getBoolean("loot.allow-duplicates", false)
            )

            val area = AreaSettings(
                mode = AreaMode.fromString(config.getString("area.mode")),
                worldName = config.getString("area.world", "world") ?: "world",
                bounds = AreaBounds(
                    minX = config.getInt("area.coordinates.min-x", -500),
                    maxX = config.getInt("area.coordinates.max-x", 500),
                    minZ = config.getInt("area.coordinates.min-z", -500),
                    maxZ = config.getInt("area.coordinates.max-z", 500)
                ),
                worldGuardRegion = config.getString("area.worldguard.region-name", "pvp_arena") ?: "pvp_arena",
                maxLocationAttempts = config.getInt("area.max-location-attempts", 30).coerceIn(1, 100),
                avoidLiquids = config.getBoolean("area.avoid-liquids", true),
                avoidLeaves = config.getBoolean("area.avoid-leaves", true)
            )

            val particles = ParticleSettings(
                enabled = config.getBoolean("visuals.particles.enabled", true),
                type = parseParticle(config.getString("visuals.particles.type"), Particle.TOTEM_OF_UNDYING),
                count = config.getInt("visuals.particles.count", 10).coerceIn(1, 200),
                intervalTicks = config.getLong("visuals.particles.interval-ticks", 10L).coerceAtLeast(1L),
                radius = config.getDouble("visuals.particles.radius", 0.8).coerceAtLeast(0.1)
            )

            val beam = BeamSettings(
                enabled = config.getBoolean("visuals.beam.enabled", true),
                particle = parseParticle(config.getString("visuals.beam.particle"), Particle.END_ROD),
                height = config.getInt("visuals.beam.height", 30).coerceIn(1, 128),
                step = config.getDouble("visuals.beam.step", 1.0).coerceAtLeast(0.5),
                intervalTicks = config.getLong("visuals.beam.interval-ticks", 10L).coerceAtLeast(1L)
            )

            val hologram = HologramSettings(
                enabled = config.getBoolean("visuals.hologram.enabled", true),
                heightOffset = config.getDouble("visuals.hologram.height-offset", 1.65),
                seeThrough = config.getBoolean("visuals.hologram.see-through", false),
                shadow = config.getBoolean("visuals.hologram.shadow", true),
                backgroundColor = parseArgbColor(config.getString("visuals.hologram.background-color")),
                lines = config.getStringList("visuals.hologram.lines").ifEmpty {
                    listOf(
                        "<gradient:#ffaa00:#ff5500><bold>✦ SUPPLY DROP ✦</bold></gradient>",
                        "<gray>Desaparece en: <yellow>{time}"
                    )
                }
            )

            val visuals = VisualSettings(
                lightningOnSpawn = config.getBoolean("visuals.lightning-on-spawn", true),
                particles = particles,
                beam = beam,
                hologram = hologram
            )

            val messages = MessageSettings(
                prefix = config.getString("messages.prefix", "<gold>[SupplyDrops] ") ?: "<gold>[SupplyDrops] ",
                spawnBroadcast = config.getStringList("messages.spawn-broadcast"),
                claimedBroadcast = config.getString("messages.claimed-broadcast", "{prefix}<green>¡{player} ha saqueado el Supply Drop!") ?: "",
                expiredBroadcast = config.getString("messages.expired-broadcast", "{prefix}<red>El Supply Drop ha expirado.") ?: "",
                commandNoPermission = config.getString("messages.command-no-permission", "{prefix}<red>Sin permiso.") ?: "",
                commandUsage = config.getString("messages.command-usage", "{prefix}<gray>Uso: /supplydrop <start|stop|reload>") ?: "",
                commandStarted = config.getString("messages.command-started", "{prefix}<green>Supply Drop iniciado.") ?: "",
                commandStartFailed = config.getString("messages.command-start-failed", "{prefix}<red>No se pudo iniciar.") ?: "",
                commandStopped = config.getString("messages.command-stopped", "{prefix}<yellow>Detenido(s) {count} drop(s).") ?: "",
                commandNoneActive = config.getString("messages.command-none-active", "{prefix}<gray>No hay drops activos.") ?: "",
                commandReloaded = config.getString("messages.command-reloaded", "{prefix}<green>Recargado.") ?: ""
            )

            val discord = DiscordSettings(
                enabled = config.getBoolean("discord.enabled", false),
                webhookUrl = config.getString("discord.webhook-url", "") ?: "",
                username = config.getString("discord.username", "HorusSupplyDrops") ?: "HorusSupplyDrops",
                avatarUrl = config.getString("discord.avatar-url", "") ?: "",
                embedTitle = config.getString("discord.embed.title", "Supply Drop") ?: "Supply Drop",
                embedDescription = config.getString("discord.embed.description", "") ?: "",
                embedColor = parseRgbHex(config.getString("discord.embed.color"), 0xFFAA00),
                embedFooter = config.getString("discord.embed.footer", "HorusSupplyDrops") ?: "HorusSupplyDrops"
            )

            return PluginConfig(
                intervalMinutes = intervalMinutes,
                durationSeconds = durationSeconds,
                allowMultiple = allowMultiple,
                despawnWhenEmpty = despawnWhenEmpty,
                containerType = containerType,
                loot = loot,
                area = area,
                visuals = visuals,
                messages = messages,
                discord = discord
            )
        }

        private fun parseParticle(name: String?, fallback: Particle): Particle {
            if (name.isNullOrBlank()) return fallback
            return runCatching { Particle.valueOf(name.trim().uppercase()) }.getOrDefault(fallback)
        }

        internal fun parseArgbColor(hex: String?): Color? {
            if (hex.isNullOrBlank() || hex.equals("NONE", ignoreCase = true)) return null
            val cleaned = hex.trim().removePrefix("#")
            return runCatching {
                when (cleaned.length) {
                    8 -> {
                        val argb = cleaned.toLong(16).toInt()
                        Color.fromARGB(argb)
                    }
                    6 -> {
                        val rgb = cleaned.toInt(16)
                        Color.fromRGB(rgb)
                    }
                    else -> null
                }
            }.getOrNull()
        }

        internal fun parseRgbHex(hex: String?, fallback: Int): Int {
            if (hex.isNullOrBlank()) return fallback
            val cleaned = hex.trim().removePrefix("#")
            return runCatching { cleaned.toInt(16) and 0xFFFFFF }.getOrDefault(fallback)
        }
    }
}
