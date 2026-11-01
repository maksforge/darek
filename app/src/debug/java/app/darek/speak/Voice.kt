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
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Darek's own voice (ADR-0014): Polish text through [PolishPhonemes] into Piper's mc_speech model on
 * ONNX Runtime, 16-bit mono samples at [SAMPLE_RATE] out. Debug-only until phase 1's checkpoint B.
 */
class Voice private constructor(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    private val idMap: Map<String, List<Long>>,
    private val scales: FloatArray,
) : AutoCloseable {
    fun synthesize(text: String): ShortArray {
        val ids = PolishPhonemes.ids(PolishPhonemes.phonemize(text), idMap)
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
                val samples = (result[0] as OnnxTensor).floatBuffer
                return ShortArray(
                    samples.remaining(),
                ) { (samples.get().coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
            }
        } finally {
            inputs.values.forEach(OnnxTensor::close)
        }
    }

    override fun close() = session.close()

    companion object {
        const val SAMPLE_RATE = 22_050
        const val MODEL = "voice/voice.onnx"
        const val FULL_MODEL = "voice/voice-full.onnx"
        private const val CONFIG = "voice/voice.onnx.json"

        fun hasAsset(
            context: Context,
            path: String,
        ): Boolean = runCatching { context.assets.open(path).close() }.isSuccess

        fun load(
            context: Context,
            model: String = MODEL,
        ): Voice {
            val config =
                Json
                    .parseToJsonElement(
                        context.assets.open(CONFIG).use { it.readBytes().decodeToString() },
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
            val bytes = context.assets.open(model).use { it.readBytes() }
            return Voice(environment, environment.createSession(bytes, OrtSession.SessionOptions()), idMap, scales)
        }
    }
}
