# Darek speaks with his own bundled voice, through our own Polish pronunciation rules

## Context

Darek's Replies were to be read out by the phone's text-to-speech engine
([ADR-0012](0012-darek-speaks-polish-and-opens-in-polish.md)). On most phones that engine is
Google's, and a phone without an offline Polish voice left Darek mute. That raised the question of
whether a Command that calls may dial on a Reply nobody hears.

The owner decided on 2026-11-01 that Darek does not rely on Google, and that a user will be able to
type their own Reply text. So fixed recorded clips are not enough: Darek needs a voice that reads any
Polish text, inside the app, offline.

Read on the host on 2026-11-01:

- **The voice.** Piper's `pl_PL-mc_speech-medium` (a VITS model in ONNX, 22.05 kHz), 63 MB. It
  renders 4.6 s of speech in 0.09 s on the PC CPU; the phone's time is a phase 1 reading (R3).
- **The pronunciation.** Piper turns text into phonemes with eSpeak NG, which is GPL-3.0. Linking it
  would put Darek's code under the GPL, which his all-rights-reserved licence cannot take. Polish
  spelling is regular enough for rules instead. A rule set written for this produces the model's
  phoneme alphabet: letters and digraphs, soft *i*, nasal vowels by what follows them, voicing
  assimilation, final devoicing, penultimate stress and numbers as words. On the 3,000 most frequent
  Polish words it agrees with eSpeak exactly on 92.4%, with a phoneme error rate of 1.4%. Most
  differences are English names, abbreviations and eSpeak's own slips (*musimy* is [muˈɕimɨ], not
  eSpeak's [muˈsimɨ]). The rules were written from Polish orthography, not ported from eSpeak's
  `pl_rules` or `pl_list`; eSpeak was only the comparison oracle on the host. They are committed as
  `scripts/voice/g2p_reference.py`.
- **The runtime.** `onnxruntime-android` from **1.29.0** on (still in 1.31.1) merges `INTERNET`,
  `ACCESS_NETWORK_STATE` and a `ContentProvider`, `ai.onnxruntime.TelemetryInitializer`, that starts
  at process start and posts usage data to Microsoft. **1.28.0** merges none of them. Its
  `jni/arm64-v8a/libonnxruntime.so` is 28.6 MB (x86_64: 34.6 MB), and every `LOAD` segment of both
  64-bit libraries is aligned to 16 KB (`0x4000`).
- **The sound.** The owner heard the same sentence through our rules and through eSpeak, and went
  ahead with ours on 2026-11-01. On the phone the same day he compared the full model with a dynamic
  int8 quantisation of it (19 MB) and heard a big difference: the full model ships. On the test
  phone it is also the faster of the two (phase 1, R3).

## Decision

- **The Reply is spoken by Darek's own voice:** the mc_speech model as Piper publishes it, run by
  ONNX Runtime for Android (`com.microsoft.onnxruntime:onnxruntime-android`, MIT), and played
  through `AudioTrack`. Android's `TextToSpeech` is not used. No other app is involved, and every phone has the voice.
- **ONNX Runtime is pinned at 1.28.0,** the last version without the telemetry provider. The
  manifest also removes `ai.onnxruntime.TelemetryInitializer` with `tools:node="remove"`, beside
  `INTERNET`'s removal ([ADR-0008](0008-darek-connects-only-where-the-user-can-see.md)), so a later
  version cannot switch it on silently. `ACCESS_NETWORK_STATE` stays Play Billing's alone. A bump
  past 1.28.0 re-reads the AAR's manifest first. 1.28.0's 64-bit `.so` files are 16 KB
  page-aligned, which Play requires; a version change re-reads that too.
- **Text becomes phonemes through our own rules,** `speak/PolishPhonemes.kt`, mapped to the model's
  ids from its `phoneme_id_map`: `^`, then each codepoint's id followed by the pad `_`, then `$`
  (phase 1, section 3). No eSpeak, no GPL code.
- **The model ships inside the app.** It is fetched at build time from a release asset of this repo,
  pinned by SHA-256, as the recogniser's model is (ADR-0009). It is loaded straight from the APK's
  assets into the ONNX session, with nothing unpacked.
- **Any Polish text can be spoken.** Phase 3 lets the user write a Command's Reply. The rules expand
  numbers, so a Reply with digits works.

## Alternatives

**The phone's text-to-speech engine.** It costs nothing in size and lets the user pick any engine,
including free ones without Google (SherpaTTS, RHVoice, eSpeak NG from F-Droid). But most phones
default to Google, the voice differs from phone to phone, and a phone without an offline Polish voice
leaves Darek mute. Rejected on 2026-11-01.

**Recorded clips of fixed Replies.** Tiny and as clean as a recording can be, but they cannot say a
Reply the user types.

**Piper or sherpa-onnx with eSpeak NG.** No rules to write, but GPL-3.0 inside the app.

**A reduced-operator ONNX Runtime build** (an ORT-format model and only the operators it uses).
It brings the native library down to a few MB, at the cost of building ONNX Runtime ourselves: a
custom native build in CI, redone for every version. Kept for the day the size reading hurts.

**The model quantised to int8** (19 MB instead of 63 MB). The owner heard it as clearly worse
on 2026-11-01, and on the test phone it loaded and spoke more slowly than the full model. Rejected:
44 MB is not worth a voice that sounds worse.

**Other free Polish voices.** Piper's *darkman* and *gosia* carry the same licence question as
mc_speech (below). Meta's MMS-TTS and Coqui's XTTS are non-commercial.

## Consequences

- Daria's voice, Piper's *gosia*, is a second model of the same kind, run the same way
  ([ADR-0016](0016-daria-is-a-persona-the-user-switches-to.md)).
- **The licence has a grey area, accepted knowingly.** mc_speech's recordings are CC0 and Piper's
  voices are published under MIT. But the voice was fine-tuned from Piper's English *lessac* voice,
  whose recordings (Blizzard 2013) are licensed for research only and exclude voice products.
  Whether that reaches a fine-tuned model is unsettled. The Licences screen credits the voice with
  both facts. The way out is a voice trained from scratch on the CC0 MC Speech recordings, which
  swaps a file and changes no code.
- About 92 MB more installed on an arm64 phone: the 63 MB model and the 28.6 MB `libonnxruntime.so`,
  both stored uncompressed in the APK. Phase 1 reads the real total (R10).
- ONNX Runtime's version is held back on purpose. A newer one means reading its manifest for
  anything that reaches the network, the way ADR-0007 found `datatransport` behind billing.
- Synthesis runs on the phone's CPU each time a Reply is spoken. Fixed Replies can be rendered once
  and cached in memory.
- The pronunciation rules are ours to maintain. A mispronounced word is a rule fix with a test, not
  a bug report to an engine.
- The no-voice case is gone. What remains is a voice that *fails* (the model does not load, or
  playback errors), which ADR-0011 treats like a Reply that finished at once.
