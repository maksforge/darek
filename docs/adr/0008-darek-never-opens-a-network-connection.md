# Darek never opens a network connection

## Context

A voice assistant is the app people most expect to be listening for someone else. Darek holds the
microphone open all day ([ADR-0010](0010-listening-is-a-microphone-service-the-user-starts.md)), so the
only privacy promise worth making is one a user can check: nothing it hears can leave the phone,
because the app has no way to send it.

Nothing Darek does needs the network. Recognition runs on a bundled model
([ADR-0009](0009-commands-are-recognised-offline-against-a-grammar-with-decoys.md)), the Reply is
Darek's own bundled voice ([ADR-0014](0014-darek-speaks-with-his-own-bundled-voice.md)), and the Gate call goes through the phone's dialler
([ADR-0011](0011-the-gate-call-is-placed-through-telecom-after-the-reply.md)).

## Decision

**The merged manifest of every build declares no `INTERNET` or `ACCESS_WIFI_STATE`.** A dependency
that merges one has it removed with `tools:node="remove"` and must work without it; a library that
cannot is not used. The tip's Play Billing Library is the known case: its `INTERNET` is removed as
[ADR-0007](0007-the-tip-is-a-play-billing-consumable-that-unlocks-nothing.md) describes.
**`ACCESS_NETWORK_STATE` is allowed only as Play Billing's merge** under ADR-0007: reading network
state opens no connection.

`scripts/aab-permissions.py` checks this on the built bundle: `INTERNET` is in its FORBIDDEN list,
`ACCESS_NETWORK_STATE` is in EXPECTED as billing's, and any permission not in EXPECTED, such as
`ACCESS_WIFI_STATE`, fails the check.

Audio is never written to storage. The recogniser consumes it frame by frame and keeps only the text
of a match.

## Alternatives

**Cloud speech recognition** (Android's default `SpeechRecognizer`, or a hosted API). Better on free
speech, worse on everything Darek needs: it sends audio off the phone, fails with no signal at the
gate, and cannot run continuously in the background.

**Crash reporting of our own.** Android vitals covers opted-in devices; the mail hand-off in
`ui/support/` covers the rest. The same trade Kozica made.

## Consequences

- The privacy policy and Play's Data safety answers stay short: audio is processed on the phone and
  never stored or sent. Adding any network feature reopens both, and this ADR.
- The Reply never leaves the app: Darek speaks it with his own bundled voice, not through another
  app's text-to-speech engine (ADR-0014).
