package app.darek.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class OpenInPolishTest {
    private val installedAt = Instant.parse("2026-10-31T08:00:00Z")
    private val otherInstall = installedAt.minus(Duration.ofDays(400))

    private val switch = LaunchLanguage(switchToPolish = true, markJudged = true)
    private val keep = LaunchLanguage(switchToPolish = false, markJudged = true)
    private val alreadyJudged = LaunchLanguage(switchToPolish = false, markJudged = false)

    @Test
    fun `a fresh install opens in Polish`() {
        assertEquals(switch, launchLanguage(judgedUnder = null, installedAt = installedAt, appLanguageSet = false))
    }

    @Test
    fun `not again once judged on this install`() {
        assertEquals(
            alreadyJudged,
            launchLanguage(judgedUnder = installedAt, installedAt = installedAt, appLanguageSet = true),
        )
    }

    @Test
    fun `a marker restored from another install with no language set switches`() {
        assertEquals(
            switch,
            launchLanguage(judgedUnder = otherInstall, installedAt = installedAt, appLanguageSet = false),
        )
    }

    @Test
    fun `a fresh install with a language already set keeps it and is still judged`() {
        assertEquals(keep, launchLanguage(judgedUnder = null, installedAt = installedAt, appLanguageSet = true))
    }

    @Test
    fun `follow the phone chosen on this install sticks`() {
        assertEquals(
            alreadyJudged,
            launchLanguage(judgedUnder = installedAt, installedAt = installedAt, appLanguageSet = false),
        )
    }

    @Test
    fun `a restored language keeps it, and a later follow the phone sticks`() {
        val restored = launchLanguage(judgedUnder = otherInstall, installedAt = installedAt, appLanguageSet = true)
        assertEquals(keep, restored)

        val judgedUnder = if (restored.markJudged) installedAt else otherInstall
        val afterFollowThePhone =
            launchLanguage(judgedUnder = judgedUnder, installedAt = installedAt, appLanguageSet = false)
        assertEquals(alreadyJudged, afterFollowThePhone)
    }
}
