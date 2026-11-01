package app.darek.listen

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A grammar Vosk cannot read drops its words without a sound, and the Command never fires. */
class GrammarTest {
    private val callOff = Grammar.json(setOf(Command.SAY_SOMETHING))
    private val callOn = Grammar.json(Command.entries.toSet())

    private fun phrases(json: String): List<String> = Json.decodeFromString(json)

    @Test
    fun with_the_call_off_there_is_no_gate_and_no_cancel_word() {
        assertTrue(callOff, callOff.contains("\"darek powiedz coś\""))
        assertFalse(callOff, callOff.contains("\\u"))
        val phrases = phrases(callOff)
        assertFalse(phrases.any { "bramę" in it })
        Grammar.CANCEL_WORDS.forEach { assertFalse(it, it in phrases) }
    }

    @Test
    fun with_the_call_on_the_gate_and_the_cancel_words_are_in() {
        assertTrue(callOn, callOn.contains("\"darek otwórz bramę\""))
        assertFalse(callOn, callOn.contains("\\u"))
        assertTrue(
            callOn
                .toByteArray(Charsets.UTF_8)
                .asList()
                .windowed(2)
                .contains(listOf(0xC4.toByte(), 0x99.toByte())),
        )
        val phrases = phrases(callOn)
        assertTrue(phrases.containsAll(Grammar.CANCEL_WORDS))
        assertTrue("bramę" in phrases)
    }

    @Test
    fun the_decoys_and_unk_are_always_in() {
        listOf(callOff, callOn).map(::phrases).forEach { phrases ->
            assertTrue(phrases.containsAll(Grammar.DECOYS))
            assertTrue(Grammar.UNKNOWN in phrases)
            assertEquals(phrases.distinct(), phrases)
        }
    }
}
