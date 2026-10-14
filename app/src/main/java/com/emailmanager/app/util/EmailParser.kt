package com.emailmanager.app.util

/** Pure Kotlin (no Android classes) so it can be unit-tested on the JVM. */
object EmailParser {

    data class ParseResult(
        /** Valid, normalized (lowercase), de-duplicated emails in the order they appeared. */
        val valid: List<String>,
        /** Entries that could not be validated, exactly as typed. */
        val invalid: List<String>,
        /** How many repeated entries were removed from the pasted text. */
        val duplicatesRemoved: Int
    )

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+'-]+@([A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,24}$")
    private val ANGLE = Regex("<([^<>]+)>")
    private val SPLIT = Regex("[,;\\s]+")
    private const val TRIM_CHARS = "\"'`,;:()[]<>. \t"

    /** Trims, strips "mailto:", "Name <addr>" wrappers and surrounding punctuation, lowercases. */
    fun normalize(raw: String): String {
        var s = raw.trim()
        ANGLE.find(s)?.let { s = it.groupValues[1] }
        s = s.trim()
        if (s.startsWith("mailto:", ignoreCase = true)) s = s.substring(7)
        s = s.trim { it in TRIM_CHARS }
        return s.lowercase()
    }

    fun isValid(email: String): Boolean {
        if (email.length > 254 || !EMAIL_REGEX.matches(email)) return false
        val local = email.substringBefore('@')
        return local.length <= 64 &&
            !local.startsWith('.') &&
            !local.endsWith('.') &&
            !local.contains("..")
    }

    fun parse(text: String): ParseResult {
        val valid = LinkedHashSet<String>()
        val invalid = ArrayList<String>()
        var duplicates = 0
        text.lineSequence().forEach { line ->
            val l = line.trim()
            if (l.isEmpty()) return@forEach
            val tokens =
                if (l.contains('<') && l.contains('>')) listOf(l)
                else l.split(SPLIT).filter { it.isNotBlank() }
            tokens.forEach { token ->
                val n = normalize(token)
                if (isValid(n)) {
                    if (!valid.add(n)) duplicates++
                } else {
                    invalid.add(token.trim())
                }
            }
        }
        return ParseResult(valid.toList(), invalid, duplicates)
    }
}
