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
 * What one final result makes Darek do: fire a Command, say [NOT_UNDERSTOOD] when he heard his own
 * name and nothing he knows, or stay silent, so talk that never names him never gets an answer.
 */
fun reactionTo(
    tokens: List<String>,
    enabled: Collection<Command>,
): Reaction? = commandIn(tokens, enabled)?.let(Reaction::Fires) ?: Reaction.NotUnderstood.takeIf { NAME in tokens }

sealed interface Reaction {
    val reply: String

    data class Fires(
        val command: Command,
    ) : Reaction {
        override val reply: String get() = command.reply
    }

    data object NotUnderstood : Reaction {
        override val reply = NOT_UNDERSTOOD
    }
}

const val NAME = "darek"
const val NOT_UNDERSTOOD = "Stary, nie rozumiem cię."

fun tokensOf(text: String): List<String> = text.split(' ', '\t', '\n').filter { it.isNotEmpty() }

private fun List<String>.hasAt(
    start: Int,
    words: List<String>,
): Boolean = start + words.size <= size && words.indices.all { this[start + it] == words[it] }
