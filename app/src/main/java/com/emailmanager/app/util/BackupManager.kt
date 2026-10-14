package com.emailmanager.app.util

import com.emailmanager.app.data.EmailEntity
import com.emailmanager.app.data.EmailStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Reads and writes the backup file. Everything stays in a file the user chooses. */
object BackupManager {

    data class Item(
        val email: String,
        val label: String,
        val notes: String,
        val status: EmailStatus,
        val createdAt: Long
    )

    data class Parsed(val items: List<Item>, val skippedInvalid: Int)

    fun toJson(list: List<EmailEntity>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("email", it.email)
                    .put("label", it.label)
                    .put("notes", it.notes)
                    .put("status", it.status.name)
                    .put("createdAt", it.createdAt)
            )
        }
        return JSONObject()
            .put("app", "EmailManager")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("entries", arr)
            .toString(2)
    }

    /**
     * Accepts either this app's JSON backup, or plain text with one email per line.
     * @throws IllegalArgumentException when the file looks like JSON but is malformed.
     */
    fun parse(rawText: String): Parsed {
        val text = rawText.removePrefix("\uFEFF").trim()
        if (text.startsWith("{") || text.startsWith("[")) {
            try {
                val arr: JSONArray = (if (text.startsWith("{")) JSONObject(text).optJSONArray("entries") else JSONArray(text))
                    ?: throw IllegalArgumentException("No entries found in file")
                val items = ArrayList<Item>()
                val seen = HashSet<String>()
                var skipped = 0
                val now = System.currentTimeMillis()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    val email = EmailParser.normalize(obj?.optString("email") ?: arr.optString(i))
                    if (!EmailParser.isValid(email)) {
                        skipped++
                        continue
                    }
                    if (!seen.add(email)) continue
                    items.add(
                        Item(
                            email = email,
                            label = obj?.optString("label").orEmpty(),
                            notes = obj?.optString("notes").orEmpty(),
                            status = EmailStatus.fromName(obj?.optString("status")),
                            createdAt = obj?.optLong("createdAt", now) ?: now
                        )
                    )
                }
                return Parsed(items, skipped)
            } catch (e: JSONException) {
                throw IllegalArgumentException("File is not a valid backup")
            }
        }
        val parsed = EmailParser.parse(text)
        val now = System.currentTimeMillis()
        return Parsed(
            parsed.valid.mapIndexed { i, e -> Item(e, "", "", EmailStatus.UNUSED, now - i) },
            parsed.invalid.size
        )
    }
}
