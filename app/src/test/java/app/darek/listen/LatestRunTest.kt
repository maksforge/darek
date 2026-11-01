package app.darek.listen

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

/** Listening's start, Persona switch and stop, which can arrive from Main and Default at once. */
@OptIn(ExperimentalCoroutinesApi::class)
class LatestRunTest {
    private val events = mutableListOf<String>()
    private var running = 0
    private var mostAtOnce = 0

    // Each run takes a second to finish cancelling, as a voice load or a synthesis does.
    private fun TestScope.latestRun(endsOnItsOwn: Boolean = false) =
        LatestRun<String>(
            backgroundScope,
            { value ->
                running++
                mostAtOnce = maxOf(mostAtOnce, running)
                events += "start $value"
                try {
                    if (!endsOnItsOwn) awaitCancellation()
                } finally {
                    withContext(NonCancellable) { delay(1_000) }
                    running--
                    events += "end $value"
                }
            },
        ) { events += "ended" }

    // advanceUntilIdle leaves backgroundScope's work alone, and the collector lives there.
    private fun TestScope.settle() {
        advanceTimeBy(60_000)
        runCurrent()
    }

    @Test
    fun `switching back while the old run is finishing ends on the value asked for last`() =
        runTest {
            val runs = latestRun()
            runs.run("darek")
            runCurrent()
            runs.change { "daria" }
            runCurrent()
            runs.change { "darek" }
            settle()
            assertEquals(listOf("start darek", "end darek", "ended", "start darek"), events)
            assertEquals(1, mostAtOnce)
        }

    @Test
    fun `a stop while the old run is finishing leaves nothing running`() =
        runTest {
            val runs = latestRun()
            runs.run("darek")
            runCurrent()
            runs.change { "daria" }
            runCurrent()
            runs.stop()
            settle()
            assertEquals(listOf("start darek", "end darek", "ended"), events)
            assertEquals(0, running)
        }

    @Test
    fun `a change while nothing runs, or a run stopped before it began, starts nothing`() =
        runTest {
            val runs = latestRun()
            runs.change { "daria" }
            runs.run("darek")
            runs.stop()
            runs.change { "daria" }
            settle()
            assertEquals(emptyList<String>(), events)
        }

    @Test
    fun `asking for the running value again keeps its run`() =
        runTest {
            val runs = latestRun()
            runs.run("darek")
            runCurrent()
            runs.run("darek")
            runs.change { it }
            settle()
            assertEquals(listOf("start darek"), events)
        }

    @Test
    fun `a run that ended on its own starts again for the same value`() =
        runTest {
            val runs = latestRun(endsOnItsOwn = true)
            runs.run("darek")
            settle()
            runs.run("darek")
            settle()
            assertEquals(List(2) { listOf("start darek", "end darek", "ended") }.flatten(), events)
        }
}
