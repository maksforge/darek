package app.darek.speak

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs

/**
 * Darek's own voice (ADR-0014): Polish text through [PolishPhonemes] into Piper's mc_speech model on
 * ONNX Runtime, 16-bit mono samples at [SAMPLE_RATE] out.
 */
class Voice private constructor(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    private val idMap: Map<String, List<Long>>,
    private val scales: FloatArray,
) : AutoCloseable {
    fun synthesize(text: String): ShortArray {
        val phonemes = PolishPhonemes.phonemize(text)
        if (phonemes.isBlank()) return ShortArray(0)
        val ids = PolishPhonemes.ids(phonemes, idMap)
        val inputs =
            mapOf(
                "input" to
                    OnnxTensor.createTensor(
                        environment,
                        LongBuffer.wrap(ids),
                        longArrayOf(1, ids.size.toLong()),
                    ),
                "input_lengths" to
                    OnnxTensor.createTensor(
                        environment,
                        LongBuffer.wrap(longArrayOf(ids.size.toLong())),
                        longArrayOf(1),
                    ),
                "scales" to OnnxTensor.createTensor(environment, FloatBuffer.wrap(scales), longArrayOf(3)),
            )
        try {
            session.run(inputs).use { result ->
                val buffer = (result[0] as OnnxTensor).floatBuffer
                val samples = FloatArray(buffer.remaining()).also { buffer.get(it) }
                val gain = normalisingGain(samples)
                return ShortArray(
                    samples.size,
                ) {
                    (samples[it] * gain)
                        .coerceIn(-1f, 1f)
                        .times(Short.MAX_VALUE)
                        .toInt()
                        .toShort()
                }
            }
        } finally {
            inputs.values.forEach(OnnxTensor::close)
        }
    }

    override fun close() = session.close()

    // The model speaks at about a third of full scale, which a phone speaker leaves too quiet.
    private fun normalisingGain(samples: FloatArray): Float {
        val peak = samples.maxOfOrNull { abs(it) } ?: return 1f
        return if (peak > 0f) minOf(PEAK / peak, MAX_GAIN) else 1f
    }

    companion object {
        const val SAMPLE_RATE = 22_050
        private const val PEAK = 0.95f
        private const val MAX_GAIN = 8f

        /** [voice] is an asset path without its extension: the Persona's (ADR-0016). */
        fun load(
            context: Context,
            voice: String,
        ): Voice {
            val config =
                Json
                    .parseToJsonElement(
                        context.assets.open("$voice.onnx.json").use { it.readBytes().decodeToString() },
                    ).jsonObject
            val idMap =
                config.getValue("phoneme_id_map").jsonObject.mapValues { (_, ids) ->
                    ids.jsonArray.map { it.jsonPrimitive.long }
                }
            val inference = config.getValue("inference").jsonObject
            val scales =
                floatArrayOf(
                    inference.getValue("noise_scale").jsonPrimitive.float,
                    inference.getValue("length_scale").jsonPrimitive.float,
                    inference.getValue("noise_w").jsonPrimitive.float,
                )
            val environment = OrtEnvironment.getEnvironment()
            val session =
                OrtSession.SessionOptions().use {
                    environment.createSession(
                        mapAsset(context, "$voice.onnx"),
                        it,
                    )
                }
            return Voice(environment, session, idMap, scales)
        }

        // openFd needs the asset stored uncompressed: `noCompress` in app/build.gradle.kts.
        private fun mapAsset(
            context: Context,
            path: String,
        ): ByteBuffer =
            context.assets.openFd(path).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use {
                    it.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
    }
}
