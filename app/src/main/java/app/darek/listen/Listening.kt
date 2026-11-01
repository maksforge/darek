package app.darek.listen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import app.darek.speak.PlaybackEnd
import app.darek.speak.ReplyPlayer
import app.darek.speak.Voice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model

/**
 * Listening (`CONTEXT.md`): the microphone open and the enabled Commands able to fire. Held by
 * `AppContainer`, so the model and the voice stay loaded between starts; in phase 1 it runs only
 * while Home is on screen, and phase 2 moves it into the service.
 *
 * Each Command ends the recording before Darek speaks and starts a new one when his Reply is
 * finished, so he never hears himself (ADR-0011).
 */
class Listening(
    context: Context,
    private val scope: CoroutineScope,
    private val volume: () -> Float,
) {
    enum class Phase { OFF, PREPARING, ON }

    data class State(
        val phase: Phase = Phase.OFF,
        val lastReply: String? = null,
    )

    private val appContext = context.applicationContext
    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState.asStateFlow()

    @Volatile private var model: Model? = null

    @Volatile private var replies: Map<Command, ShortArray>? = null

    private var job: Job? = null

    fun start(enabled: Set<Command>) {
        if (job != null || enabled.isEmpty()) return
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        job =
            scope.launch {
                mutableState.update { it.copy(phase = Phase.PREPARING) }
                try {
                    val loaded = withContext(Dispatchers.IO) { model ?: loadModel().also { model = it } }
                    val rendered =
                        withContext(Dispatchers.Default) {
                            replies
                                ?: renderReplies().also { if (it.isNotEmpty()) replies = it }
                        }
                    val grammar = Grammar.json(enabled)
                    mutableState.update { it.copy(phase = Phase.ON) }
                    while (true) {
                        val command =
                            Recogniser(loaded)
                                .heard(grammar)
                                .filterIsInstance<Heard.Final>()
                                .mapNotNull { commandIn(it.tokens, enabled) }
                                .first()
                        Log.i(TAG, "command=${command.name}")
                        mutableState.update { it.copy(lastReply = command.reply) }
                        val end = rendered[command]?.let { ReplyPlayer.play(it, volume()) } ?: PlaybackEnd.FAILED
                        Log.i(VOICE_TAG, "reply command=${command.name} end=$end")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "listening failed", e)
                } finally {
                    mutableState.update { it.copy(phase = Phase.OFF) }
                    job = null
                }
            }
    }

    fun stop() {
        job?.cancel()
    }

    private fun loadModel(): Model {
        val started = SystemClock.elapsedRealtime()
        val directory = RecogniserModel.unpack(appContext)
        val unpacked = SystemClock.elapsedRealtime()
        return Model(directory.path).also {
            Log.i(MODEL_TAG, "unpack=${unpacked - started}ms load=${SystemClock.elapsedRealtime() - unpacked}ms")
        }
    }

    // A voice that fails to load leaves the Replies shown but silent; Listening goes on.
    private fun renderReplies(): Map<Command, ShortArray> =
        runCatching {
            val started = SystemClock.elapsedRealtime()
            Voice.load(appContext).use { voice -> Command.entries.associateWith { voice.synthesize(it.reply) } }.also {
                Log.i(VOICE_TAG, "ready=${SystemClock.elapsedRealtime() - started}ms")
            }
        }.getOrElse {
            Log.e(VOICE_TAG, "voice failed to load", it)
            emptyMap()
        }

    private companion object {
        const val TAG = "DarekListen"
        const val MODEL_TAG = "DarekModel"
        const val VOICE_TAG = "DarekVoice"
    }
}
