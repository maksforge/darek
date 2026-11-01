# Phase 1 — Say *"Hej Darek, powiedz coś"* with the app open and Darek answers out loud

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). This phase
builds [ADR-0009](adr/0009-commands-are-recognised-offline-against-a-grammar-with-decoys.md)'s
recogniser, [ADR-0011](adr/0011-the-gate-call-is-placed-through-telecom-after-the-reply.md)'s Reply,
cancel window and call, and [ADR-0012](adr/0012-darek-speaks-polish-and-opens-in-polish.md)'s offline
Polish voice. It adds [ADR-0013](adr/0013-a-command-may-only-reply.md): a Command that only replies.
It edits ADR-0009 (size, the grammar's encoding) and ADR-0011 (which half of its HyperOS question
this phase can answer) in place. `CONTEXT.md` is owed a change to *Command*, which no longer always has a
Number, and to *Listening*, which in this phase exists only while Home is on screen.

**`PLAN.md`'s phase 1 and phase 3 lines were wrong, and this PR corrects them.**

- Phase 1 promised the gate call as the phase's result. The owner moved the first milestone on
  2026-10-31: a Command that only answers, *"Hej Darek, powiedz coś"* → *„Co tam, mordo?"*. It can be
  read end to end on the test phone, which has no SIM. The call is still built in this phase, but
  **only in the debug build** (section 4).
- Phase 3 was called *the door*, where closed testing opens. **The door opened with phase 0:**
  Alpha 0.2.0 went to Google's review on 2026-10-31 with the tester-swap group as its testers. So a
  release cut by this phase can reach strangers. That moves the microphone's prominent disclosure out
  of phase 3 and into this phase, and keeps `CALL_PHONE` out of the release until phase 3's Number
  editor exists.

**What closing this phase means.** Today the app opens on a home screen that does nothing. After this
phase, Home has a *Słuchaj* button. While Home is on screen and the button is on, saying *"Hej Darek,
powiedz coś"* makes Darek answer *„Co tam, mordo?"* out loud, in Polish, with no network. In the debug
build, *"Darek, otwórz bramę"* also makes him answer *„Otwieram bramę"*, wait about 2 s for *"anuluj"*,
and then dial a Number set in the debug settings. The difference a device can be asked about: the
release AAB declares `RECORD_AUDIO` and not `CALL_PHONE`. Spoken to from 3 m, the test phone answers.

---

## 0. What this phase inherits

- **The model and the grammar have a host reading** from 2026-10-31 (ADR-0009): synthetic voices,
  40/40 Commands heard, 0/64 near-misses fired with Decoys in the grammar. Real voices on a phone's
  microphone have never been tried. That is this phase's gate.
