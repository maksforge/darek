package app.darek.listen

import org.junit.Assert.assertEquals
import org.junit.Test

/** Written by hand from ADR-0009's rule and phase 1's script. A wrong row here is a false fire. */
class CommandMatchTest {
    private val all = Command.entries

    private fun heard(text: String) = commandIn(tokensOf(text), all)

    @Test
    fun the_commands_fire() {
        assertEquals(Command.SAY_SOMETHING, heard("darek powiedz coś"))
        assertEquals(Command.GATE, heard("darek otwórz bramę"))
    }

    @Test
    fun words_before_and_after_are_ignored() {
        assertEquals(Command.SAY_SOMETHING, heard("[unk] darek powiedz coś"))
        assertEquals(Command.SAY_SOMETHING, heard("darek powiedz coś [unk]"))
        assertEquals(Command.SAY_SOMETHING, heard("[unk] [unk] darek powiedz coś marek"))
        assertEquals(Command.GATE, heard("darek otwórz bramę [unk]"))
        assertEquals(Command.GATE, heard("marek darek otwórz bramę"))
    }

    @Test
    fun near_misses_do_not_fire() {
        listOf(
            "marek powiedz coś",
            "jarek otwórz bramę",
            "janek otwórz bramę",
            "darka otwórz bramę",
            "darku powiedz coś",
            "dawid powiedz coś",
            "otwórz bramę",
            "powiedz coś",
            "darek",
            "darek powiedz",
            "darek otwórz",
            "darek otwórz brama",
            "darek zamknij bramę",
            "darek powiedz [unk] coś",
            "powiedz coś darek",
            "otwórz bramę darek",
            "darek [unk] otwórz bramę",
            "darek [unk] brama [unk]",
            "darek bramę otwórz",
            "darek coś powiedz",
            "",
        ).forEach { assertEquals(it, null, heard(it)) }
    }

    @Test
    fun a_command_that_is_not_enabled_does_not_fire() {
        assertEquals(null, commandIn(tokensOf("darek otwórz bramę"), listOf(Command.SAY_SOMETHING)))
        assertEquals(Command.SAY_SOMETHING, commandIn(tokensOf("darek powiedz coś"), listOf(Command.SAY_SOMETHING)))
    }

    @Test
    fun the_first_command_heard_wins() {
        assertEquals(Command.GATE, heard("darek otwórz bramę darek powiedz coś"))
    }

    @Test
    fun tokens_split_on_any_whitespace() {
        assertEquals(listOf("darek", "powiedz", "coś"), tokensOf(" darek  powiedz\tcoś\n"))
    }
}
