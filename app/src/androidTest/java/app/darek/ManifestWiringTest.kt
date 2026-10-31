package app.darek

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What Android reads off the installed manifest, which no source file shows whole: a dependency can
 * merge a permission, and a receiver the OS starts on its own is referenced by nothing in Kotlin. Add
 * a case here for every component the OS starts by itself.
 */
@RunWith(AndroidJUnit4::class)
class ManifestWiringTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun no_network_and_none_of_the_stripped_permissions() {
        val requested =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                .orEmpty()
                .toSet()
        assertEquals(emptySet<String>(), requested intersect NOT_REQUESTED)
    }

    private companion object {
        // INTERNET never (ADR-0008); the rest is what the template's reminders and WorkManager
        // declared or merged. Phase 2 brings POST_NOTIFICATIONS and FOREGROUND_SERVICE back with the
        // Listening service, and this list and aab-permissions.py move with them.
        val NOT_REQUESTED =
            setOf(
                "android.permission.INTERNET",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.SCHEDULE_EXACT_ALARM",
                "android.permission.WAKE_LOCK",
                "android.permission.FOREGROUND_SERVICE",
            )
    }
}
