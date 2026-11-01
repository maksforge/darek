package app.darek.listen

/**
 * ADR-0009's rule: the Command whose words appear adjacent and in order in one final result's raw
 * tokens, `[unk]` included, with anything before or after ignored. Partial results never come here.
 */
fun commandIn(
    tokens: List<String>,
    enabled: Collection<Command>,
): Command? =
    tokens.indices.firstNotNullOfOrNull { start ->
        enabled.firstOrNull { command -> tokens.hasAt(start, command.words) }
    }

/**
 * What one final result makes Darek do: fire a Command, say one of [NOT_UNDERSTOOD] when he heard
 * his own name and nothing he knows, or stay silent, so talk that never names him never gets an answer.
 */
fun reactionTo(
    tokens: List<String>,
    enabled: Collection<Command>,
): Reaction? = commandIn(tokens, enabled)?.let(Reaction::Fires) ?: Reaction.NotUnderstood.takeIf { NAME in tokens }

sealed interface Reaction {
    data class Fires(
        val command: Command,
    ) : Reaction

    data object NotUnderstood : Reaction
}

const val NAME = "darek"

/** Speech, not UI (ADR-0012): picked at random, never the same twice in a row. */
val NOT_UNDERSTOOD =
    listOf(
        "Stary, nie rozumiem cię.",
        "Co? Nie kumam.",
        "Sorry, nie załapałem.",
        "Powtórz, bo nie zrozumiałem.",
        "Mordo, nie wiem, o co ci chodzi.",
        "Hę? Możesz jeszcze raz?",
        "Nie ogarniam, powiedz to inaczej.",
        "Słyszę cię, ale nie wiem, czego chcesz.",
    )

fun tokensOf(text: String): List<String> = text.split(' ', '\t', '\n').filter { it.isNotEmpty() }

private fun List<String>.hasAt(
    start: Int,
    words: List<String>,
): Boolean = start + words.size <= size && words.indices.all { this[start + it] == words[it] }
