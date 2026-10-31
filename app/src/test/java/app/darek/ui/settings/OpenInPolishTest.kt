package app.darek.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class OpenInPolishTest {
    private val installedAt = Instant.parse("2026-10-31T08:00:00Z")
    private val otherInstall = installedAt.minus(Duration.ofDays(400))

    @Test
    fun `a fresh install opens in Polish`() {
        assertTrue(opensInPolish(openedUnder = null, installedAt = installedAt, appLanguageSet = false))
    }

    @Test
    fun `not again once switched on this install`() {
        assertFalse(opensInPolish(openedUnder = installedAt, installedAt = installedAt, appLanguageSet = true))
    }

    @Test
    fun `a marker restored from another install with no language set switches`() {
        assertTrue(opensInPolish(openedUnder = otherInstall, installedAt = installedAt, appLanguageSet = false))
    }

    @Test
    fun `a marker restored with a language set leaves that language`() {
        assertFalse(opensInPolish(openedUnder = otherInstall, installedAt = installedAt, appLanguageSet = true))
    }

    @Test
    fun `follow the phone chosen on this install sticks`() {
        assertFalse(opensInPolish(openedUnder = installedAt, installedAt = installedAt, appLanguageSet = false))
    }
}
