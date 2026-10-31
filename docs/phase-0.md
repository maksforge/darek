# Phase 0 — Darek opens on his own screen, in Polish, with nothing of the template left

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). This phase
amends [ADR-0001](adr/0001-the-database-stops-being-disposable-at-1-0.md),
[ADR-0002](adr/0002-the-media-pipeline-is-kind-aware-and-file-first.md),
[ADR-0003](adr/0003-two-scheduling-mechanisms.md) and
[ADR-0005](adr/0005-backup-is-automatic-for-the-database-and-manual-for-media.md) as their modules
go, and builds [ADR-0012](adr/0012-darek-speaks-polish-and-opens-in-polish.md)'s first-launch switch.
`CONTEXT.md` is owed nothing: no domain term appears on screen yet.

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
- `minSdk` 26 stays. The test phone is API 30, the owner's is API 36.

## What is in, and what is deliberately not

**In:** deleting, per [`optional-modules.md`](optional-modules.md), the Room database and schema gate,
the photo pipeline, the manual backup export and the custom `BackupAgent` (platform Auto Backup takes
over), reminders and WorkManager, and the Items domain; a home screen that holds the Listening
switch's place and carries the tip's ask; Darek's palette; the one-time Polish switch on first launch;
`aab-permissions.py`, `edge-to-edge.py`'s SCENES, the privacy policy and `play-app-content.md`
rewritten against what is left.

**Not in, and each with a reason rather than a phase:**

- **The Commands' storage.** A JSON list under one DataStore key is the shape (house rules), but its
  fields are settled by phase 1's reading and phase 3's editor. Writing it now writes it twice.
- **Vosk, the model, `RECORD_AUDIO`, `CALL_PHONE`.** Phase 1. A permission merged before the
  feature that uses it is a Play question with no answer.
- **The mark.** The owner's to choose; the placeholder ships until then.

**Kept although nothing uses them yet:** `work/Autostart.kt`, `work/BatteryExemption.kt`,
`work/NotificationPermission.kt` and the `com.miui.securitycenter` `<queries>` entry. Phase 2's
Listening service needs all four (ADR-0010), and deleting them now means rewriting them in a month.

## Auto Backup and live state

With the custom agent gone, every DataStore key reaches the user's next phone. Phase 0's keys are all
settings (theme, language, the tip's ask). The **one-time Polish marker** is the exception worth
naming: restored onto a new phone it says "already switched", which is right — the user's language
choice is restored with it. Listening state, which arrives in phase 2, is live state and is judged at
the read (`optional-modules.md`), not trusted from a restore.

---

## Checkpoints

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | Strip: reminders and WorkManager, Room, photos, backup export, Items | `refactor:` | — |
| **B** | The home screen, with the tip's ask | `feat:` | A |
| **C** | Darek's palette | `feat:` | — |
| **D** | Opens in Polish on first launch (ADR-0012) | `feat:` | — |
| **E** | Readings on the test phone | `docs:` | A–D |

A goes first because B replaces the screen A removes. C and D touch nothing A does and can merge in
any order.

## E. The readings

On the test phone, from a release-shaped build (`./gradlew assembleDebug -PreleaseShapedDebug`):

1. `adb shell dumpsys package com.maksforge.darek.debug | grep permission` — no
   `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `WAKE_LOCK`.
2. `adb shell run-as com.maksforge.darek.debug ls databases` — no `darek.db`.
3. With the phone's language set to English: a fresh install opens in Polish; switching to English
   in Settings, force-stopping and reopening keeps English.

## Amendments owed

ADR-0001, 0002, 0003 and 0005 each get a dated amendment in checkpoint A's PR, saying the module is
gone from Darek and what replaced it. The `DOD.md` standing schema gate is marked *parked* with its
five points kept verbatim.
