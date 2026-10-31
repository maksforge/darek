package app.darek.tip

import java.time.Duration
import java.time.Instant

/** How long the app has to have been installed before the tip is mentioned. */
val TIP_ASK_AFTER: Duration = Duration.ofDays(30)

/**
 * Whether the home screen should mention the tip: **once per install, a month in** (ADR-0007).
 *
 * The month is the whole measure, because Darek keeps no record of use to count.
 *
 * [answeredUnder] is the `firstInstallTime` the ask was last answered under, judged at the read:
 * Auto Backup carries the answer to the user's next phone, where the install time differs, so it
 * does not count there and the new install is asked once, a month in.
 */
fun tipAskDue(
    answeredUnder: Instant?,
    installedAt: Instant,
    now: Instant,
): Boolean = answeredUnder != installedAt && Duration.between(installedAt, now) >= TIP_ASK_AFTER
