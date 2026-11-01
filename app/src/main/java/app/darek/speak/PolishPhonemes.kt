package app.darek.speak

import java.math.BigInteger
import java.text.Normalizer

/**
 * Polish text to the phonemes Darek's voice was trained on (ADR-0014), without eSpeak.
 *
 * A port of `scripts/voice/g2p_reference.py`; `PolishPhonemesTest` holds the two together.
 */
object PolishPhonemes {
    private const val NASAL = "̃"
    private const val SOFT = "ʲ"
    private const val RZ = "Ʒ"
    private const val DZ = "ʣ"

    private val vowels = setOf("a", "ɛ", "i", "ɔ", "u", "ɨ", "ɔ$NASAL", "ɛ$NASAL")
    private val voicedToVoiceless =
        linkedMapOf(
            "b" to "p",
            "d" to "t",
            "ɡ" to "k",
            "z" to "s",
            "ʒ" to "ʃ",
            RZ to "ʃ",
            "ʑ" to "ɕ",
            "dz" to "ts",
            DZ to "ts",
            "dʒ" to "tʃ",
            "dʑ" to "tɕ",
            "v" to "f",
            "ɣ" to "x",
        )
    private val voicelessToVoiced =
        voicedToVoiceless.entries
            .filter { it.key != RZ && it.key != DZ }
            .associate { it.value to it.key }
    private val obstruents = voicedToVoiceless.keys + voicelessToVoiced.keys
    private val softBeforeVowelI = setOf("b", "ɡ")
    private val clitics =
        setOf(
            "się",
            "nie",
            "mi",
            "ci",
            "go",
            "mu",
            "ją",
            "je",
            "by",
            "że",
            "i",
            "a",
            "o",
            "u",
            "w",
            "z",
            "do",
            "na",
            "za",
            "po",
            "od",
            "ze",
            "we",
            "przy",
            "przez",
            "dla",
            "bez",
            "nad",
            "pod",
            "przed",
            "ku",
        )
    private val plain =
        mapOf(
            'a' to "a",
            'e' to "ɛ",
            'i' to "i",
            'o' to "ɔ",
            'u' to "u",
            'ó' to "u",
            'y' to "ɨ",
            'b' to "b",
            'c' to "ts",
            'ć' to "tɕ",
            'd' to "d",
            'f' to "f",
            'g' to "ɡ",
            'h' to "x",
            'j' to "j",
            'k' to "k",
            'l' to "l",
            'ł' to "w",
            'm' to "m",
            'n' to "n",
            'ń' to "ɲ",
            'p' to "p",
            'r' to "r",
            's' to "s",
            'ś' to "ɕ",
            't' to "t",
            'w' to "v",
            'z' to "z",
            'ź' to "ʑ",
            'ż' to "ʒ",
            'x' to "ks",
            'q' to "k",
            'v' to "v",
            'ą' to "ą",
            'ę' to "ę",
        )

    // dz is spoken as dʑ because eSpeak writes it so, and the voice learned eSpeak's phonemes.
    private val digraphs =
        mapOf("ch" to "x", "cz" to "tʃ", "sz" to "ʃ", "rz" to RZ, "dż" to "dʒ", "dź" to "dʑ", "dz" to DZ)

    private val softBeforeI = mapOf("dz" to "dʑ", "c" to "tɕ", "s" to "ɕ", "z" to "ʑ", "n" to "ɲ")
    private val letterVowels = "aeiouóyąę".toSet()

    private val ones = listOf("zero", "jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć")
    private val teens =
        listOf(
            "dziesięć",
            "jedenaście",
            "dwanaście",
            "trzynaście",
            "czternaście",
            "piętnaście",
            "szesnaście",
            "siedemnaście",
            "osiemnaście",
            "dziewiętnaście",
        )
    private val tens =
        listOf(
            "",
            "",
            "dwadzieścia",
            "trzydzieści",
            "czterdzieści",
            "pięćdziesiąt",
            "sześćdziesiąt",
            "siedemdziesiąt",
            "osiemdziesiąt",
            "dziewięćdziesiąt",
        )
    private val hundreds =
        listOf(
            "",
            "sto",
            "dwieście",
            "trzysta",
            "czterysta",
            "pięćset",
            "sześćset",
            "siedemset",
            "osiemset",
            "dziewięćset",
        )

    private val hours =
        listOf(
            "zero",
            "pierwsza",
            "druga",
            "trzecia",
            "czwarta",
            "piąta",
            "szósta",
            "siódma",
            "ósma",
            "dziewiąta",
            "dziesiąta",
            "jedenasta",
            "dwunasta",
            "trzynasta",
            "czternasta",
            "piętnasta",
            "szesnasta",
            "siedemnasta",
            "osiemnasta",
            "dziewiętnasta",
            "dwudziesta",
            "dwudziesta pierwsza",
            "dwudziesta druga",
            "dwudziesta trzecia",
            "dwudziesta czwarta",
        )

