# Phase 0 — Darek opens on his own screen, in Polish, with nothing of the template left

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). This phase
amends [ADR-0001](adr/0001-the-database-stops-being-disposable-at-1-0.md),
[ADR-0002](adr/0002-the-media-pipeline-is-kind-aware-and-file-first.md),
[ADR-0003](adr/0003-two-scheduling-mechanisms.md) and
[ADR-0005](adr/0005-backup-is-automatic-for-the-database-and-manual-for-media.md) as their modules
go, and builds [ADR-0012](adr/0012-darek-speaks-polish-and-opens-in-polish.md)'s first-launch switch.
`CONTEXT.md` is owed nothing: no domain term appears on screen yet.

**`PLAN.md`'s phase 0 line was wrong, and this PR corrects it.** It promised the Commands' storage
and the mark; both move out of this phase (below).

**What closing this phase means.** Today the app is the template: an Items list with photos, a
database with its schema gate, reminders on WorkManager, a manual backup export, a slate-blue scheme.
After this phase it opens — in Polish, on any phone — on one Darek home screen that holds the place of
the Listening switch, with Settings, Help and the Licences behind it, in Darek's own palette. The
difference a device can be asked about: the installed build declares none of the reminders'
permissions, has no `darek.db`, and a first launch on an English phone shows Polish.

---

## 0. What this phase inherits

- Phase P's pipeline: secrets, the `main` ruleset, Pages, the Play entry `com.maksforge.darek`.
  Nothing has been uploaded, so nothing is frozen beyond the package name.
