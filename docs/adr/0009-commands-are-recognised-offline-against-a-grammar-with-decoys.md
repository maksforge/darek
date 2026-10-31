# Commands are recognised offline by Vosk, against a grammar of the Commands plus Decoys

## Context

Darek has to hear one of a handful of fixed Polish phrases — *"Darek, otwórz bramę"* — in a room or a
car, for hours, without the network, and must **not** act on anything else. A false match is worse
than a miss: it opens a gate. Free speech around the phone is noise to be ignored, not input.

Offline Polish recognisers that run continuously on a phone come down to one: **Vosk** (Kaldi), with
its `vosk-model-small-pl-0.22` model (Apache 2.0, ~53 MB unpacked to ~50 MB of files). Small Vosk
models accept a **runtime grammar**: a list of phrases that restricts the search.

A host reading on 2026-10-31 (vosk 0.3.75, the model above, 104 clips of synthetic Polish speech from
two voices at two speeds, half with pink noise: 40 Commands, 64 non-Commands including *"Marek, otwórz
bramę"*, *"Jarek, otwórz bramę"*, *"Otwórz bramę"* and *"Darek mówił, że brama jest zepsuta"*):

| Mode | Commands heard | False matches | CPU (real-time factor) |
| --- | --- | --- | --- |
| Grammar = the Commands only | 40/40 | **16/64** | 0.018 |
| Grammar = the Commands + Decoys | 40/40 | 0/64 | 0.017 |
| Free decoding, matched in code | 40/40 | 0/64 | 0.082 |

A grammar of only the Commands has nowhere else to put *"Marek"*, so it returns the Darek Command at
confidence 1.00. That is the failure this ADR exists to prevent.

## Decision

- **Vosk** (`com.alphacephei:vosk-android`, with JNA) recognises 16 kHz mono audio from `AudioRecord`.
  Both libraries' 64-bit `.so` files are 16 KB page-aligned, which Play requires.
- **The model ships inside the app** and is unpacked to `filesDir` on first launch. No download step,
  no network ([ADR-0008](0008-darek-never-opens-a-network-connection.md)).
- **The grammar is the Commands plus Decoys**: names and words close to the Commands' (*marek, jarek,
  janek, darka, darku, dawid, otwórz, zamknij, brama*) and `[unk]`, so near-misses have somewhere to
  land.
- **A Command fires only on an exact match of a whole Command** in a final result. Partial results
  and word salads (*"darek bramę otwieraj"*) never fire.

## Alternatives

**Free decoding with matching in code.** Equally accurate in the reading, and it would understand any
phrasing. It costs ~5× the CPU of a grammar, all day, on a phone in a pocket. Kept as the fallback if
real voices on a real microphone show the grammar is too narrow.

**A separate wake-word engine** (Porcupine, openWakeWord) followed by recognition. Porcupine's custom
words need a vendor account and key; openWakeWord has no Polish models. And "Darek" alone is a common
name: the Command, not the name, has to be what fires.

**Android's on-device `SpeechRecognizer`** (API 31+). Not continuous, no grammar, and whether a Polish
on-device model is present depends on the phone.

**Downloading the model on first run.** A smaller install, but a network permission and a broken first
launch without signal. Rejected with ADR-0008.

## Consequences

- The install is ~50 MB larger, and the model takes the same again in `filesDir` once unpacked.
- The Apache 2.0 licences of Vosk, its model and JNA's join `assets/licences/`.
- The reading is synthetic speech. **Phase 1 owes the real one**: real voices, the phone's own
  microphone, at a distance and in a car. Decoys are added from what that reading mishears.
- Recognition pauses while Darek speaks, so he does not answer himself (ADR-0011).
