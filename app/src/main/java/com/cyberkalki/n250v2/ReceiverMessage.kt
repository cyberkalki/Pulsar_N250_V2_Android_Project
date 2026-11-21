package com.cyberkalki.n250v2

import org.json.JSONObject

/**
 * Versioned, line-delimited message exchanged by the paired Motorola sender and OPPO receiver.
 * Send one compact JSON object per line, UTF-8. Text-only messages use type="text".
 */
data class ReceiverMessage(
    val version: Int = CURRENT_VERSION,
    val type: String,
    val text: String = "",
    val sender: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonLine(): String = JSONObject()
        .put("version", version)
        .put("type", type)
        .put("text", text)
        .put("sender", sender)
        .put("timestamp", timestamp)
        .toString()

    companion object {
        const val CURRENT_VERSION = 1
        const val MAX_LINE_CHARS = 8192

        fun parse(line: String): ReceiverMessage? {
            if (line.isBlank() || line.length > MAX_LINE_CHARS) return null
            return try {
                val json = JSONObject(line)
                val version = json.optInt("version", CURRENT_VERSION)
                val type = json.optString("type", "text")
                if (version != CURRENT_VERSION || type.length > 48) return null
                ReceiverMessage(
                    version = version,
                    type = type,
                    text = json.optString("text", line).take(MAX_LINE_CHARS),
                    sender = json.optString("sender", "").take(128),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                ReceiverMessage(type = "text", text = line.take(MAX_LINE_CHARS))
            }
        }
    }
}