# The Gate call is placed through Telecom, after Darek has finished his Reply

## Context

A GSM gate opener is driven by calling its SIM: it recognises the caller's number, rejects the call
and opens the gate, so the call costs nothing. Darek's action is therefore a phone call to a number,
placed while no Darek screen is visible — only the listening service is running.

`startActivity(ACTION_CALL)` from there is a background activity launch, which Android 10+ blocks and
a foreground service does not exempt. `TelecomManager.placeCall()` with `CALL_PHONE` goes through the
system's Telecom service, which checks the permission and nothing about visibility.

Play's malware policy forbids making calls without the user's consent; the rule is aimed at
premium-rate fraud, and `CALL_PHONE` is otherwise unrestricted.

## Decision

- **Calls are placed with `TelecomManager.placeCall(tel:<number>)`**, never with an activity intent.
- **The number is one the user typed in** for that Command. Darek ships with no number and never
  reads contacts.
- **A Command is acknowledged before it acts**: Darek speaks the Reply (*"Otwieram bramę"*) and waits
  for the engine's `onDone`. A **cancel window** of about 2 s follows, in which *"Darek, stop"*,
  *"anuluj"* or the notification's *Anuluj* action cancels; only then does he dial.
- **Consent** is the Number the user typed for that Command and the user's own start of Listening:
  that is what Play's malware policy asks for. A false match therefore costs a spoken Reply the user
  can cancel, not a silent dial.
- **Recognition is paused while he speaks and from the dial until the call ends**, so he cannot hear
  himself or the call. The end of the call is read from `AudioManager`'s mode
  (`OnModeChangedListener` on API 31+, `getMode()` below): recognition resumes when it returns to
  `MODE_NORMAL`, or after a timeout. No `READ_PHONE_STATE`.
- If dialling fails, he says so (*"Nie udało się zadzwonić"*) rather than staying silent.

## Alternatives

**Dial first, then speak.** Faster by a second, but the user learns nothing until the gate moves, and
the call takes the audio path the Reply needs.

**Call state from `TelephonyCallback` or `TelecomManager.isInCall()`.** Both need `READ_PHONE_STATE`
on API 31+, another runtime permission for something the audio mode already shows.

**SMS-driven openers.** Some openers accept an SMS. SMS permissions are restricted on Play to default
messaging apps; calling covers the openers people actually have.

**Hanging up after a few seconds** with `TelecomManager.endCall()` for openers that answer instead of
rejecting. It needs `ANSWER_PHONE_CALLS` and is deprecated; deferred until an opener that needs it
appears.

## Consequences

- The test phone has no SIM, so the dial is read on the owner's phone, with the owner's go-ahead each
  time (`CLAUDE.md`, environment notes). A dial is a real gate opening.
- Whether MIUI/HyperOS add a check of their own to `placeCall` from a background service is phase 1's
  reading on the owner's phone.
- The cancel window adds about 2 s between the Reply and the gate.
- Dual-SIM phones dial on the default SIM; choosing a SIM per Command waits for someone who needs it.
