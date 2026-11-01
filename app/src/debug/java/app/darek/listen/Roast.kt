package app.darek.listen

import kotlin.random.Random

/**
 * Debug-only prototype of a Command with a name in it: „Hej Darek, obraź Szymona" answers with a
 * random wiązanka at that name. Masculine names only, because the insults' adjectives agree with
 * the name's gender.
 */
object Roast {
    private const val VERB = "obraź"

    data class Name(
        val accusative: String,
        val vocative: String,
    )

    val NAMES =
        listOf(
            Name("szymona", "Szymonie"),
            Name("kubę", "Kubo"),
            Name("tomka", "Tomku"),
            Name("pawła", "Pawle"),
            Name("michała", "Michale"),
            Name("piotrka", "Piotrku"),
            Name("bartka", "Bartku"),
            Name("krzyśka", "Krzyśku"),
            Name("maksa", "Maksie"),
            Name("wojtka", "Wojtku"),
            Name("adama", "Adamie"),
            Name("kamila", "Kamilu"),
            Name("łukasza", "Łukaszu"),
            Name("mateusza", "Mateuszu"),
            Name("jakuba", "Jakubie"),
            Name("grześka", "Grzesiu"),
        )

    private val INSULTS =
        listOf(
            "%s, ty pierdolony dzbanie, kurwa mać!",
            "%s, ty jebany cymbale, idź się utop w kałuży.",
            "Kurwa, %s, ty chuju złamany, weź spierdalaj.",
            "%s, ty ciulu jeden, do dupy z takim kolegą.",
            "Ja pierdolę, %s, ty łajzo bez mózgu.",
            "%s, ty kutafonie niemyty, mordę w kubeł i spierdalaj.",
        )

    val phrases: List<String> =
        listOf(VERB) + NAMES.map { it.accusative } + NAMES.map { "darek $VERB ${it.accusative}" }

    fun nameIn(tokens: List<String>): Name? =
        NAMES.firstOrNull { name ->
            val words = listOf("darek", VERB, name.accusative)
            tokens.windowed(words.size).any { it == words }
        }

    private var last = -1

    fun insult(name: Name): String {
        var pick = Random.nextInt(INSULTS.size)
        if (pick == last) pick = (pick + 1) % INSULTS.size
        last = pick
        return INSULTS[pick].format(name.vocative)
    }
}
