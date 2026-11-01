package app.darek.speak

/** How a played Reply ended, which the `DarekVoice` log line names. */
enum class PlaybackEnd { MARKER, WATCHDOG, FAILED, NOTHING_TO_PLAY }

/**
 * When a Reply is finished (ADR-0011): the first of its last frame, a failure, or a watchdog at the
 * clip's length plus [GRACE_MS], so a stalled track cannot keep recognition paused.
 */
object ReplyEnd {
    const val GRACE_MS = 2_000L

    fun watchdogMs(frames: Int): Long = frames * 1_000L / Voice.SAMPLE_RATE + GRACE_MS

    fun of(
        frames: Int,
        startedAtMs: Long?,
        nowMs: Long,
        markerReached: Boolean,
        failed: Boolean,
    ): PlaybackEnd? =
        when {
            failed -> PlaybackEnd.FAILED
            markerReached -> PlaybackEnd.MARKER
            startedAtMs != null && nowMs - startedAtMs >= watchdogMs(frames) -> PlaybackEnd.WATCHDOG
            else -> null
        }
}