- The tip stays (the owner's default for MaksForge apps); its one ask moves to the home screen.
  ADR-0007's ten items go with the Items and its 30 days stay, so `tipAskDue` becomes "installed at
  least 30 days ago and not asked yet".
- `minSdk` 26 stays. The test phone is API 30, the owner's is API 36.

## What is in, and what is deliberately not

**In:** deleting, per [`optional-modules.md`](optional-modules.md), the Room database and schema gate,
the photo pipeline, the manual backup export and the custom `BackupAgent` (platform Auto Backup takes
over), reminders and WorkManager, and the Items domain; a home screen that holds the Listening
switch's place and carries the tip's ask; Darek's palette; the one-time Polish switch on first launch;
`aab-permissions.py`, `edge-to-edge.py`'s SCENES, the privacy policy and `play-app-content.md`
rewritten against what is left. Around the code: CI's instrumented job stays green on one kept
`ManifestWiringTest` case, rewritten to assert the stripped permissions are absent and that
`INTERNET` is absent too (ADR-0008), which stays true after phase 2; `CLAUDE.md`'s
Stack, Layout and Commands rows are updated; `device-gate.py` keeps its autostart half and loses
the reminder checks.

**Not in, and each with a reason rather than a phase:**

- **The Commands' storage.** A JSON list under one DataStore key is the shape (house rules), but its
  fields are settled by phase 1's reading and phase 3's editor. Writing it now writes it twice.
- **Vosk, the model, `RECORD_AUDIO`, `CALL_PHONE`.** Phase 1. A permission merged before the
  feature that uses it is a Play question with no answer.
- **The mark.** The owner's to choose; the placeholder ships until then.

**Kept although nothing uses them yet:** `work/Autostart.kt`, `work/BatteryExemption.kt`,
`work/NotificationPermission.kt` and the `com.miui.securitycenter` `<queries>` entry. Phase 2's
Listening service needs all four (ADR-0010), and deleting them now means rewriting them in a month.
`NotificationPermission.kt` loses its reminder half: `openChannelNotificationSettings(ReminderChannel)`
and the KDoc's references to it and `channelCanAppear`. The permissions are not kept with the code:
after the strip the manifest declares none of `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`,
`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, and phase 2 re-adds what it uses.

## Auto Backup and live state

With the custom agent gone, every DataStore key reaches the user's next phone. Phase 0's keys are
the theme, a setting, and two one-time markers, both judged at the read (`optional-modules.md`):
each stores the `PackageInfo.firstInstallTime` it was written under and counts only if that equals
the current one.

- **The Polish marker.** The switch runs only when the marker does not match **and**
  `LocaleManagerCompat.getApplicationLocales(context)` is empty (the platform store on 33+,
  AppCompat's file below), so a restored explicit choice, English or Polish, is never overwritten.
  The marker is written whether or not the app switched, and only once an Activity exists, because
  below 33 AppCompat stores the locale only when one attaches. The one accepted edge: a user who
  chose "follow the phone" and moves phones gets Polish once on the new one.
- **`TIP_ASKED`.** A restored answer from another install does not count, so after a phone move the
  ask comes again 30 days later, as ADR-0007 intends.

`BACKUP_FOLDER_URI` leaves with the backup module; `ONBOARDING_DONE` stays only if something still
reads it. Listening state, which arrives in phase 2, is live state and is judged at the read the
same way.

---

## Checkpoints

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | Strip: reminders and WorkManager, Room, photos, backup export, Items | `refactor:` | — |
| **B** | The home screen, with the tip's ask | `feat:` | A |
| **C** | Darek's palette | `feat:` | — |
| **D** | Opens in Polish on first launch (ADR-0012) | `feat:` | — |
| **E** | Readings on both phones | `docs:` | A–D |

A goes first because B replaces the screen A removes. C and D touch nothing A does and can merge in
any order.

## E. The readings

Readings 1 and 2 on the test phone, from a plain debuggable build (`./gradlew installDebug`), after
a fresh install and one launch:

1. `adb shell pm path com.maksforge.darek.debug` prints a path (the package is installed), then
   the permission sections of `adb shell dumpsys package com.maksforge.darek.debug` — no
   `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`,
   `SCHEDULE_EXACT_ALARM`, `INTERNET`. Read the sections whole: `grep permission` is case-sensitive
   and drops `com.android.vending.BILLING` and `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
2. `adb shell run-as com.maksforge.darek.debug ls databases` — no `darek.db`. Not on
   `-PreleaseShapedDebug`: it is not debuggable, so `run-as` fails and lists nothing.
3. On both phones, with the language set to English: a fresh install opens in Polish; switching to
   English in Settings, force-stopping and reopening keeps English. The test phone (API 30) reads
   AppCompat's storage; the owner's phone `132bc856` (API 36) reads `LocaleManager`, and is announced
   in chat before the reading takes its screen (`CLAUDE.md`).

### Taken 2026-10-31

All from `./gradlew assembleDebug` at `bdaebbc` (versionCode 34, versionName 0.2.0), installed with
`adb install -t` after `adb uninstall com.maksforge.darek.debug`, and launched once with
`adb shell am start -W -n com.maksforge.darek.debug/app.darek.MainActivity` (the activity's class
keeps the `app.darek` namespace; only the `applicationId` is `com.maksforge.darek`).

**1. Permissions — pass.** Test phone `mnbm7hw8ivdqzx9l`, API 30:

```text
$ adb shell pm path com.maksforge.darek.debug
package:/data/app/~~Evrn_CR7AWB5zn-Q6ktCkQ==/com.maksforge.darek.debug-R1knJcU9noC0mzXx5S7xvA==/base.apk
$ adb shell dumpsys package com.maksforge.darek.debug | sed -n '/declared permissions:/,/runtime permissions:/p'
    declared permissions:
      com.maksforge.darek.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION: prot=signature, INSTALLED
    requested permissions:
      com.android.vending.BILLING
      com.maksforge.darek.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
      android.permission.ACCESS_NETWORK_STATE
    install permissions:
      com.maksforge.darek.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION: granted=true
      com.android.vending.BILLING: granted=true
      android.permission.ACCESS_NETWORK_STATE: granted=true
    ...
      runtime permissions:
$ adb shell dumpsys package com.maksforge.darek.debug | grep -cE "FOREGROUND_SERVICE|POST_NOTIFICATIONS|WAKE_LOCK|RECEIVE_BOOT_COMPLETED|SCHEDULE_EXACT_ALARM|android.permission.INTERNET"
0
```

The three requested are the three `play-app-content.md` accounts for; none of the six asked about.

**2. No database of the app's own — pass.** Test phone, same install:

```text
$ adb shell run-as com.maksforge.darek.debug ls databases
com.google.android.datatransport.events
com.google.android.datatransport.events-journal
```

No `darek.db`. The two files are Play Billing's `datatransport` usage log, the one
`backup_rules.xml` and `data_extraction_rules.xml` keep out of Auto Backup; it exists from the first
launch, before any purchase. The rest of the sandbox is `files/datastore/app_preferences.preferences_pb`
and AppCompat's locale file.

**3. API 36 — not taken.** The owner's phone, adb serial `192.168.0.28:46073` (Wi-Fi), agreed in chat
for one install, launch, locale read and uninstall:

```text
$ adb shell getprop ro.build.version.sdk
36
$ adb shell getprop persist.sys.locale
en-US
$ adb shell settings get system system_locales
en-US,pl-PL
$ adb install -t app/build/outputs/apk/debug/app-debug.apk
adb: failed to install app/build/outputs/apk/debug/app-debug.apk: Failure [INSTALL_FAILED_USER_RESTRICTED: Install canceled by user]
$ adb shell pm path com.maksforge.darek.debug   # exit 1, nothing left behind
```

HyperOS refusing the first install of a new package, as `CLAUDE.md`'s environment notes say. The
reading stopped there: the workarounds (USB installation in developer options, or a file-manager
tap) are the owner's to make. **The phone is English first**, so once the build is on it, a Polish
first screen and `adb shell cmd locale get-app-locales com.maksforge.darek.debug` printing `pl` prove
the override of an English phone through `LocaleManager`, not only the write.

**3, API 30 half — pass.** Test phone. MIUI's *Languages* screen
(`adb shell am start -a android.settings.LOCALE_SETTINGS`) is a single-choice list, not AOSP's
ordered one, so the phone was switched by tapping *English (United States)* and restored by tapping
*Polski*:

```text
$ adb shell getprop persist.sys.locale        # after choosing English
en-US
$ adb uninstall com.maksforge.darek.debug && adb install -t app/build/outputs/apk/debug/app-debug.apk
$ adb shell run-as com.maksforge.darek.debug ls files     # before the launch
ls: files: No such file or directory
$ adb shell am start -W -n com.maksforge.darek.debug/app.darek.MainActivity
$ adb shell run-as com.maksforge.darek.debug cat files/androidx.appcompat.app.AppCompatDelegate.application_locales_record_file
<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><locales application_locales="pl" />
```

The first screen read *Tu Darek będzie słuchał.*, the Polish `home_listening_placeholder` (English:
*Darek will listen here.*). Then, in the app's Settings, *Język → English*, a force-stop and a
relaunch:

```text
$ adb shell am force-stop com.maksforge.darek.debug
$ adb shell am start -W -n com.maksforge.darek.debug/app.darek.MainActivity
$ adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml | grep -oE 'text="[^"]+"'
text="Darek will listen here."
text="Darek debug"
$ adb shell run-as com.maksforge.darek.debug cat files/androidx.appcompat.app.AppCompatDelegate.application_locales_record_file
<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><locales application_locales="en" />
```

The user's English survived the restart: the first-launch marker did not switch it back.

**Restored afterwards:** the test phone's language is Polish again (`persist.sys.locale` and
`system_locales` both `pl-PL`), `stay_on_while_plugged_in` still `2`, no keyguard showing, MIUI
optimisation still `false`; the debug build is reinstalled clean and not launched. Nothing on the
owner's phone changed.

