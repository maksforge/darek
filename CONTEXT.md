# Vocabulary

**The rule this file exists to enforce: one concept, one word, everywhere.** The entity, the DAO, the
route key, the string resource and the sentence in the UI all use it. When the domain word and the
user-facing word genuinely differ, both are written here with the difference stated.

## The domain

| Term | Means | Not |
| --- | --- | --- |
| **Darek** | the assistant: the name users speak and the voice that answers. Also the app's name | the bot, the AI, JARVIS |
| **Command** | one fixed Polish phrase Darek acts on, with its **Reply** — *"Darek, otwórz bramę"*. A Command either **calls** its Gate's **Number** or **only replies** — *"Darek, powiedz coś"* (ADR-0013). Commands come in Gigs (ADR-0015): Darek's own (*brama*, *garaż*, *furtka*, *powiedz coś*), each with Decoys tuned for it, and the ones the user installs. The user gives each one that calls a Number and switches it on or off, but never types a phrase into Darek (ADR-0009). Spoken, never translated (ADR-0012). In Polish UI: *komenda* | wake word, hotword, skill, intent, trigger |
| **Gate** | what a Command opens by being called: a GSM gate opener's SIM. A Command for a garage or a wicket is still a Gate in code; the UI uses the Command's own word for it. In Polish UI: *brama* | device, relay, contact |
| **Number** | the phone number a Command calls, typed in by the user | contact, recipient |
| **Reply** | what Darek says aloud in his own voice (ADR-0014) and shows when a Command fires, before he dials if it calls; only shown if his voice fails (ADR-0011) — *"Otwieram bramę"*, *„Co tam, mordo?"*. From phase 3 the user can write a Command's Reply | response, answer, confirmation |
| **Listening** | the state in which the microphone is open and Commands can fire. Started and stopped only by the user. In phase 1 it lasts while Home is on screen; from phase 2 it is the microphone service and outlives the screen (ADR-0010). In Polish UI: *Darek słucha* | always-on, hotword detection, recording |
| **Decoy** | a word in the recogniser's grammar that exists only so a near-miss (*Marek*, *Jarek*) has somewhere to land instead of matching a Command (ADR-0009). Never shown in the UI | filler, garbage word |
| **Gig** | a file of Commands, with their Replies, that the user installs into Darek from his Gigs page or from a file. Darek ships with Gigs of his own (the gate, *powiedz coś*), which can be switched off but not removed. A Gig is data, never code: it can only use what Darek already does, and it declares every permission and address it needs, which the user sees before installing. It never carries a Number; a Command in it that calls uses one the user picks. In Polish UI: *fucha*, plural *fuchy* | pack, plugin, mod, extension, skill |
| **Approved** | said of a Gig MaksForge reviewed and signed: every Gig on Darek's Gigs page, and Darek's own. It carries the MaksForge badge; a Gig without it came from somewhere else and is installed only after a strong warning. In Polish UI: *zatwierdzona* | official, verified, trusted |
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
