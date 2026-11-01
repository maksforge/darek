package app.darek.speak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A Reply that never finishes leaves Darek showing he listens while he hears nothing. */
class ReplyEndTest {
    private val twelveSeconds = 12 * Voice.SAMPLE_RATE

    @Test
    fun the_last_frame_finishes_it() {
        assertEquals(PlaybackEnd.MARKER, ReplyEnd.of(twelveSeconds, 0, 3_000, markerReached = true, failed = false))
    }

    @Test
    fun a_failure_finishes_it_at_once() {
        assertEquals(PlaybackEnd.FAILED, ReplyEnd.of(twelveSeconds, 0, 0, markerReached = false, failed = true))
        assertEquals(PlaybackEnd.FAILED, ReplyEnd.of(twelveSeconds, null, 0, markerReached = false, failed = true))
    }

    @Test
    fun the_watchdog_finishes_it_at_the_clip_plus_two_seconds() {
        assertEquals(
            PlaybackEnd.WATCHDOG,
            ReplyEnd.of(twelveSeconds, 1_000, 15_000, markerReached = false, failed = false),
        )
        assertNull(ReplyEnd.of(twelveSeconds, 1_000, 14_999, markerReached = false, failed = false))
    }

    @Test
    fun a_twelve_second_clip_is_not_finished_at_ten() {
        assertNull(ReplyEnd.of(twelveSeconds, 0, 10_000, markerReached = false, failed = false))
    }

    @Test
    fun nothing_finishes_before_playback_starts() {
        assertNull(ReplyEnd.of(twelveSeconds, null, 60_000, markerReached = false, failed = false))
    }
}
