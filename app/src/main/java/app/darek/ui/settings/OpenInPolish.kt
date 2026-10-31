package app.darek.ui.settings

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import app.darek.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Whether to switch the app to Polish at this launch (ADR-0012): once per install, and never over a
 * language already set, so a restored explicit choice survives a phone move. The marker is judged
 * at the read, as `docs/phase-0.md`, *Auto Backup and live state*, describes.
 */
fun opensInPolish(
    openedUnder: Instant?,
    installedAt: Instant,
    appLanguageSet: Boolean,
): Boolean = openedUnder != installedAt && !appLanguageSet

suspend fun Context.opensInPolish(
    preferences: AppPreferences,
    installedAt: Instant,
): Boolean =
    withContext(Dispatchers.IO) {
        opensInPolish(
            openedUnder = preferences.openedInPolishUnder.first(),
            installedAt = installedAt,
            appLanguageSet = !LocaleManagerCompat.getApplicationLocales(this@opensInPolish).isEmpty,
        )
    }
