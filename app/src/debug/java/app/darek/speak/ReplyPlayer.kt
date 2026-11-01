package app.darek.speak

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** How a played Reply ended, which phase 1's R3 reads from the `DarekVoice` log line. */
enum class ReplyEnd { MARKER, WATCHDOG }

/** Plays one rendered Reply through a static [AudioTrack] and reports what ended it. */
object ReplyPlayer {
    suspend fun play(samples: ShortArray): ReplyEnd {
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
            val watchdogMs = samples.size * 1000L / Voice.SAMPLE_RATE + 2_000
            return if (withTimeoutOrNull(watchdogMs) { ended.await() } != null) ReplyEnd.MARKER else ReplyEnd.WATCHDOG
        } finally {
            track.release()
        }
    }
}
