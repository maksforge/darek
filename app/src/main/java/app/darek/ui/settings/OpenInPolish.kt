package app.darek.ui.settings

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import app.darek.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * What this launch does about the switch to Polish (ADR-0012). Each install's first launch is judged
 * once, switched or not, and never switches over a language already set, so a restored explicit
 * choice survives a phone move. The marker is judged at the read, as `docs/phase-0.md`, *Auto Backup
 * and live state*, describes.
 */
data class LaunchLanguage(
    val switchToPolish: Boolean,
    val markJudged: Boolean,
)

fun launchLanguage(
    judgedUnder: Instant?,
    installedAt: Instant,
    appLanguageSet: Boolean,
): LaunchLanguage {
    val firstLaunch = judgedUnder != installedAt
    return LaunchLanguage(switchToPolish = firstLaunch && !appLanguageSet, markJudged = firstLaunch)
}

suspend fun Context.launchLanguage(
    preferences: AppPreferences,
    installedAt: Instant,
): LaunchLanguage =
    withContext(Dispatchers.IO) {
        launchLanguage(
            judgedUnder = preferences.languageJudgedUnder.first(),
            installedAt = installedAt,
            appLanguageSet = !LocaleManagerCompat.getApplicationLocales(this@launchLanguage).isEmpty,
        )
    }
