package app.darek.speak

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** Plays one rendered Reply through a static [AudioTrack] and reports what ended it. */
object ReplyPlayer {
    suspend fun play(
        samples: ShortArray,
        volume: Float = 1f,
    ): PlaybackEnd {
        // One frame would put the end marker at 0, which may never fire.
        if (samples.size < 2) return PlaybackEnd.NOTHING_TO_PLAY
        return try {
            playTrack(samples, volume)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PlaybackEnd.FAILED
        }
    }

    private suspend fun playTrack(
        samples: ShortArray,
        volume: Float,
    ): PlaybackEnd {
        val track =
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                ).setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setSampleRate(Voice.SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
        val ended = CompletableDeferred<Unit>()
        try {
            track.write(samples, 0, samples.size)
            track.setVolume(volume)
            // A marker at exactly the frame count never fires on some devices: the head stops on the last frame.
            track.notificationMarkerPosition = samples.size - 1
            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: AudioTrack) {
                        ended.complete(Unit)
                    }

                    override fun onPeriodicNotification(track: AudioTrack) = Unit
                },
                Handler(Looper.getMainLooper()),
            )
            track.play()
            val started = SystemClock.elapsedRealtime()
            val marked = withTimeoutOrNull(ReplyEnd.watchdogMs(samples.size)) { ended.await() } != null
            return ReplyEnd.of(samples.size, started, SystemClock.elapsedRealtime(), marked, failed = false)
                ?: PlaybackEnd.WATCHDOG
        } finally {
            track.release()
        }
    }
}