    private val temperatureRange = Regex("(?<=\\d)\\s*[\\-–−]\\s*(?=\\d+(?:[.,]\\d+)?\\s*°)")
    private val temperature =
        Regex("((?<![\\p{L}\\p{N}_])[+\\-−])?(\\d+)(?:[.,](\\d+))?\\s*°\\s*([cf](?![a-ząćęłńóśźż]))?")
    private val clockTime = Regex("(?<![\\p{L}\\p{N}_.,:])([01]?\\d|2[0-4]):([0-5]\\d)(?![\\p{L}\\p{N}_]|[.,:]\\d)")
    private val minusSign = Regex("(?<![\\p{L}\\p{N}_])[\\-−](?=\\d)")
    private val plusSign = Regex("(?<![\\p{L}\\p{N}_])\\+(?=\\d)")
    private val digitRun = Regex("\\d+")
    private val token = Regex("[a-ząćęłńóśźż]+|[,.?!;:]")

    fun phonemize(text: String): String {
        var expanded = temperatureRange.replace(text.lowercase(), ", ")
        expanded = temperature.replace(expanded) { " " + temperatureWords(it) + " " }
        expanded = clockTime.replace(expanded) { " " + timeWords(it) + " " }
        expanded = minusSign.replace(expanded, " minus ")
        expanded = plusSign.replace(expanded, " plus ")
        expanded = digitRun.replace(expanded) { " " + spokenNumber(it.value) + " " }
        val tokens = token.findAll(expanded).map { it.value }.toList()
        val words = tokens.filter { it[0].isLetter() }.map { it to wordSegments(it) }
        val out = mutableListOf<String>()
        var w = 0
        for (t in tokens) {
            if (!t[0].isLetter()) {
                if (out.isNotEmpty()) out[out.lastIndex] += t
                continue
            }
            val (word, segs) = words[w]
            val following = words.getOrNull(w + 1)?.second
            if ((word == "w" || word == "z") && following != null) {
                val first = following.firstOrNull { it != SOFT } ?: ""
                val spoken = if (first in voicelessToVoiced) listOf(voicedToVoiceless.getValue(segs[0])) else segs
                out += spoken.joinToString("")
            } else {
                out += stress(assimilate(segs), word)
            }
            w++
        }
        return out.joinToString(" ").replace(RZ, "ʒ").replace(DZ, "dʑ")
    }

    /** Piper's input: `^`, then each codepoint's id followed by the pad, then `$`. Unknown codepoints are dropped. */
    fun ids(
        phonemes: String,
        idMap: Map<String, List<Long>>,
    ): LongArray {
        val pad = idMap.getValue("_")
        val ids = mutableListOf<Long>()
        ids += idMap.getValue("^")
        ids += pad
        val decomposed = Normalizer.normalize(phonemes, Normalizer.Form.NFD)
        var i = 0
        while (i < decomposed.length) {
            val codepoint = decomposed.codePointAt(i)
            val key = String(Character.toChars(codepoint))
            idMap[key]?.let {
                ids += it
                ids += pad
            }
            i += Character.charCount(codepoint)
        }
        ids += idMap.getValue("$")
        return ids.toLongArray()
    }

    private fun temperatureWords(match: MatchResult): String {
        val (sign, wholeText, fraction, scale) = match.destructured
        val whole = wholeText.toBigInteger()
        val hasFraction = fraction.any { it != '0' }
        val words = mutableListOf<String>()
        if ((sign == "-" || sign == "−") && (whole.signum() != 0 || hasFraction)) {
            words += "minus"
        } else if (sign == "+") {
            words += "plus"
        }
        if (fraction == "5" && whole <= BigInteger.ONE) {
            words += if (whole.signum() == 0) "pół" else "półtora"
        } else {
            words += spokenNumber(wholeText)
            if (hasFraction) {
                words += if (fraction == "5") listOf("i pół") else listOf("przecinek", fractionWords(fraction))
            }
        }
        val lastTwo = whole.mod(BigInteger.valueOf(100)).toInt()
        words +=
            when {
                hasFraction -> "stopnia"
                whole == BigInteger.ONE -> "stopień"
                lastTwo % 10 in 2..4 && lastTwo !in 12..14 -> "stopnie"
                else -> "stopni"
            }
        when (scale) {
            "c" -> words += "celsjusza"
            "f" -> words += "fahrenheita"
        }
        return words.joinToString(" ")
    }

    private fun fractionWords(digits: String): String {
        val significant = digits.trimStart('0')
        return (List(digits.length - significant.length) { "zero" } + spokenNumber(significant)).joinToString(" ")
    }

    private fun timeWords(time: MatchResult): String {
        val hour = time.groupValues[1].toInt()
        val minute = time.groupValues[2].toLong()
        val words = mutableListOf(hours[hour])
        if (minute in 1..9) {
            words += listOf("zero", numberWords(minute))
        } else if (minute != 0L || hour == 0) {
            words += numberWords(minute)
        }
        return words.joinToString(" ")
    }

