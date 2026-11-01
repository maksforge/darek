# Darek connects only to MaksForge and to the addresses a fucha declares, and sends nothing he hears

## Context

A voice assistant is the app people most expect to be listening for someone else. Darek holds the
microphone open all day ([ADR-0010](0010-listening-is-a-microphone-service-the-user-starts.md)), so
the privacy promise has to be one a user can check: **what you say to him never leaves the phone.**

Darek's own work needs no network. Recognition runs on a bundled model
([ADR-0009](0009-commands-are-recognised-offline-against-a-grammar-with-decoys.md)), the Reply is
his own bundled voice ([ADR-0014](0014-darek-speaks-with-his-own-bundled-voice.md)), and the Gate
call goes through the phone's dialler
([ADR-0011](0011-the-gate-call-is-placed-through-telecom-after-the-reply.md)). Fuchy are what need
it: installing them from the Gigs page, updating them, and the ones whose task is on the internet,
such as the weather ([ADR-0015](0015-darek-takes-on-fuchy.md)).

Android grants `INTERNET` at install time, to every user, with no prompt, and has no way to hold an
app to some domains: the network security config governs certificates and plain HTTP, not
destinations. So once Darek declares it, where he connects is a rule of this code, checked by reading
the source and watching the traffic, not by reading the manifest.

## Decision

**Darek declares `INTERNET` from the phase that builds fuchy** (`docs/PLAN.md`). Until that phase
ships, the merged manifest declares none, and `scripts/aab-permissions.py` keeps it in FORBIDDEN.

**Darek connects to two kinds of address, and no other:**
- **maksforge.com**, for the Gigs page's `fuchy.json`, a fucha's file and its updates. The request
  carries nothing about the user: no account, no device ID, no analytics. A fresh install with only
  the bundled fuchy connects nowhere.
- **the addresses a fucha declares**, only while it runs, only with the permission the user granted
  it, and carrying only the values it declares, each one shown by name before install.

**Audio and the words Darek heard never leave the phone.** Audio is consumed frame by frame and
never written to storage; no fucha can declare either as a value to send.

**No dependency connects anywhere of its own accord.** ONNX Runtime from 1.29.0 merges a telemetry
provider that posts to Microsoft: it is pinned at 1.28.0, which has none, and the manifest removes
the provider anyway ([ADR-0014](0014-darek-speaks-with-his-own-bundled-voice.md)). Play Billing
talks to the Play Store app, not the network, but merges `datatransport`, a log uploader that today
fails only because `INTERNET` is missing
([ADR-0007](0007-the-tip-is-a-play-billing-consumable-that-unlocks-nothing.md)); the phase that adds
`INTERNET` removes its upload service in the manifest and reads the traffic to prove it.
`ACCESS_WIFI_STATE` stays forbidden; `ACCESS_NETWORK_STATE` is allowed as Play Billing's merge.

## Alternatives

**Never connect** (this ADR until 2026-11-01). The strongest promise, checkable from the manifest
alone, but fuchy could then only arrive by hand and no fucha could fetch anything. The owner chose
fuchy installed from the app.

**A companion app holding `INTERNET`**, with Darek handing it requests. Darek's manifest would stay
clean, but every user wanting the weather would install a second app, and the Gigs page would live
in it rather than in Darek.

**Cloud speech recognition.** Better on free speech, but it sends audio off the phone, fails with no
signal at the gate, and cannot run continuously in the background. Rejected for good: it breaks the
one promise this ADR keeps.

**Crash reporting of our own.** Android vitals covers opted-in devices; the mail hand-off in
`ui/support/` covers the rest.

## Consequences

- When the fuchy phase ships, the privacy policy, the listing, Data safety and
  `docs/play-app-content.md` are rewritten from "no network" to this ADR, and Data safety is re-read
  for the values fuchy send. Until then they stay as they are.
- `aab-permissions.py` moves `INTERNET` from FORBIDDEN to EXPECTED in that phase, and
  `ManifestWiringTest` with it.
- A dependency that connects on its own is still not used.
