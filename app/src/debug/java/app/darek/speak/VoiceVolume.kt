package app.darek.speak

import android.content.Context

/** The debug build's volume for Darek's voice, 0–1 of the normalised Reply. Checkpoint B moves it to `AppPreferences`. */
object VoiceVolume {
    private const val FILE = "debug_voice"
    private const val KEY = "volume"

    fun get(context: Context): Float = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getFloat(KEY, 1f)

    fun set(
        context: Context,
        volume: Float,
    ) {
        context
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY, volume)
            .apply()
    }
}
