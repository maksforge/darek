package app.darek.listen

import kotlinx.serialization.json.Json

/**
 * The recogniser's grammar (ADR-0009): each enabled Command as a phrase and word by word, the
 * Decoys, the cancel words while a Command that calls is enabled, and `[unk]`.
 *
 * kotlinx.serialization writes `coś` as UTF-8; Vosk takes a `ś` escape for an unknown word and
 * drops it, and the Command could never fire.
 */
object Grammar {
    val DECOYS = listOf("marek", "jarek", "janek", "darka", "darku", "dawid", "otwórz", "zamknij", "brama")
    val CANCEL_WORDS = listOf("stop", "anuluj")
    const val UNKNOWN = "[unk]"

    fun phrases(enabled: Set<Command>): List<String> =
        buildList {
            enabled.forEach { command ->
                add(command.words.joinToString(" "))
                addAll(command.words)
            }
            addAll(DECOYS)
            if (enabled.any { it.calls }) addAll(CANCEL_WORDS)
            add(UNKNOWN)
        }.distinct()

    fun json(enabled: Set<Command>): String = Json.encodeToString(phrases(enabled))
}
