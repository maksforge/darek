package app.darek.ui.settings

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.darek.R
import app.darek.listen.LooseMatch
import app.darek.speak.ReplyPlayer
import app.darek.speak.Voice
import app.darek.speak.VoiceVolume
import app.darek.theme.Spacing
import app.darek.ui.common.SectionHeader
import app.darek.ui.probe.ProbeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The developer-only section of Settings. **This file exists only in the debug source set** — see
 * the release half for why that is a source set and not a `BuildConfig.DEBUG` branch.
 *
 * *Say this* speaks any text in Darek's voice and logs what phase 1's R3 reads:
 * `adb logcat -s DarekVoice`. The probe is R1/R2's: `adb logcat -s DarekProbe`.
 */
@Composable
fun DebugSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasFullModel = remember { Voice.hasAsset(context, Voice.FULL_MODEL) }
    var sample by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf(samples[0]) }
    var fullModel by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(VoiceVolume.get(context)) }

    SectionHeader(stringResource(R.string.debug_voice))
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        status =
                            runCatching { DebugVoice.say(context, text, fullModel, volume) }.getOrElse { "failed: $it" }
                        busy = false
                    }
                },
            ) { Text(stringResource(R.string.debug_voice_say)) }
            Button(onClick = {
                sample = (sample + 1) % samples.size
                text = samples[sample]
            }) { Text(stringResource(R.string.debug_voice_next)) }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.debug_voice_text)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (hasFullModel) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = fullModel, onCheckedChange = { fullModel = it })
                Text(stringResource(R.string.debug_voice_full), modifier = Modifier.padding(start = Spacing.tight))
            }
        }
        Text(
            stringResource(R.string.debug_voice_volume, (volume * 100).toInt()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = volume,
            onValueChange = { volume = it },
            onValueChangeFinished = { VoiceVolume.set(context, volume) },
        )
        if (status.isNotEmpty()) {
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    SectionHeader(stringResource(R.string.debug_probe))
    Button(
        onClick = { context.startActivity(Intent(context, ProbeActivity::class.java)) },
        modifier = Modifier.padding(horizontal = Spacing.base),
    ) { Text(stringResource(R.string.debug_probe_open)) }
    var likeness by remember { mutableStateOf(LooseMatch.likeness(context)) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.base)) {
        Text(
            stringResource(R.string.debug_probe_likeness, (likeness * 100).roundToInt()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = likeness,
            onValueChange = { likeness = (it * 20).roundToInt() / 20f },
            onValueChangeFinished = { LooseMatch.setLikeness(context, likeness) },
            valueRange = 0.5f..1f,
        )
        Text(
            stringResource(R.string.debug_probe_likeness_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val samples =
    listOf(
        "Co tam, mordo?",
        "Otwieram bramę.",
        "Jest 21:30, a o 7:05 zadzwoni budzik.",
        "Na dworze jest -5°C, a w nocy będzie -13,2°C.",
        "Jutro +2°, w południe 0°C, wieczorem -0,5°C.",
        "Dziś 22°C, wczoraj 1°, w lutym -25°C, a woda ma 21,5 °C.",
        "Chrząszcz brzmi w trzcinie, a wąskie mosty drżą.",
    )

private object DebugVoice {
    private const val TAG = "DarekVoice"
    private val lock = Mutex()
    private var loaded: Pair<String, Voice>? = null

    suspend fun say(
        context: Context,
        text: String,
        fullModel: Boolean,
        volume: Float,
    ): String =
        lock.withLock {
            val model = if (fullModel) Voice.FULL_MODEL else Voice.MODEL
            val (voice, loadMs) = withContext(Dispatchers.Default) { voiceFor(context, model) }
            val started = SystemClock.elapsedRealtime()
            val samples = withContext(Dispatchers.Default) { voice.synthesize(text) }
            val synthMs = SystemClock.elapsedRealtime() - started
            val end = ReplyPlayer.play(samples, volume)
            val audioMs = samples.size * 1000L / Voice.SAMPLE_RATE
            "model=${model.substringAfterLast('/')} load=${loadMs}ms synth=${synthMs}ms audio=${audioMs}ms end=$end"
                .also { Log.i(TAG, "$it text=\"$text\"") }
        }

    private fun voiceFor(
        context: Context,
        model: String,
    ): Pair<Voice, Long> {
        loaded?.takeIf { it.first == model }?.let { return it.second to 0L }
        loaded?.second?.close()
        val started = SystemClock.elapsedRealtime()
        val voice = Voice.load(context, model)
        loaded = model to voice
        return voice to SystemClock.elapsedRealtime() - started
    }
}