    private fun spokenNumber(digits: String): String =
        digits.toLongOrNull()?.takeIf { it < 1_000_000_000 }?.let(::numberWords)
            ?: digits.map { ones[it.digitToInt()] }.joinToString(" ")

    internal fun numberWords(value: Long): String {
        var n = value
        if (n < 10) return ones[n.toInt()]
        val words = mutableListOf<String>()
        if (n >= 1000) {
            val k = n / 1000
            if (k == 1L) {
                words += "tysiąc"
            } else {
                words += numberWords(k)
                words += if (k % 10 in 2..4 && k % 100 !in 12..14) "tysiące" else "tysięcy"
            }
            n %= 1000
        }
        if (n >= 100) {
            words += hundreds[(n / 100).toInt()]
            n %= 100
        }
        if (n in 10..19) {
            words += teens[(n - 10).toInt()]
        } else {
            if (n >= 20) words += tens[(n / 10).toInt()]
            if (n % 10 != 0L) words += ones[(n % 10).toInt()]
        }
        return words.joinToString(" ")
    }

    private fun wordSegments(word: String) = resolveNasals(lettersToSegments(word))

    private fun lettersToSegments(word: String): List<String> {
        val segs = mutableListOf<String>()
        var i = 0

        fun next(k: Int): Char? = word.getOrNull(i + k)
        while (i < word.length) {
            val soft = softBeforeI.entries.firstOrNull { word.startsWith(it.key + "i", i) }
            if (soft != null) {
                segs += soft.value
                if (next(soft.key.length + 1) in letterVowels) {
                    if (soft.value == "ɲ") segs += SOFT
                } else {
                    segs += "i"
                }
                i += soft.key.length + 1
                continue
            }
            val digraph = digraphs[word.substring(i, minOf(i + 2, word.length))]
            if (digraph != null) {
                segs += digraph
                i += 2
                continue
            }
            val ch = word[i]
            val last = segs.lastOrNull()
            if (ch == 'i' && last != null && last !in vowels && next(1) in letterVowels) {
                segs += if (last == "b" || last !in setOf("p", "f", "v", "m", "k", "ɡ")) "j" else SOFT
                i++
                continue
            }
            if (ch == 'i' && last in softBeforeVowelI) segs += SOFT
            plain[ch]?.let { segs += it }
            i++
        }
        return segs
    }

    private fun resolveNasals(segs: List<String>): List<String> {
        val out = mutableListOf<String>()
        segs.forEachIndexed { k, s ->
            if (s != "ą" && s != "ę") {
                out += s
                return@forEachIndexed
            }
            val vowel = if (s == "ą") "ɔ" else "ɛ"
            when (segs.drop(k + 1).firstOrNull { it != SOFT }) {
                "p", "b" -> out += listOf(vowel, "m")
                "t", "d", "ts", "dz", DZ, "tʃ", "dʒ" -> out += listOf(vowel, "n")
                "k", "ɡ" -> out += listOf(vowel, "ŋ")
                "tɕ", "dʑ" -> out += listOf(vowel, "ɲ")
                "l", "w" -> out += vowel
                null -> out += if (s == "ę") "ɛ" else vowel + NASAL
                else -> out += vowel + NASAL
            }
        }
        return out
    }

    private fun assimilate(input: List<String>): List<String> {
        val segs = input.toMutableList()
        val idx = segs.indices.filter { segs[it] != SOFT }
        for (k in idx.reversed()) {
            val s = segs[k]
            if (s in voicedToVoiceless) {
                segs[k] = voicedToVoiceless.getValue(s)
            } else if (s !in obstruents) {
                break
            }
        }
        for (p in idx.size - 2 downTo 0) {
            val a = idx[p]
            val b = idx[p + 1]
            val x = segs[a]
            val y = segs[b]
            val transparent = y == "v" || y == RZ
            if (x in obstruents && y in obstruents && !transparent) {
                if (y in voicedToVoiceless && x in voicelessToVoiced) {
                    segs[a] = voicelessToVoiced.getValue(x)
                } else if (y in voicelessToVoiced && x in voicedToVoiceless) {
                    segs[a] = voicedToVoiceless.getValue(x)
                }
            }
        }
        for (p in 0 until idx.size - 1) {
            val a = idx[p]
            val b = idx[p + 1]
            val y = segs[b]
            if ((y == "v" || y == RZ) && segs[a] in voicelessToVoiced) {
                segs[b] = voicedToVoiceless.getValue(y)
            }
        }
        return segs
    }

    private fun stress(
        segs: List<String>,
        word: String,
    ): String {
        val vowelAt = segs.indices.filter { segs[it] in vowels || segs[it].endsWith(NASAL) }
        if (vowelAt.isEmpty() || (vowelAt.size == 1 && word in clitics)) return segs.joinToString("")
        val marks = mutableMapOf(vowelAt.getOrElse(vowelAt.size - 2) { vowelAt[0] } to "ˈ")
        if (vowelAt.size >= 4) marks[vowelAt[0]] = "ˌ"
        return segs.indices.joinToString("") { (marks[it] ?: "") + segs[it] }
    }
}
