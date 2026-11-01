# A Command may only reply, and *"Darek, powiedz coś"* is the first

## Context

[ADR-0009](0009-commands-are-recognised-offline-against-a-grammar-with-decoys.md) and
[ADR-0011](0011-the-gate-call-is-placed-through-telecom-after-the-reply.md) assume every Command
calls a Number. The owner asked on 2026-10-31 for a first milestone without a call: *"Hej Darek,
powiedz coś"*, to which Darek answers *„Co tam, mordo?"*.

Three facts make that more than a demo:
- The test phone has no SIM, so a Command that calls can never be read end to end on it. A Command
  that only replies can.
- A user setting Darek up has no way to check he hears them that does not ring the gate.
- Phase 1's release reaches closed testers before a user can type a Number (phase 3). Without a
  Command that needs no Number, those testers get an app that does nothing.

## Decision

- **A Command either calls or only replies.** The catalogue says which. A Command that only replies
  has a Reply and no Gate and no Number. It speaks its Reply and does nothing else, so it has no
  cancel window.
- **ADR-0009 applies to it unchanged**: the same grammar, Decoys and adjacency rule. A false match
  costs a spoken sentence, but the rule stays one rule.
- **The first is *"Darek, powiedz coś"* → *„Co tam, mordo?"***. Words before a Command are ignored
  (ADR-0009), so the owner's *"Hej Darek, powiedz coś"* fires it.
- **It ships, and it stays.** It is on by default because it needs no Number. It is Darek's way of
  saying *I can hear you* without dialling anything.

## Alternatives

**Hold phase 1 until the call works.** Every reading of the voice loop would then need the owner's
phone and a real gate opening. Closed testers would get nothing until phase 3.

**Make it debug-only, a test hook.** It would serve the readings and nothing else. The setup check is
worth as much to a user as to the developer.

**Let the user type their own reply-only phrases.** No Decoys would be chosen for them, and Vosk
drops words outside its model. New phrases arrive in fuchy instead
([ADR-0015](0015-darek-takes-on-fuchy.md)): the vocabulary is checked at install, and an approved
fucha's Decoys by the owner's review.

## Consequences

- *Command* in `CONTEXT.md` no longer implies a Number; the catalogue in code carries the kind.
- The listing and the privacy policy may name it. Neither has to change for it: it reads no data
  and calls nothing.
- *„Co tam, mordo?"* is colloquial. It is the owner's choice of voice for Darek, and the listing's
  screenshots will show it.
- A second reply-only Command is a fucha: bundled with a release, or installed from the fuchy page.
