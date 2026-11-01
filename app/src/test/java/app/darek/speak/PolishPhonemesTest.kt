package app.darek.speak

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolishPhonemesTest {
    private data class Row(
        val text: String,
        val phonemes: String,
        val ids: List<Long>?,
    )

    private val rows =
        resource("polish-phonemes.tsv")
            .lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val cells = line.split('\t')
                Row(cells[0], cells[1], cells.getOrNull(2)?.split(' ')?.map(String::toLong))
            }.toList()

    private val idMap: Map<String, List<Long>> =
        Json
            .parseToJsonElement(resource("voice.onnx.json"))
            .jsonObject
            .getValue("phoneme_id_map")
            .jsonObject
            .mapValues { (_, ids) -> ids.jsonArray.map { it.jsonPrimitive.long } }

    @Test
    fun `every word and Reply is phonemised as the host reference does`() {
        val wrong = rows.filter { PolishPhonemes.phonemize(it.text) != it.phonemes }
        assertTrue(
            "${wrong.size} of ${rows.size} differ, e.g. " +
                wrong.take(5).joinToString { "${it.text}: ${PolishPhonemes.phonemize(it.text)} ≠ ${it.phonemes}" },
            wrong.isEmpty(),
        )
    }

    @Test
    fun `the Replies reach the model as the host reference's ids, pads and nasal tildes included`() {
        val replies = rows.filter { it.ids != null }
        assertTrue(replies.any { "ą" in it.text })
        replies.forEach { assertEquals(it.text, it.ids, PolishPhonemes.ids(it.phonemes, idMap).toList()) }
    }

    @Test
    fun `numbers are read as words`() {
        assertEquals("dwadzieścia jeden", PolishPhonemes.numberWords(21))
        assertEquals("dwa tysiące dwadzieścia sześć", PolishPhonemes.numberWords(2026))
    }

    @Test
    fun `a clock time is read the formal way, with the hour as a feminine ordinal`() {
        same("21:30", "dwudziesta pierwsza trzydzieści")
        same("7:05", "siódma zero pięć")
        same("12:00", "dwunasta")
        same("00:00", "zero zero")
    }

    @Test
    fun `only a colon makes a clock time, never a date, a price or a version`() {
        same("12.05.2026", "dwanaście. pięć. dwa tysiące dwadzieścia sześć")
        same("3.50 zł", "trzy. pięćdziesiąt zł")
        same("wersja 1.15", "wersja jeden. piętnaście")
        same("ż12:30", "ż dwanaście: trzydzieści")
    }

    @Test
    fun `temperatures take their sign, their scale and the right form of stopień`() {
        same("-5°C", "minus pięć stopni celsjusza")
        same("+1°", "plus jeden stopień")
        same("22°C", "dwadzieścia dwa stopnie celsjusza")
        same("12°", "dwanaście stopni")
        same("0,5°", "pół stopnia")
        same("-0,5°C", "minus pół stopnia celsjusza")
        same("1,5°", "półtora stopnia")
        same("21,5°", "dwadzieścia jeden i pół stopnia")
        same("1,05°", "jeden przecinek zero pięć stopnia")
        same("-13,2°C", "minus trzynaście przecinek dwa stopnia celsjusza")
        same("0°C", "zero stopni celsjusza")
        same("Temperatura -3", "temperatura minus trzy")
    }

    @Test
    fun `a temperature range reads as two numbers, not a minus`() {
        same("od 15-20°C", "od piętnaście, dwadzieścia stopni celsjusza")
        same("15 - 20°", "piętnaście, dwadzieścia stopni")
    }

    @Test
    fun `a number too long for a Long is read digit by digit`() {
        same("10000000000000000000°", "jeden " + "zero ".repeat(19) + "stopni")
        same("1,00000000000000000001°", "jeden przecinek " + "zero ".repeat(19) + "jeden stopnia")
    }

    private fun same(
        written: String,
        spoken: String,
    ) = assertEquals(written, PolishPhonemes.phonemize(spoken), PolishPhonemes.phonemize(written))

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)) { "missing test resource $name" }.readText()
}
