package app.darek.listen

import kotlin.random.Random

/**
 * Debug-only prototype of a Command with a name in it: „Hej Darek, obraź Szymona" answers with a
 * random wiązanka at the last word heard. Grammar mode hears only [KNOWN]; free decoding hears any
 * name, and [vocative] guesses its form and gender from the ending.
 */
object Roast {
    private const val VERB = "obraź"

    enum class Gender { MALE, FEMALE, UNKNOWN }

    data class Name(
        val vocative: String,
        val gender: Gender,
    )

    private val KNOWN =
        listOf(
            "szymona",
            "kubę",
            "tomka",
            "pawła",
            "michała",
            "piotrka",
            "bartka",
            "krzyśka",
            "maksa",
            "wojtka",
            "adama",
            "kamila",
            "łukasza",
            "mateusza",
            "jakuba",
            "grześka",
            "kasię",
            "anię",
            "karolinę",
            "martę",
            "ewę",
            "magdę",
            "olę",
            "zosię",
            "julię",
            "monikę",
            "agnieszkę",
            "natalię",
            "paulinę",
            "kingę",
        )

    private val MASCULINE_IN_E = setOf("kubę", "barnabę", "kosmę")

    private val TAKES_U = listOf("k", "g", "ch", "sz", "cz", "rz", "ż", "j", "l", "ś", "ć", "ń", "ź", "c")

    private val MALE =
        listOf(
            "%s, ty pierdolony dzbanie, kurwa mać!",
            "%s, ty jebany cymbale, idź się utop w kałuży.",
            "Kurwa, %s, ty chuju złamany, weź spierdalaj.",
            "%s, ty ciulu jeden, do dupy z takim kolegą.",
            "%s, ty kutafonie niemyty, mordę w kubeł i spierdalaj.",
        )

    private val FEMALE =
        listOf(
            "%s, ty jebana łajzo, idź się utop w kałuży.",
            "Kurwa, %s, ty wredna małpo, weź spierdalaj.",
            "%s, ty sieroto pierdolona, do dupy z taką koleżanką.",
            "%s, ty zołzo jedna, mordę w kubeł, kurwa mać!",
        )

    private val ANYONE =
        listOf(
            "Ja pierdolę, %s, ogarnij się kurwa.",
            "Kurwa mać, %s, weź spierdalaj.",
            "%s? Szkoda kurwa gadać, ja pierdolę.",
        )

    val phrases: List<String> = listOf(VERB) + KNOWN + KNOWN.map { "darek $VERB $it" }

    fun nameIn(
        tokens: List<String>,
        likeness: Float,
    ): Name? {
        val start =
            tokens.indices.firstOrNull { i ->
                tokens[i] == "darek" && i + 2 < tokens.size && LooseMatch.alike(tokens[i + 1], VERB) >= likeness
            } ?: return null
        val word = tokens.drop(start + 2).lastOrNull { it != Grammar.UNKNOWN } ?: return null
        return vocative(word)
    }

    fun vocative(heard: String): Name {
        val word = heard.lowercase()
        return when {
            word in MASCULINE_IN_E -> Name(word.dropLast(1).capitalised() + "o", Gender.MALE)
            word.endsWith("ę") -> Name(feminine(word.dropLast(1)).capitalised(), Gender.FEMALE)
            word.endsWith("a") && word.length > 2 -> Name(masculine(word.dropLast(1)).capitalised(), Gender.MALE)
            else -> Name(word.capitalised(), Gender.UNKNOWN)
        }
    }

    private fun feminine(stem: String): String =
        when {
            listOf("si", "ni", "ci", "zi", "dzi").any(stem::endsWith) || stem.endsWith("l") -> stem + "u"
            else -> stem + "o"
        }

    private fun masculine(stem: String): String =
        when {
            TAKES_U.any(stem::endsWith) -> stem + "u"
            stem.endsWith("ł") -> stem.dropLast(1) + "le"
            stem.endsWith("st") -> stem.dropLast(2) + "ście"
            stem.endsWith("r") -> stem + "ze"
            stem.endsWith("t") -> stem.dropLast(1) + "cie"
            stem.endsWith("d") -> stem.dropLast(1) + "dzie"
            stem.endsWith("i") -> stem + "u"
            else -> stem + "ie"
        }

    private fun String.capitalised() = replaceFirstChar(Char::uppercase)

    private var last = ""

    fun insult(name: Name): String {
        val pool =
            when (name.gender) {
                Gender.MALE -> MALE + ANYONE
                Gender.FEMALE -> FEMALE + ANYONE
                Gender.UNKNOWN -> ANYONE
            }
        val pick = pool.filter { it != last }.random(Random)
        last = pick
        return pick.format(name.vocative)
    }
}