- **Every word of the Commands is in the model's vocabulary.** Read on the host 2026-11-01 (vosk
  0.3.45, `vosk-model-small-pl-0.22`): *hej, darek, powiedz, coś, otwórz, bramę, garaż, furtkę, stop,
  anuluj, marek, jarek* are all accepted. That holds **only when the grammar's JSON carries Polish
  letters as UTF-8**. Written with `\u` escapes (Python's default `ensure_ascii`), Vosk takes
  `coś` as a literal word and drops it with a warning. *coś, otwórz, bramę, garaż* and *furtkę*
  vanish, and the Commands can never fire. Android's `org.json` writes UTF-8, but a test pins it
  (*Tests*).
- **The model is 53 MB zipped (SHA-256 `c4cd1649…6fce15f0d`) and 92 MB unpacked.** ADR-0009 said
  ~50 MB unpacked, which was wrong; it is corrected. After the first run the phone holds both the copy
  inside the APK and the unpacked one, ~145 MB in all.
- **The test phone has Google's TTS engine** (`com.google.android.tts`). Whether it has an offline
  `pl-PL` voice installed is reading R3.
- **The release is minified** (`isMinifyEnabled = true`). JNA finds Vosk's native methods by
  reflection. A missing keep rule does not crash; recognition silently returns nothing. So the
  end-to-end reading is taken on `-PreleaseShapedDebug`.
- **Closed testers exist, or will within days** (above). Anything a `feat:` merge releases may reach
  a stranger's phone.

## What is in, and what is deliberately not

**In:**
- The model, fetched at build time and unpacked on the phone.
- A recogniser over `AudioRecord` with ADR-0009's grammar and match rule.
- The Command catalogue with one Command that only replies (ADR-0013) and one that calls.
- The offline Polish voice and what Home shows without one.
- The *Słuchaj* button, with the microphone disclosure and the `RECORD_AUDIO` ask on its first tap.
- In the debug build only: a Number for *brama*, `CALL_PHONE`, the cancel window and `placeCall`.
- A debug-only probe screen that shows the recogniser's raw results, which is the gate's apparatus.

**Not in, and each with a reason rather than a phase:**

- **Hearing with the screen off or the app closed.** That is the microphone service, its
  notification and its Play declaration (ADR-0010). It is a different cost from everything here, so
  it is a different phase (`PLAN.md` rule 2).
- **`CALL_PHONE` and the call in the release build.** A user would have no way to type a Number.
  A permission shipped before the feature that uses it is a Play question with no answer. The
  call's code lives in `main/`; the permission and the only way to set a Number live in
  `app/src/debug/`.
- **Garaż and furtka.** They are the same code path as brama with different words. Their Decoys are
  read once brama's are settled. Adding them now triples the reading for no new knowledge.
- **The Commands' stored shape** (the JSON list under one key). Phase 3's editor settles which
  fields a user changes. The debug Number is a debug-only store (section 4), so nothing in `main/`
  commits to a shape early.

**The tempting one: free decoding "just in case".** ADR-0009 keeps free decoding with matching in
code as the fallback. It is one flag away in Vosk, so it looks free to ship both. It costs ~5× the
CPU all day once phase 2 runs it in a pocket. A second mode also doubles every reading. It comes in
only through verdict 4 below, by a new ADR.

---

## Checkpoints

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | **The gate**: the probe, debug-only, and the reading with a real voice on both phones | `chore:` | — |
| **B** | The reply-only Command in the release: *Słuchaj*, disclosure, voice, *powiedz coś* | `feat:` | A |
| **C** | The Gate call, debug-only: Number, `CALL_PHONE`, cancel window, `placeCall` | `chore:` | B |
| **D** | Readings on both phones; listing, privacy policy and App content for B's release | `docs:` | B, C |

**Why A is a `chore:`.** Everything A adds is under `app/src/debug/` or unused by the release: the
Vosk dependency is `debugImplementation`, and the model is attached to the debug assets. The
exception is the pure matcher in `main/`, which nothing in the release calls yet. A `feat:` would
cut a release with no change a user can see. B moves the dependency and the model to `main`.

**Why C is a `chore:` after B.** C's code in `main/` does nothing in a release, which has no Number
and no `CALL_PHONE`. B's release must not wait on it. C comes after B because it reuses B's Reply
and pause-while-speaking path instead of building a second one.

D's listing, privacy-policy and App-content edits ride with **the release that ships B**, as the
`DOD.md` item already says. They are written in the release's notes PR, not after the upload.

---

## 1. The gate: does the phone's microphone hear a real voice?

### The question, in the form that can be measured

With the grammar of ADR-0009 (the Commands, the cancel words, the Decoys, `[unk]`), a real voice,
and each phone's own microphone with the app open:

- Does *"Hej Darek, powiedz coś"* fire at least **9 times in 10 at 0.5 m** and **8 in 10 at 3 m**?
- Does *"Darek, otwórz bramę"* do the same?
- Does **nothing** fire for the near-miss script and for 2 minutes of radio or conversation?

### The apparatus

`app/src/debug/`: a **probe** screen, opened from Settings' debug section.
- It starts a recogniser with the grammar and shows each partial and final result's raw tokens.
- It marks each final result with the Command the matcher would fire, if any.
- It logs every final result to logcat with the tag `DarekProbe`, so the reading is collected with
  `adb logcat -s DarekProbe` instead of being transcribed off the screen.
- `RECORD_AUDIO` is declared in the debug manifest and asked for on the probe's start.
- The probe has a grammar/free toggle, used **only** under verdict 4.
- It writes no audio anywhere (ADR-0008 holds in the debug build too).

The owner speaks; the session runs adb and reads the log. The script, each line said once unless a
count is given:

| Group | Lines |
| --- | --- |
| Commands | *"Hej Darek, powiedz coś"* ×10 at 0.5 m, ×10 at 3 m; *"Darek, otwórz bramę"* ×10 at 0.5 m, ×10 at 3 m; both ×5 with the radio on |
| Embedded, must fire | *"Darek, otwórz bramę, proszę"*; *"No to Darek, powiedz coś"* |
| Near-misses, must not fire | *"Marek, powiedz coś"*; *"Jarek, otwórz bramę"*; *"Otwórz bramę"*; *"Darek, powiedz mi coś"*; *"Powiedz coś, Darek"*; *"Darek mówił, że brama jest zepsuta"*; *"Darek bramę otwórz"* |
| Noise, must not fire | 2 minutes of radio speech; 2 minutes of ordinary conversation that mentions Darek |

### The verdicts, decided now rather than argued about later

1. **Both Commands meet both thresholds on both phones, and nothing false fires.** → B as planned.
2. **The Commands meet the thresholds, but something false fires.** → Add Decoys from the logged
   tokens of what misfired, re-read only the failing lines and their Commands, then B.
3. **The thresholds hold at 0.5 m but not at 3 m.** → B goes ahead. The distance becomes a known
   gap, written into the listing's *what it cannot do*, and phase 2 re-reads it with the phone where
   it will actually sit.
4. **The 0.5 m threshold fails.** → Repeat the Command lines with the probe's free-decoding toggle.
   - If free decoding meets the thresholds, a new ADR supersedes ADR-0009's grammar for free
     decoding, at its CPU cost, before B.
   - If it does not, the phase stops and the owner hears it plainly: no offline Polish model that
     fits a phone hears this voice. The larger Vosk Polish model is a server model.

### The verdict, taken *(date)*

*(Filled in from R1 and R2.)*

## 2. The model, the recogniser and the match

**The model is fetched at build time, not committed.**
- A Gradle task downloads `vosk-model-small-pl-0.22.zip` from alphacephei.com into Gradle's user
  cache, checks its SHA-256 and fails the build on a mismatch.
- It unzips into a generated assets directory, registered through `androidComponents`. A clean
  build does not fetch again.
- CI caches the zip keyed on the hash. If the host ever disappears, the zip is mirrored as an asset
  of a GitHub release in this repo and the URL changes; the hash does not.
- Committing it would add 53 MB of binary to every clone for good.

**Unpacking.**
- On the first tap of *Słuchaj*, the model is copied out of the APK into
  `noBackupFilesDir/vosk-model-small-pl-0.22.tmp/`, then renamed to the final name.
- A directory with the final name is therefore complete. A half-finished copy is deleted and
  redone, never loaded.
- A new model is a new directory name; the old one is deleted on the next unpack.
- While it runs, the button shows *Przygotowuję…*. R6 reads how long it takes on the test phone.

**The recogniser** is one class in `listen/`.
- It runs `AudioRecord` at 16 kHz mono, 16-bit, `VOICE_RECOGNITION` source, on its own thread.
- It feeds a Vosk `Recognizer` built with the grammar and emits partial and final results as a
  `Flow`.
- It has no idea of screens. Phase 2 moves it, unchanged, into the service.
- It stops reading audio while Darek speaks and resumes on the engine's `onDone` (ADR-0011), so he
  never hears himself.

**The grammar** is built from the catalogue (section 3): every word of every enabled Command, the
cancel words, the Decoys, and `[unk]`. A Command that cannot act is not enabled, and its phrase is
not in the grammar: in the release, *brama*, which has no Number. The cancel words are in the
grammar only when a Command that calls is enabled.

**The match** is ADR-0009's rule, as one pure function in `listen/CommandMatch.kt`:
- Input: a final result's raw tokens. Output: the Command whose words appear adjacent and in order,
  or none.
- Leading and trailing words are ignored, so *"Hej Darek, powiedz coś"* fires *Darek, powiedz coś*.
  *Hej* is not part of the Command; the owner's greeting works because it comes before it.
- Partial results never fire a Command. They are read only for the cancel words (section 4).

## 3. The catalogue, the Reply and Home

**The catalogue** is a Kotlin enum in `listen/Command.kt`. Words are lower-case, as the model emits
them:

| Command | Words | Reply | Acts |
| --- | --- | --- | --- |
| `SAY_SOMETHING` | *darek powiedz coś* | *„Co tam, mordo?"* | replies only (ADR-0013) |
| `GATE` | *darek otwórz bramę* | *„Otwieram bramę."* | calls its Number (ADR-0011) |

The Reply text is speech, not UI: it lives in the enum, outside `strings.xml` (ADR-0012).

**The voice** comes from a pure function, `speak/pickVoice(voices)`. It picks the first voice that is:
- locale `pl-PL`,
- `!isNetworkConnectionRequired`,
- without `FEATURE_NOT_INSTALLED`.

If there is none, Home shows that Darek cannot answer aloud, with a button that opens the engine's
`ACTION_INSTALL_TTS_DATA` screen. Commands still act without a voice (ADR-0012).

**Home** loses its placeholder line and gains, in the middle:
- **The *Słuchaj* button.** Off, it says *Słuchaj*; on, *Darek słucha* with *Zatrzymaj*. It turns
  itself off when Home leaves the screen (`ON_STOP`), because in this phase the microphone belongs
  to the visible screen. The button is the switch phase 2 makes outlive the screen.
- **The last Reply, as a line of text under the button** (*„Co tam, mordo?"*). This confirms the
  Command for a user with the sound off, and is what a reading reads off the screen.

**The microphone ask** (`PLAN.md` rule 4: the ask waits for the feature).
- On the first tap of *Słuchaj*, a sheet explains, before the system dialog, that Darek listens only
  while this button is on. It also says the sound is processed on the phone and never recorded or
  sent. *Dalej* opens the system's `RECORD_AUDIO` dialog.
- **On a denial**, the button stays off and Home shows a line that Darek cannot hear without the
  microphone, with *Zezwól*.
- **On a permanent denial** (`shouldShowRequestPermissionRationale` false after a denial), *Zezwól*
  becomes *Otwórz ustawienia* and opens the app's details screen. The app re-reads the grant on
  every `ON_START`.

The copy, in English, translated once at the merge:

- `home_listen` *Listen* · `home_listening` *Darek is listening* · `home_stop` *Stop* ·
  `home_preparing` *Getting ready…*
- `mic_disclosure_title` *Darek needs the microphone*
- `mic_disclosure_body` *Darek listens only while the Listen button is on. What he hears is processed
  on this phone and is never recorded or sent anywhere.*
- `mic_disclosure_continue` *Continue*
- `mic_denied` *Darek can't hear you without the microphone.* · `mic_allow` *Allow* ·
  `mic_open_settings` *Open settings*
- `voice_missing` *There's no offline Polish voice on this phone, so Darek can't answer aloud.* ·
  `voice_install` *Install a voice*

## 4. The Gate call, in the debug build

- **The Number** comes through a seam, `listen/GateNumber.kt`, one file per variant (house rules).
  - `src/debug/` reads a debug-only `SharedPreferences` file, written by a field in Settings' debug
    section.
  - `src/release/` returns `null`.
  - With `null`, `GATE` is not enabled and not in the grammar.
- **`CALL_PHONE`** is declared in `src/debug/AndroidManifest.xml` only. It is asked for when the
  debug Number is saved, because that is when the feature is switched on.
- **The sequence** (ADR-0011):
  1. Final result matches `GATE`.
  2. Recognition pauses and Darek speaks *„Otwieram bramę."*.
  3. On `onDone`, recognition resumes and the **cancel window** of 2 s runs.
  4. If a partial result containing *stop* or *anuluj* arrives, Darek cancels and says *„Anulowane."*
  5. If not, `TelecomManager.placeCall(tel:<number>)`, and recognition stays paused until the audio
     mode has left `MODE_NORMAL` and returned, or 60 s.
  6. If the call fails, Darek says *„Nie udało się zadzwonić."*.
- **The cancel window is a pure state machine**, `listen/CancelWindow.kt`, driven by events (Reply
  done, partial result, tick) and a clock. It answers one question: dial or not. It is the safety
  value of this phase: getting it wrong opens a gate the user said no to.

---

## Tests

`PLAN.md` rule 3's second clause. Each fails in the direction the app exists to prevent:

- **`CommandMatchTest`**: the 40 Command and 64 non-Command token strings of ADR-0009's host reading,
  plus R1's misfires once taken. Checks adjacency, order, an `[unk]` between words, and leading and
  trailing words. It fails toward a **false fire**.
- **`CancelWindowTest`**:
  - A partial result with *anuluj* at 1.9 s cancels.
  - One at 2.1 s is too late and the dial stands.
  - No dial happens before the Reply's `onDone`, however much time passes.
  - A cancel word in a partial result *before* `onDone` cancels too.

  It fails toward a **dial the user cancelled**.
- **`GrammarTest`**: the grammar JSON contains `coś` and `bramę` as UTF-8, never `\u`. With the
  call off it contains no word of `GATE` and no cancel word. It fails toward **Commands that can
  never fire**, silently.
- **`PickVoiceTest`**: a network voice is never picked, even when it is the only `pl-PL` one. It
  fails toward **the Reply text leaving the phone** through the TTS engine (ADR-0008).

## Documents this phase amends

- **New [ADR-0013](adr/0013-a-command-may-only-reply.md)**, and **ADR-0009** (92 MB unpacked;
  UTF-8 grammar; Commands that only reply) and **ADR-0011** (it applies to Commands that call; the
  HyperOS background question moves to phase 2) edited in place. In this PR.
- **`CONTEXT.md`**: *Command* and *Listening*. In this PR.
- **`PLAN.md`**: phase 1's and phase 3's lines, and the asks table rule 4 asks for. In this PR.
- **In the release that ships B** (`DOD.md`, *Before the next upload*):
  - The listing's test-build paragraph, in both languages: *"This test build answers one Command
    with the app open, „Hej Darek, powiedz coś". Listening with the screen off and the call arrive in
    the next test builds."* The screenshots are re-shot with the *Słuchaj* button.
  - The privacy policy's microphone bullet. *"While it is on, a notification says that Darek is
    listening"* becomes *"the app's screen, or later its notification, says…"*.
  - `play-app-content.md`: `RECORD_AUDIO` gets its row and leaves the *not in the artifact yet*
    list, and the Data safety answer is re-read (still *no data collected*: audio is processed on
    the phone). `CALL_PHONE` stays on that list.
  - `aab-permissions.py`: `RECORD_AUDIO` into EXPECTED. The `android.hardware.microphone` feature it
    implies, `required`, is accepted: a phone without a microphone cannot use Darek.
  - Licences: the Vosk API, the model and JNA (Apache 2.0, JNA dual-licensed and taken as Apache),
    in `assets/licences/` and the `licensee` allowlist.

## Known gaps, deliberately

- **Darek hears only while Home is on screen.** Phase 2's service is the fix, and costs a
  notification, a Play declaration and a battery reading.
- **The call exists only in the debug build** until phase 3 lets a user type a Number.
- **~145 MB on the phone after the first *Słuchaj*.** It is the APK's copy plus the unpacked model.
  Removing the APK's copy means a download (ADR-0008 forbids it) or an install-time asset pack,
  which is still unpacked by us. Not worth it for one model.
- **HyperOS and `placeCall` from the background** is unanswerable here: phase 1 dials only with the
  app open. ADR-0011 now gives it to phase 2.
- **Dual-SIM phones set to *ask every time*** may show a SIM chooser on `placeCall`. R7 reads whether
  the owner's phone does; choosing a SIM per Command waits for a user who needs it (ADR-0011).

## Readings

`PLAN.md` rule 3: filled in as the phase runs, from the device, with the command that read it.

**Device state:**
- **Test phone** `mnbm7hw8ivdqzx9l`: woken and unlocked before each reading
  (`svc power stayon usb`, `input keyevent KEYCODE_WAKEUP`, `wm dismiss-keyguard`).
  `persist.sys.miui_optimization` must read `false` before any install.
- **Owner's phone** `132bc856`: announced in chat before a reading takes its screen. R7 needs the
  owner's go-ahead in chat each time, because it opens the real gate.
- **Voices:** R1, R2 and R8 need the owner's voice. The session runs adb and reads the log; it
  cannot speak.

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1 | The gate script (section 1) on the test phone, debug build, probe | `adb logcat -s DarekProbe` while the owner reads the script | |
| R2 | The same on the owner's phone | as R1, `-s 132bc856` | |
| R3 | An offline `pl-PL` voice exists on each phone, and which one `pickVoice` chose | the `DarekVoice` log line on first *Słuchaj*; `adb shell settings get secure tts_default_synth` | |
| R4 | End to end on `-PreleaseShapedDebug`: *Słuchaj* → *"Hej Darek, powiedz coś"* → *„Co tam, mordo?"* heard and shown, on both phones | `./gradlew installDebug -PreleaseShapedDebug`, then the screen and `screencap` | |
| R5 | Denial: deny once → *Zezwól*; deny again → *Otwórz ustawienia* opens the app's details screen | `adb shell pm revoke … RECORD_AUDIO`, then the screen | |
| R6 | First unpack time on the test phone, and that a killed half-unpack is redone | `DarekModel` log line; `adb shell am force-stop` mid-unpack, relaunch, `run-as … ls no_backup` | |
| R7 | The Gate call on the owner's phone, with the owner's go-ahead: Reply, ~2 s, dial; then once with *"anuluj"* in the window, no dial | the screen, `adb shell dumpsys telecom` call list | |
| R8 | The owner's phone in a parked car, engine running, Commands at arm's length | `adb logcat -s DarekProbe` | |
| R9 | The release AAB declares `RECORD_AUDIO` and not `CALL_PHONE`; the debug APK declares both | `python3 scripts/aab-permissions.py` on `bundleRelease`; `aapt2 dump permissions` on the debug APK | |
