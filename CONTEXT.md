# Vocabulary

**The rule this file exists to enforce: one concept, one word, everywhere.** The entity, the DAO, the
route key, the string resource and the sentence in the UI all use it. When the domain word and the
user-facing word genuinely differ, both are written here with the difference stated.

## The domain

| Term | Means | Not |
| --- | --- | --- |
| **Darek** | the assistant: the name users speak and the voice that answers. Also the app's name | the bot, the AI, JARVIS |
| **Command** | one fixed Polish phrase Darek acts on, with its **Number** and its **Reply** — *"Darek, otwórz bramę"*. Spoken, never translated (ADR-0012). In Polish UI: *komenda* | wake word, hotword, skill, intent, trigger |
| **Gate** | what a Command opens by being called: a GSM gate opener's SIM. A Command for a garage or a wicket is still a Gate in code; the UI uses the user's own name for it. In Polish UI: *brama* | device, relay, contact |
| **Number** | the phone number a Command calls, typed in by the user | contact, recipient |
| **Reply** | what Darek says aloud when a Command fires, before he dials — *"Otwieram bramę"* | response, answer, confirmation |
| **Listening** | the state in which the microphone service runs and Commands can fire (ADR-0010). Started and stopped only by the user. In Polish UI: *Darek słucha* | always-on, hotword detection, recording |
| **Decoy** | a word in the recogniser's grammar that exists only so a near-miss (*Marek*, *Jarek*) has somewhere to land instead of matching a Command (ADR-0009). Never shown in the UI | filler, garbage word |
| **Tip** | a one-off payment to the developer through Google Play that **unlocks nothing** (ADR-0007). The home screen mentions it once per install; Help and feedback offers it for good | donation, support the project, premium, supporter |

## Words we deliberately avoid

- **"Recording"** — Darek never records. Audio is consumed frame by frame and never stored
  (ADR-0008); the UI and the listing say *listening*, never *recording*.
- **"Wake word"** — there is none. The whole Command is matched, not "Darek" followed by anything; a
  person saying "Darek" to a friend named Darek does nothing.
- **"Open the gate"** as a promise — Darek *calls* the Gate. Whether the opener accepts the caller is
  the opener's configuration; copy says *dzwonię do bramy* where the difference matters.

## Naming conventions in code

Some names in this codebase are deliberately generic — `AppDatabase`, `AppTheme`, `AppPreferences`,
`AppContainer`, `MainApplication`, `MainActivity`. They stay correct whatever the app is called, so
renaming the app is a package move rather than a repo-wide find-and-replace. Don't rename them to
match the product; name the *domain* types after the domain instead.
