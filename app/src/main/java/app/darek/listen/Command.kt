package app.darek.listen

/**
 * The catalogue (ADR-0009). Words are lower-case, as the recogniser emits them; the Reply is
 * speech, not UI, so it stays out of `strings.xml` (ADR-0012).
 */
enum class Command(
    phrase: String,
    val reply: String,
    val calls: Boolean,
) {
    SAY_SOMETHING("darek powiedz coś", "Co tam, mordo?", calls = false),
    GATE("darek otwórz bramę", "Otwieram bramę.", calls = true),
    ;

    val words: List<String> = phrase.split(' ')
}
