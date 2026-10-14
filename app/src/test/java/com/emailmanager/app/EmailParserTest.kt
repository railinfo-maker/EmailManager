package com.emailmanager.app

import com.emailmanager.app.util.EmailParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailParserTest {

    @Test fun validAddressesAreAccepted() {
        listOf("a@b.com", "first.last+tag@sub.example.co.in", "O'Brien@example.org", "x_y%z@a-b.io")
            .forEach { assertTrue(it, EmailParser.isValid(it.lowercase())) }
    }

    @Test fun invalidAddressesAreRejected() {
        listOf("plain", "a@b", "@b.com", "a@.com", "a..b@c.com", ".a@c.com", "a.@c.com", "a b@c.com", "a@b.c", "a@-b.com")
            .forEach { assertFalse(it, EmailParser.isValid(it)) }
    }

    @Test fun parseRemovesDuplicatesAndNormalizesCase() {
        val r = EmailParser.parse("One@Gmail.com\nTWO@gmail.com\none@gmail.com\n\n  two@gmail.com  ")
        assertEquals(listOf("one@gmail.com", "two@gmail.com"), r.valid)
        assertEquals(2, r.duplicatesRemoved)
        assertTrue(r.invalid.isEmpty())
    }

    @Test fun parseReportsInvalidLines() {
        val r = EmailParser.parse("good@example.com\nnot-an-email\nbad@@example.com")
        assertEquals(listOf("good@example.com"), r.valid)
        assertEquals(listOf("not-an-email", "bad@@example.com"), r.invalid)
    }

    @Test fun parseHandlesMailtoAndAngleBrackets() {
        val r = EmailParser.parse("mailto:Alice@Example.com\nBob Smith <bob@example.com>\n\"carol@example.com\",")
        assertEquals(listOf("alice@example.com", "bob@example.com", "carol@example.com"), r.valid)
    }

    @Test fun parseSplitsCommaSeparatedValuesOnOneLine() {
        val r = EmailParser.parse("a@x.com, b@x.com; c@x.com")
        assertEquals(listOf("a@x.com", "b@x.com", "c@x.com"), r.valid)
    }
}
