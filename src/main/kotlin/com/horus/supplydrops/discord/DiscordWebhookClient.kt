package com.horus.supplydrops.discord

import com.horus.supplydrops.config.DiscordSettings
import org.bukkit.plugin.java.JavaPlugin
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

/**
 * Sends Discord webhook embed notifications asynchronously using JDK 21's [HttpClient].
 */
class DiscordWebhookClient(private val plugin: JavaPlugin) {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun sendSpawnNotification(
        settings: DiscordSettings,
        worldName: String,
        x: Int,
        y: Int,
        z: Int,
        formattedTime: String
    ) {
        if (!settings.enabled || settings.webhookUrl.isBlank()) return

        val replacePlaceholders: (String) -> String = { input ->
            input
                .replace("{world}", worldName)
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())
                .replace("{z}", z.toString())
                .replace("{time}", formattedTime)
        }

        val payload = buildEmbedPayload(
            username = settings.username,
            avatarUrl = settings.avatarUrl,
            title = replacePlaceholders(settings.embedTitle),
            description = replacePlaceholders(settings.embedDescription),
            color = settings.embedColor,
            footer = replacePlaceholders(settings.embedFooter)
        )

        val request = runCatching {
            HttpRequest.newBuilder()
                .uri(URI.create(settings.webhookUrl.trim()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build()
        }.getOrElse { ex ->
            plugin.logger.warning("Invalid Discord webhook URL: ${ex.message}")
            return
        }

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .whenComplete { response, throwable ->
                if (throwable != null) {
                    plugin.logger.warning("Failed to send Discord webhook: ${throwable.message}")
                } else if (response.statusCode() !in 200..299) {
                    plugin.logger.warning("Discord webhook returned HTTP ${response.statusCode()}.")
                }
            }
    }

    companion object {
        internal fun buildEmbedPayload(
            username: String,
            avatarUrl: String,
            title: String,
            description: String,
            color: Int,
            footer: String,
            timestampIso: String = Instant.now().toString()
        ): String {
            val sb = StringBuilder()
            sb.append('{')
            if (username.isNotBlank()) {
                sb.append("\"username\":\"").append(escapeJson(username)).append("\",")
            }
            if (avatarUrl.isNotBlank()) {
                sb.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\",")
            }
            sb.append("\"embeds\":[{")
            sb.append("\"title\":\"").append(escapeJson(title)).append("\",")
            sb.append("\"description\":\"").append(escapeJson(description)).append("\",")
            sb.append("\"color\":").append(color and 0xFFFFFF).append(',')
            sb.append("\"timestamp\":\"").append(escapeJson(timestampIso)).append('"')
            if (footer.isNotBlank()) {
                sb.append(",\"footer\":{\"text\":\"").append(escapeJson(footer)).append("\"}")
            }
            sb.append("}]}")
            return sb.toString()
        }

        internal fun escapeJson(value: String): String {
            val out = StringBuilder(value.length + 8)
            for (ch in value) {
                when (ch) {
                    '\\' -> out.append("\\\\")
                    '"' -> out.append("\\\"")
                    '\n' -> out.append("\\n")
                    '\r' -> out.append("\\r")
                    '\t' -> out.append("\\t")
                    '\b' -> out.append("\\b")
                    '\u000C' -> out.append("\\f")
                    else -> {
                        if (ch.code < 0x20) {
                            out.append("\\u%04x".format(ch.code))
                        } else {
                            out.append(ch)
                        }
                    }
                }
            }
            return out.toString()
        }
    }
}
