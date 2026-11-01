package app.darek.ui.probe

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.darek.listen.Command
import app.darek.listen.Grammar
import app.darek.listen.Heard
import app.darek.listen.Recogniser
import app.darek.listen.RecogniserModel
import app.darek.listen.commandIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model

enum class ProbePhase { IDLE, PREPARING, LISTENING }

data class ProbeLine(
    val tokens: String,
    val command: Command?,
)

data class ProbeState(
    val phase: ProbePhase = ProbePhase.IDLE,
    val free: Boolean = false,
    val partial: String = "",
    val finals: List<ProbeLine> = emptyList(),
    val error: String? = null,
)

/** Phase 1's R1/R2 apparatus: every final result goes to `adb logcat -s DarekProbe`. */
class ProbeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(ProbeState())
    val state: StateFlow<ProbeState> = mutableState.asStateFlow()

    @Volatile private var model: Model? = null
    private var job: Job? = null

    fun setFree(free: Boolean) {
        if (state.value.phase == ProbePhase.IDLE) mutableState.update { it.copy(free = free) }
    }

    fun start() {
        if (job != null) return
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        job =
            viewModelScope.launch {
                mutableState.update { it.copy(phase = ProbePhase.PREPARING, error = null) }
                try {
                    val loaded = withContext(Dispatchers.IO) { model ?: load().also { model = it } }
                    val free = state.value.free
                    val mode = if (free) "free" else "grammar"
                    mutableState.update { it.copy(phase = ProbePhase.LISTENING) }
                    Log.i(TAG, "start mode=$mode")
                    val grammar = if (free) null else Grammar.json(Command.entries.toSet())
                    Recogniser(loaded).heard(grammar).collect { heard ->
                        when (heard) {
                            is Heard.Partial ->
                                mutableState.update {
                                    it.copy(
                                        partial = heard.tokens.joinToString(" "),
                                    )
                                }
                            is Heard.Final -> onFinal(heard, mode)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "failed", e)
                    mutableState.update { it.copy(error = e.toString()) }
                } finally {
                    Log.i(TAG, "stop")
                    mutableState.update { it.copy(phase = ProbePhase.IDLE, partial = "") }
                    job = null
                }
            }
    }

    fun stop() {
        job?.cancel()
    }

    private fun onFinal(
        heard: Heard.Final,
        mode: String,
    ) {
        if (heard.tokens.isEmpty()) return
        val line = ProbeLine(heard.tokens.joinToString(" "), commandIn(heard.tokens, Command.entries))
        Log.i(TAG, "final mode=$mode command=${line.command?.name ?: "-"} tokens=\"${line.tokens}\"")
        mutableState.update { it.copy(partial = "", finals = (listOf(line) + it.finals).take(MAX_LINES)) }
    }

    private fun load(): Model {
        val started = SystemClock.elapsedRealtime()
        val directory = RecogniserModel.unpack(getApplication())
        val unpacked = SystemClock.elapsedRealtime()
        return Model(directory.path).also {
            Log.i(TAG, "model unpack=${unpacked - started}ms load=${SystemClock.elapsedRealtime() - unpacked}ms")
        }
    }

    // A cancelled job's blocking load still runs to the end, so the model may not exist yet.
    override fun onCleared() {
        job?.invokeOnCompletion { model?.close() } ?: model?.close()
    }

    private companion object {
        const val TAG = "DarekProbe"
        const val MAX_LINES = 100
    }
}
