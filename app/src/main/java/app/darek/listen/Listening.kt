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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
        /** A partial result holds the Persona's name: it shows it is hearing before the Command is finished. */
        val hearingName: Boolean = false,
    )

    private val appContext = context.applicationContext
    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState.asStateFlow()

    @Volatile private var model: Model? = null

    @Volatile private var voice: Pair<Persona, Voice>? = null

    private val rendered = ConcurrentHashMap<Pair<Persona, String>, ShortArray>()

    @Volatile private var lastNotUnderstood: Line? = null

    @Volatile private var job: Job? = null

    @Volatile private var jobPersona: Persona? = null

    private data class Answer(
        val label: String,
        val text: String,
    )

    /** Started again under another Persona, a running Listening restarts (ADR-0016). */
    fun start(
        enabled: Set<Command>,
        persona: Persona,
    ) {
        val running = job
        if (running != null) {
            if (jobPersona != persona) {
                scope.launch {
                    running.cancelAndJoin()
                    start(enabled, persona)
                }
            }
            return
        }
        if (enabled.isEmpty()) return
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
                    withContext(Dispatchers.Default) { prepareVoice(persona) }
                    if (enabled.any { it.calls }) {
                        withContext(Dispatchers.Default) { renderAhead(persona, NOT_UNDERSTOOD.take(1)) }
                        launch(Dispatchers.Default) { renderAhead(persona, NOT_UNDERSTOOD.drop(1)) }
                    }
                    val grammar = Grammar.json(enabled, persona, ListeningExtras.phrases(persona))
                    mutableState.update { it.copy(phase = Phase.ON) }
                    while (true) {
                        val answer =
                            Recogniser(loaded)
                                .heard(grammar)
                                .onEach { heard ->
                                    val hearing = heard is Heard.Partial && persona.word in heard.tokens
                                    mutableState.update { it.copy(hearingName = hearing) }
                                }.filterIsInstance<Heard.Final>()
                                .mapNotNull { answerTo(it.tokens, enabled, persona) }
                                .first()
                        Log.i(TAG, "reaction=${answer.label}")
                        mutableState.update { it.copy(hearingName = false, lastReply = answer.text) }
                        Log.i(VOICE_TAG, "reply reaction=${answer.label} end=${speak(persona, answer.text)}")
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
        jobPersona = persona
        launched.start()
    }

    fun stop() {
        job?.cancel()
    }

    private fun answerTo(
        tokens: List<String>,
        enabled: Set<Command>,
        persona: Persona,
    ): Answer? =
        when (val reaction = reactionTo(tokens, enabled, persona)) {
            null -> null
            is Reaction.Fires -> Answer(reaction.command.name, reaction.command.reply.by(persona))
            Reaction.Corrected -> Answer("CORRECTED", persona.correction)
            Reaction.NotUnderstood, Reaction.NotConfigured ->
                ListeningExtras.reply(appContext, tokens, persona)?.let { Answer("EXTRA", it) }
                    ?: if (reaction == Reaction.NotUnderstood) {
                        Answer("NOT_UNDERSTOOD", pickNotUnderstood(persona))
                    } else {
                        Answer("NOT_CONFIGURED", NOT_CONFIGURED.by(persona))
                    }
        }

    private fun pickNotUnderstood(persona: Persona): String {
        val ready =
            NOT_UNDERSTOOD
                .filter { rendered.containsKey(persona to it.by(persona)) }
                .ifEmpty { NOT_UNDERSTOOD.take(1) }
        return ready
            .filter { it != lastNotUnderstood }
            .ifEmpty { ready }
            .random()
            .also { lastNotUnderstood = it }
            .by(persona)
    }

    private suspend fun speak(
        persona: Persona,
        text: String,
    ): PlaybackEnd {
        val samples =
            rendered[persona to text]
                ?: voice
                    ?.takeIf { it.first == persona }
                    ?.let { (_, loaded) -> withContext(Dispatchers.Default) { synthesized(loaded, text) } }
                ?: return PlaybackEnd.FAILED
        return ReplyPlayer.play(samples, volume())
    }

    // A voice that fails to load or to synthesize leaves the Replies shown but silent; Listening
    // goes on, and the next start tries again. Only the chosen Persona's voice is held (ADR-0016).
    private suspend fun prepareVoice(persona: Persona) {
        if (voice?.first == persona) return
        voice?.second?.close()
        voice = null
        val started = SystemClock.elapsedRealtime()
        voice =
            runCatching { Voice.load(appContext, persona.voice, persona.voiceScales) }
                .onFailure { Log.e(VOICE_TAG, "voice failed to load", it) }
                .getOrNull()
                ?.let { persona to it } ?: return
        renderAhead(persona, Command.entries.map { it.reply } + NOT_CONFIGURED + Line(persona.correction))
        Log.i(VOICE_TAG, "persona=$persona ready=${SystemClock.elapsedRealtime() - started}ms")
    }

    private suspend fun renderAhead(
        persona: Persona,
        lines: List<Line>,
    ) {
        val (owner, loaded) = voice ?: return
        if (owner != persona) return
        for (line in lines) {
            currentCoroutineContext().ensureActive()
            val key = persona to line.by(persona)
            if (rendered.containsKey(key)) continue
            synthesized(loaded, key.second)?.let { rendered[key] = it }
        }
    }

    private fun synthesized(
        loaded: Voice,
        text: String,
    ): ShortArray? =
        runCatching { loaded.synthesize(text) }
            .onFailure { Log.e(VOICE_TAG, "synthesis failed", it) }
            .getOrNull()

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
