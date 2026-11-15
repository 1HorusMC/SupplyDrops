package com.horus.supplydrops.discord

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WebhookUrlTest {
    @Test
    fun `accepts only https discord webhook urls`() {
        assertTrue(DiscordWebhookClient.isValidWebhookUrl("https://discord.com/api/webhooks/1/abc"))
        assertFalse(DiscordWebhookClient.isValidWebhookUrl("http://discord.com/api/webhooks/1/abc"))
        assertFalse(DiscordWebhookClient.isValidWebhookUrl("https://127.0.0.1/api/webhooks/1/abc"))
        assertFalse(DiscordWebhookClient.isValidWebhookUrl("https://discord.com.evil.com/api/webhooks/1/abc"))
        assertFalse(DiscordWebhookClient.isValidWebhookUrl("https://discord.com@evil.com/api/webhooks/1/abc"))
        assertFalse(DiscordWebhookClient.isValidWebhookUrl("file:///etc/passwd"))
    }
}
