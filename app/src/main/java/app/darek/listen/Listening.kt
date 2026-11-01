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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.util.concurrent.ConcurrentHashMap

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
        val failed: Boolean = false,
        /** A partial result holds his name: he shows he is hearing before the Command is finished. */
        val hearingName: Boolean = false,
    )

    private val appContext = context.applicationContext
    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState.asStateFlow()

    @Volatile private var model: Model? = null

    @Volatile private var voice: Voice? = null

    private val rendered = ConcurrentHashMap<String, ShortArray>()

    @Volatile private var lastNotUnderstood: String? = null

    @Volatile private var job: Job? = null

    private data class Answer(
        val label: String,
        val text: String,
    )

    fun start(enabled: Set<Command>) {
        if (job != null || enabled.isEmpty()) return
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val launched =
            scope.launch(start = CoroutineStart.LAZY) {
                mutableState.update { it.copy(phase = Phase.PREPARING, failed = false) }
                try {
                    val loaded = withContext(Dispatchers.IO) { model ?: loadModel().also { model = it } }
                    withContext(Dispatchers.Default) { prepareVoice() }
                    launch(Dispatchers.Default) { renderAhead(NOT_UNDERSTOOD) }
                    val grammar = Grammar.json(enabled, ListeningExtras.phrases())
                    mutableState.update { it.copy(phase = Phase.ON) }
                    while (true) {
                        val answer =
                            Recogniser(loaded)
                                .heard(grammar)
                                .onEach { heard ->
                                    val hearing = heard is Heard.Partial && NAME in heard.tokens
                                    mutableState.update { it.copy(hearingName = hearing) }
                                }.filterIsInstance<Heard.Final>()
                                .mapNotNull { answerTo(it.tokens, enabled) }
                                .first()
                        Log.i(TAG, "reaction=${answer.label}")
                        mutableState.update { it.copy(hearingName = false, lastReply = answer.text) }
                        Log.i(VOICE_TAG, "reply reaction=${answer.label} end=${speak(answer.text)}")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "listening failed", e)
                    mutableState.update { it.copy(failed = true) }
                } finally {
                    mutableState.update { it.copy(phase = Phase.OFF, hearingName = false) }
                    if (job === currentCoroutineContext()[Job]) job = null
                }
            }
        job = launched
        launched.start()
    }

    fun stop() {
        job?.cancel()
    }

    private fun answerTo(
        tokens: List<String>,
        enabled: Set<Command>,
    ): Answer? =
        when (val reaction = reactionTo(tokens, enabled)) {
            null -> null
            is Reaction.Fires -> Answer(reaction.command.name, reaction.command.reply)
            Reaction.NotUnderstood ->
                ListeningExtras.reply(appContext, tokens)?.let { Answer("EXTRA", it) }
                    ?: Answer("NOT_UNDERSTOOD", pickNotUnderstood())
        }

    private fun pickNotUnderstood(): String {
        val ready = NOT_UNDERSTOOD.filter(rendered::containsKey).ifEmpty { NOT_UNDERSTOOD.take(1) }
        return ready
            .filter { it != lastNotUnderstood }
            .ifEmpty { ready }
            .random()
            .also { lastNotUnderstood = it }
    }

    private suspend fun speak(text: String): PlaybackEnd {
        val samples =
            rendered[text]
                ?: voice?.let { loaded -> withContext(Dispatchers.Default) { loaded.synthesize(text) } }
                ?: return PlaybackEnd.FAILED
        return ReplyPlayer.play(samples, volume())
    }

    // A voice that fails to load leaves the Replies shown but silent; Listening goes on, and the
    // next start tries again.
    private fun prepareVoice() {
        if (voice != null) return
        val started = SystemClock.elapsedRealtime()
        voice =
            runCatching { Voice.load(appContext) }
                .onFailure { Log.e(VOICE_TAG, "voice failed to load", it) }
                .getOrNull() ?: return
        renderAhead(Command.entries.map { it.reply } + NOT_UNDERSTOOD.first())
        Log.i(VOICE_TAG, "ready=${SystemClock.elapsedRealtime() - started}ms")
    }

    private fun renderAhead(texts: List<String>) {
        val loaded = voice ?: return
        texts.filterNot(rendered::containsKey).forEach { rendered[it] = loaded.synthesize(it) }
    }

    private fun loadModel(): Model {
        val started = SystemClock.elapsedRealtime()
        val directory = RecogniserModel.unpack(appContext)
        val unpacked = SystemClock.elapsedRealtime()
        return Model(directory.path).also {
            Log.i(MODEL_TAG, "unpack=${unpacked - started}ms load=${SystemClock.elapsedRealtime() - unpacked}ms")
        }
    }

    private companion object {
        const val TAG = "DarekListen"
        const val MODEL_TAG = "DarekModel"
        const val VOICE_TAG = "DarekVoice"
    }
}
