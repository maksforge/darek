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

fun tokensOf(text: String): List<String> = text.split(' ', '\t', '\n').filter { it.isNotEmpty() }

private fun List<String>.hasAt(
    start: Int,
    words: List<String>,
): Boolean = start + words.size <= size && words.indices.all { this[start + it] == words[it] }
