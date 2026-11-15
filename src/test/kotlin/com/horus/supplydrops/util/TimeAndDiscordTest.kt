package com.horus.supplydrops.util

import com.horus.supplydrops.discord.DiscordWebhookClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimeAndDiscordTest {

    @Test
    fun `formats seconds into mm ss and hh mm ss`() {
        assertEquals("00:00", TimeFormatter.formatDuration(-5))
        assertEquals("00:45", TimeFormatter.formatDuration(45))
        assertEquals("05:00", TimeFormatter.formatDuration(300))
        assertEquals("01:01:05", TimeFormatter.formatDuration(3665))
    }

    @Test
    fun `escapes json characters in discord embed payload`() {
        val payload = DiscordWebhookClient.buildEmbedPayload(
            username = "Horus \"Bot\"",
            avatarUrl = "",
            title = "Drop\nTitle",
            description = "Line 1\\Line 2",
            color = 0xFFAA00,
            footer = "Footer",
            timestampIso = "2026-11-15T10:00:00Z"
        )

        assertTrue(payload.contains("\"username\":\"Horus \\\"Bot\\\"\""))
        assertTrue(payload.contains("\"title\":\"Drop\\nTitle\""))
        assertTrue(payload.contains("\"description\":\"Line 1\\\\Line 2\""))
        assertTrue(payload.contains("\"color\":16755200"))
    }
}
