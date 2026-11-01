package app.darek.listen

import android.content.Context

/** The debug build's extra answers on Home: the roast prototype, which the release never carries. */
object ListeningExtras {
    fun phrases(): List<String> = Roast.phrases

    fun reply(
        context: Context,
        tokens: List<String>,
    ): String? = Roast.nameIn(tokens, LooseMatch.likeness(context))?.let(Roast::insult)
}
