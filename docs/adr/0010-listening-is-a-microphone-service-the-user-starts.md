# Listening is a microphone foreground service, and only the user starts it

## Context

Darek is only useful hands-free, so he must hear a Command with the screen off and the app closed.
Android allows that in one way: a foreground service of type `microphone`, with its permanent
notification.

Android limits **when** that service may start. Since Android 14 (targetSdk 34) a microphone service
cannot be started from the background at all — not from `BOOT_COMPLETED`, not from an alarm, not by a
service the app already runs. The exemptions are: an activity of the app is visible, the user tapped
the app's widget, or the user tapped one of its notifications. A Quick Settings tile is not documented
as an exemption; AOSP binds the tile without the privileges the check needs, so a tile must open an
activity (`startActivityAndCollapse`) that starts the service. Below 14 the same start is allowed but
the microphone records silence (Android 11's while-in-use rule). Sources:
[background start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start),
[Android 15's boot restrictions](https://developer.android.com/about/versions/15/behavior-changes-15),
and for the tile, AOSP's `TileLifecycleManager` and `ActiveServices` — read, not yet measured.

Once running, a foreground service keeps its process out of Doze's wakelock cull, and the audio server
holds its own wakelock while capturing, so listening continues with the screen off.

## Decision

- **Listening** is one foreground service, type `microphone`, with an ongoing notification that says
  Darek is listening and offers *Stop*.
- **It starts only from a user action**: the switch in the app, the notification's *Słuchaj* action,
  or the Quick Settings tile through a translucent activity.
- **After a reboot it does not start by itself.** A boot receiver posts a notification whose action
  starts it — one tap, an exempt start.
- **MIUI and other vendor ROMs** get a setup step for autostart and the battery saver's "No
  restrictions", because they kill foreground services the platform would keep.
- A **silenced** microphone (another app or a call took it) is shown, not hidden: the notification
  says Darek cannot hear.

## Alternatives

**Listen only while the app is open.** No permanent notification, little battery, and no special Play
declaration — but it is not the app the owner asked for. Rejected on 2026-10-31.

**Start on boot anyway** through a loophole (an alarm that opens an activity, an accessibility service).
Each is either refused on current Android or a Play policy violation.

## Consequences

- Play Console needs the **foreground service declaration** for `microphone` (use case: voice commands
  for an assistant, nothing saved) with a **video** of the user starting it, and a **prominent
  disclosure** in the app right before the `RECORD_AUDIO` request.
- The notification is permanent while listening. That is the platform's price and also the honest
  signal that the microphone is open.
- Battery cost is real and has to be measured over a night on the test phone before the door.
