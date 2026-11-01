# Daria is a persona the user switches to: her name, her voice, his lines in the feminine

## Context

The owner wants a choice of a man's or a woman's voice. On 2026-11-01 he chose **Daria**: switched
on in Settings, she answers to *„Hej Daria"* and speaks as a woman. Darek stays the default on a
fresh install, and the app, its icon and its listing keep the name Darek.

A persona is more than a voice. The name is the first word of every Command, the word the name
Decoys guard ([ADR-0009](0009-commands-are-recognised-offline-against-a-grammar-with-decoys.md)),
and the word whose presence decides between „nie rozumiem" and silence. Polish verbs agree with the
speaker: *nie załapałem* is Darek, *nie załapałam* is Daria.

Read on the host on 2026-11-01:
- Vosk's `vosk-model-small-pl-0.22` knows *daria*, *darii* and *darię*, and the near-misses *maria*,
  *marii* and *dario* (read from the symbol table in `graph/Gr.fst`, where *darek*, *marek* and
  *powiedz* were found and a nonsense word was not).
- Piper's `pl_PL-gosia-medium` is 63 MB, 22.05 kHz, with the same inference settings as Darek's
  mc_speech. Its phoneme map is mc_speech's less five symbols our rules never produce, so
  [ADR-0014](0014-darek-speaks-with-his-own-bundled-voice.md)'s pronunciation rules drive it as they
  are.

## Decision

- **A Persona is Darek or Daria**, chosen in Settings, Darek by default. It sets the name, the voice
  and the grammatical gender. Personas are built into the app, never fuchy
  ([ADR-0015](0015-darek-takes-on-fuchy.md)).
- **Only the chosen Persona's name works.** Every Command is the Persona's name followed by its
  phrase, so a Command is written once and works for both. The other Persona's name is a Decoy, with
  *maria*, *marii* and *dario* added beside *marek* and *jarek*. Said as a name, it gets a correction
  instead of „nie rozumiem": Daria says *„Nie jestem Darek, mówisz z Darią."*, Darek says
  *„Daria? Tu Darek, stary."*.
- **Daria's voice is `pl_PL-gosia-medium` as Piper publishes it**, bundled in the app at full
  quality and fetched at build time from the same pinned Hugging Face commit as Darek's, SHA-256
  checked. Only the chosen Persona's voice is loaded.
- **Her lines are his in the feminine.** Every Reply Darek ships has both forms. A fucha's Reply may
  carry a feminine form; without one, Daria speaks the masculine one.
- **The UI names the chosen Persona** wherever it says who is listening or what to say: *„Daria
  słucha"*, the hints, the notification, with feminine verb forms where they agree with her. The
  app's name, icon, listing and About screen stay Darek.

## Alternatives

**Both names answer at once, each in its own voice.** No switch, but twice the phrases that can fire
a call, two sets of name Decoys, and a user who chose Daria still answered by Darek when they name a
friend.

**Daria with her own character and lines.** The owner may grow her some later. Writing every line
twice, in the app and in every fucha, is the cost of starting there.

**A Persona as a fucha.** A Persona reaches every Command, every Decoy and every Reply's gender; it is
who answers, not one job among others.

**Daria's voice quantised to int8, or downloaded on demand through a Play asset pack.** The owner
heard int8 as clearly worse for Darek (ADR-0014) and finds a bigger download no problem. An asset
pack adds a download state, an offline failure and a `bundletool` test path for one file.

**Leaving the UI as Darek.** It would tell a Daria user the wrong name to say.

## Consequences

- The download grows by another ~63 MB, and the installed size by about the same.
- *gosia* carries mc_speech's licence grey area: CC0 recordings, fine-tuned from Piper's English
  *lessac* voice, whose recordings are research-only (ADR-0014). The Licences screen credits it the
  same way.
- Every UI string that names the Persona takes it as a parameter and needs both genders in both
  languages; the translation gate holds them like any other string.
- The fuchy format writes a Command's phrase without the name, and gives a Reply an optional
  feminine form.
- Daria is read on the phone like Darek's voice was (load time, synthesis time, the owner's ear), and
  her name and Decoys are read against real voices before they are trusted to call.
