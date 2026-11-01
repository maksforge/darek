package app.darek.listen

import android.content.Context

/** The debug build's extra answers on Home: the roast prototype, which the release never carries. */
object ListeningExtras {
    fun phrases(persona: Persona): List<String> = Roast.phrases(persona)

    fun reply(
        context: Context,
        tokens: List<String>,
        persona: Persona,
    ): String? = Roast.nameIn(tokens, LooseMatch.likeness(context), persona)?.let(Roast::insult)
}