**3, API 36 — taken later the same day, failed, fixed, passed.** The owner allowed the install on
the phone. The first launch of current main opened in English and the platform held no app language:

```text
$ adb shell cmd locale get-app-locales com.maksforge.darek.debug   # after the first launch
Locales for com.maksforge.darek.debug for user 0 are []
```

while the marker was written. From 13 `AppCompatDelegate.setApplicationLocales` reaches
`LocaleManager` only through an open Activity's delegate, so called from `MainApplication.onCreate` it
did nothing; below 13 it only records the request, which is why the API 30 reading passed. Calling
`LocaleManager` directly from the Application set `[pl]` but left the first screen English until the
next launch. The fix keeps the decision in `MainApplication` and, from 13, makes the switch in
`MainActivity.onCreate` through AppCompat, which applies it to that screen. The test phone (API 30)
re-read after the fix: AppCompat's file still holds `application_locales="pl"` on a fresh launch.

**3, API 36, both halves — pass.** Owner's phone, from `./gradlew assembleDebug` at `2a94688`
(versionCode 40, versionName 0.2.0), the phone still English first (`persist.sys.locale` `en-US`).
The build went on as an update, and `pm clear` stood in for the uninstall + install: the rule
switches when the stored marker differs from `firstInstallTime`, and a clear deletes the marker and
the platform's app language alike, which is the state a fresh install starts in, while an uninstall
would make the next install a first install again, which HyperOS refused above without the owner's
tap.

