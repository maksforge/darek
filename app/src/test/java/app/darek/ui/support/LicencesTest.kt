package app.darek.ui.support

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parsing Licensee's report, which is the half worth testing — the screen that draws the result is
 * a list. Everything here runs on the JVM because `Licences.kt` deliberately names no Android type.
 */
class LicencesTest {
    @Test
    fun `artifacts are grouped by licence, not by artifact`() {
        val report =
            """
            [
              {"groupId":"a","artifactId":"one","version":"1.0",
               "spdxLicenses":[{"identifier":"Apache-2.0","name":"Apache License 2.0"}]},
              {"groupId":"b","artifactId":"two","version":"2.0",
               "spdxLicenses":[{"identifier":"Apache-2.0","name":"Apache License 2.0"}]}
            ]
            """.trimIndent()
        val groups = groupLicences(report)
        assertEquals(1, groups.size)
        assertEquals(2, groups.single().artifacts.size)
    }

    @Test
    fun `a dual-licensed artifact appears under both, because the obligation is per licence`() {
        val report =
            """
            [
              {"groupId":"a","artifactId":"one","version":"1.0",
               "spdxLicenses":[
                 {"identifier":"Apache-2.0","name":"Apache License 2.0"},
                 {"identifier":"MIT","name":"MIT License"}]}
            ]
            """.trimIndent()
        assertEquals(2, groupLicences(report).size)
    }

    @Test
    fun `a plugin bump that adds an unknown field must not blank the screen`() {
        val report =
            """[{"groupId":"a","artifactId":"one","version":"1.0","somethingNew":true,
                 "spdxLicenses":[{"identifier":"Apache-2.0","name":"Apache License 2.0"}]}]"""
        assertTrue(groupLicences(report).isNotEmpty())
    }

    @Test
    fun `malformed input throws, and the ViewModel is where that is caught`() {
        // Deliberately *not* swallowed here. This function's job is to parse; a report it cannot
        // parse means the build's generator is broken, and returning an empty list would hide that
        // behind an empty screen. `LicencesViewModel` wraps the call so a user never sees a crash,
        // which is the right place for the decision — one caller, one policy.
        assertThrows(Exception::class.java) { groupLicences("not json") }
    }

    @Test
    fun `the bundled models join their licences, each voice under each of its three`() {
        val report =
            """
            [{"groupId":"a","artifactId":"one","version":"1.0",
              "spdxLicenses":[{"identifier":"MIT","name":"MIT License"}]}]
            """.trimIndent()
        val groups = groupLicences(report, BUNDLED_MODELS)
        listOf("pl_PL-mc_speech-medium", "pl_PL-gosia-medium").forEach { voice ->
            val voiceIn = groups.filter { group -> group.artifacts.any { it.artifactId == voice } }
            assertEquals(voice, listOf("CC0-1.0", "MIT", null), voiceIn.map { it.spdxId })
        }
        assertEquals(3, groups.single { it.spdxId == "MIT" }.artifacts.size)
        val apache = groups.single { it.spdxId == "Apache-2.0" }
        assertTrue(apache.artifacts.any { it.artifactId == "vosk-model-small-pl" })
    }

    @Test
    fun `each bundled model lists its source and SHA-256 under its coordinates`() {
        BUNDLED_MODELS.forEach { model ->
            assertTrue(model.listing.startsWith("${model.coordinates}\nhttps://"))
            assertTrue(Regex("SHA-256 [0-9a-f]{64}$").containsMatchIn(model.listing))
        }
    }

    @Test
    fun `an artifact from the report has no source and lists only its coordinates`() {
        val report =
            """
            [{"groupId":"a","artifactId":"one","version":"1.0",
              "spdxLicenses":[{"identifier":"MIT","name":"MIT License"}]}]
            """.trimIndent()
        assertEquals(
            "a:one:1.0",
            groupLicences(report)
                .single()
                .artifacts
                .single()
                .listing,
        )
    }
}
