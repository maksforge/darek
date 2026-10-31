package app.darek.tip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TipAskTest {
    private val installedAt = Instant.parse("2026-09-01T08:00:00Z")
    private val monthLater = installedAt + TIP_ASK_AFTER

    private fun due(
        answeredUnder: Instant? = null,
        now: Instant = monthLater,
    ) = tipAskDue(answeredUnder, installedAt, now)

    @Test
    fun `due once installed a month`() {
        assertTrue(due())
    }

    @Test
    fun `not due a millisecond short of the month`() {
        assertFalse(due(now = monthLater.minusMillis(1)))
    }

    @Test
    fun `never again once answered on this install`() {
        assertFalse(due(answeredUnder = installedAt, now = monthLater.plus(TIP_ASK_AFTER)))
    }

    @Test
    fun `an answer restored from another install does not count`() {
        assertTrue(due(answeredUnder = installedAt.minus(TIP_ASK_AFTER)))
    }
}