```text
$ adb shell dumpsys package com.maksforge.darek.debug | grep firstInstallTime   # before
      firstInstallTime=2026-10-31 22:50:58
$ adb install -r -t app/build/outputs/apk/debug/app-debug.apk
Success
$ adb shell pm clear com.maksforge.darek.debug
Success
$ adb shell cmd locale get-app-locales com.maksforge.darek.debug
Locales for com.maksforge.darek.debug for user 0 are []
$ adb shell am start -W -n com.maksforge.darek.debug/app.darek.MainActivity
LaunchState: COLD
$ adb shell cmd locale get-app-locales com.maksforge.darek.debug
Locales for com.maksforge.darek.debug for user 0 are [pl]
$ adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml | grep -oE '(text|content-desc)="[^"]+"'
text="Tu Darek będzie słuchał."
text="Darek debug"
content-desc="Ustawienia"
```

Then, in the app's Settings, *Język → English* (an `input tap` on the option `uiautomator dump`
located), a force-stop and a relaunch:

```text
$ adb shell cmd locale get-app-locales com.maksforge.darek.debug   # straight after the tap
Locales for com.maksforge.darek.debug for user 0 are [en]
$ adb shell am force-stop com.maksforge.darek.debug
$ adb shell pidof com.maksforge.darek.debug   # prints nothing: no process left
$ adb shell am start -W -n com.maksforge.darek.debug/app.darek.MainActivity
LaunchState: COLD
$ adb shell cmd locale get-app-locales com.maksforge.darek.debug
Locales for com.maksforge.darek.debug for user 0 are [en]
$ adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml | grep -oE '(text|content-desc)="[^"]+"'
text="Darek will listen here."
text="Darek debug"
content-desc="Settings"
```

English survived the restart. Then *Language → System*, the same force-stop and relaunch:

```text
$ adb shell cmd locale get-app-locales com.maksforge.darek.debug   # straight after the tap
Locales for com.maksforge.darek.debug for user 0 are []
$ adb shell am force-stop com.maksforge.darek.debug
$ adb shell pidof com.maksforge.darek.debug   # prints nothing: no process left
$ adb shell am start -W -n com.maksforge.darek.debug/app.darek.MainActivity
LaunchState: COLD
$ adb shell cmd locale get-app-locales com.maksforge.darek.debug
Locales for com.maksforge.darek.debug for user 0 are []
$ adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml | grep -oE '(text|content-desc)="[^"]+"'
text="Darek will listen here."
text="Darek debug"
content-desc="Settings"
```

Following the phone stayed so: the marker did not switch it back to Polish.

**Left on the owner's phone:** the debug build at `2a94688` (versionCode 40), installed since the
first install of the earlier reading. Its data is the DataStore file
`files/datastore/app_preferences.preferences_pb`, whose one key is this install's first-launch
marker `language_judged_under`, and the platform's app language, set back to Polish through the app's Settings (`get-app-locales` `[pl]`) so
the owner sees Darek as designed. `/sdcard/ui.xml` was deleted and the phone left on its home screen;
no phone setting was changed.

**Verdict:** all readings pass. Phase 0 closes.

## Amendments owed

ADR-0001, 0002, 0003 and 0005 each get a dated amendment in checkpoint A's PR, saying the module is
gone from Darek and what replaced it. The `DOD.md` standing schema gate is marked *parked* with its
five points kept verbatim. ADR-0007 gets one in checkpoint B's PR: the ask's item count leaves with
the Items domain, and it is due 30 days after install.
