package app.darek.listen

import android.content.Context
import androidx.core.content.edit

/**
 * Debug-only: lets a reply-only Command fire when each of its words is only *alike* what was heard,
 * letter by letter. „darek" stays exact, or a Marek would fire it; Commands that call never come
 * here (ADR-0009).
 */
object LooseMatch {
    private const val FILE = "debug_probe"
    private const val KEY = "likeness"
    private const val NAME = "darek"

    fun likeness(context: Context): Float = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getFloat(KEY, 0.7f)

    fun setLikeness(
        context: Context,
        likeness: Float,
    ) {
        context
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit { putFloat(KEY, likeness) }
    }

    fun matches(
        tokens: List<String>,
        words: List<String>,
        likeness: Float,
    ): Boolean =
        tokens.windowed(words.size).any { window ->
            window.zip(words).all { (heard, word) -> heard == word || (word != NAME && alike(heard, word) >= likeness) }
        }

    fun alike(
        a: String,
        b: String,
    ): Float {
        val longest = maxOf(a.length, b.length)
        return if (longest == 0) 1f else 1f - distance(a, b).toFloat() / longest
    }

    private fun distance(
        a: String,
        b: String,
    ): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, substitution)
            }
            previous = current
        }
        return previous[b.length]
    }
}
