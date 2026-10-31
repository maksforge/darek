# Darek speaks Polish, and the app opens in Polish on every phone

## Context

[ADR-0004](0004-english-base-with-an-in-app-language-switcher.md) makes English the base: strings are
written in English, translated from English, and an unmatched locale falls back to English. That
suits an app whose users could be anyone.

Darek's users cannot be anyone. The Commands, the recogniser and the Reply are Polish
([ADR-0009](0009-commands-are-recognised-offline-against-a-grammar-with-decoys.md)), so a person who
does not speak Polish cannot use him at all. The owner set Polish as the main language on 2026-10-31.

## Decision

**ADR-0004 stands for how strings are written** — English in `values/`, Polish in `values-pl/`,
translated from English and held complete by the translation gate — **and is amended for which one
shows**:

- **On first launch the app sets its locale to Polish** through AppCompat's `setApplicationLocales`,
  whatever the phone's language. It does so once, guarded by a one-time DataStore marker, so a later
  choice in the switcher, English or "follow the phone", sticks.
- **Commands and Replies are Polish and are not translated.** They are speech, not UI: they live in
  the Command definitions, outside `strings.xml`, and the translation gate does not see them.
- **The Reply needs an offline Polish voice.** Darek checks the text-to-speech engine for a `pl-PL`
  voice that is installed and needs no network; without one he offers the engine's install screen and
  shows that he cannot answer aloud. A Command still acts without a voice.

## Alternatives

**Polish as the base (`values/` in Polish).** Simplest on paper, but every template script, the
translator brief and the gate assume an English source. Opening in Polish gets the same result for the user without forking the tooling.

**Follow the phone's language.** An English-language phone would get an English UI around a Polish
voice — a mismatch that only confuses.

## Consequences

- The Play listing's default language is Polish (pl-PL), with English as a translation; the
  screenshots are Polish.
- `TranslationTest` and the gate keep working unchanged.
- A user who picks English in the switcher keeps English; Darek still speaks Polish.
