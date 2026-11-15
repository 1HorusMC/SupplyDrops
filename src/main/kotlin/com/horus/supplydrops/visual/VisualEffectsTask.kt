package com.horus.supplydrops.visual

import com.horus.supplydrops.config.VisualSettings
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

/**
 * Spawns periodic particles and an optional vertical particle beam above an active supply drop.
 */
class VisualEffectsTask(
    private val plugin: JavaPlugin,
    private val settings: VisualSettings,
    private val containerLocation: Location
) {
    private var particleTask: BukkitTask? = null
    private var beamTask: BukkitTask? = null

    fun start() {
        val center = containerLocation.clone().add(0.5, 0.7, 0.5)

        if (settings.particles.enabled) {
            val p = settings.particles
            particleTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                Runnable {
                    val world = center.world ?: return@Runnable
                    world.spawnParticle(
                        p.type,
                        center,
                        p.count,
                        p.radius,
                        0.4,
                        p.radius,
                        0.02
                    )
                },
                0L,
                p.intervalTicks
            )
        }

        if (settings.beam.enabled) {
            val b = settings.beam
            beamTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                Runnable {
                    val world = center.world ?: return@Runnable
                    var yOffset = 1.0
                    while (yOffset <= b.height) {
                        world.spawnParticle(
                            b.particle,
                            center.x,
                            center.y + yOffset,
                            center.z,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0
                        )
                        yOffset += b.step
                    }
                },
                0L,
                b.intervalTicks
            )
        }
    }

    fun stop() {
        particleTask?.cancel()
        particleTask = null
        beamTask?.cancel()
        beamTask = null
    }
}
