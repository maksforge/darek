package app.darek.listen

import android.content.Context

/** The release half of the debug build's extra answers: none. */
object ListeningExtras {
    fun phrases(): List<String> = emptyList()

    @Suppress("UNUSED_PARAMETER")
    fun reply(
        context: Context,
        tokens: List<String>,
    ): String? = null
}
