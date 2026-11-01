#!/usr/bin/env python3
"""Darek's Polish pronunciation rules, the host reference `speak/PolishPhonemes.kt` is ported from.

Written from Polish orthography (ADR-0014). eSpeak NG was only a comparison oracle on the host.

    python3 scripts/voice/g2p_reference.py "Co tam, mordo?"
    python3 scripts/voice/g2p_reference.py --ids <model.onnx.json> "Co tam, mordo?"
"""

import json
import re
import unicodedata

VOWELS = {"a", "ɛ", "i", "ɔ", "u", "ɨ", "ɔ̃", "ɛ̃"}
VOICED_TO_VOICELESS = {"b": "p", "d": "t", "ɡ": "k", "z": "s", "ʒ": "ʃ", "Ʒ": "ʃ", "ʑ": "ɕ", "dz": "ts", "ʣ": "ts", "dʒ": "tʃ", "dʑ": "tɕ", "v": "f", "ɣ": "x"}
VOICELESS_TO_VOICED = {v: k for k, v in VOICED_TO_VOICELESS.items() if k not in ("Ʒ", "ʣ")}
OBSTRUENTS = set(VOICED_TO_VOICELESS) | set(VOICELESS_TO_VOICED)
GLIDE_J = {"b", "m", "v"}
SOFT_BEFORE_VOWEL_I = {"b", "ɡ"}
CLITICS = {"się", "nie", "mi", "ci", "go", "mu", "ją", "je", "by", "że", "i", "a", "o", "u", "w", "z",
           "do", "na", "za", "po", "od", "ze", "we", "przy", "przez", "dla", "bez", "nad", "pod", "przed", "ku"}

PLAIN = {"a": "a", "e": "ɛ", "i": "i", "o": "ɔ", "u": "u", "ó": "u", "y": "ɨ", "b": "b", "c": "ts", "ć": "tɕ",
         "d": "d", "f": "f", "g": "ɡ", "h": "x", "j": "j", "k": "k", "l": "l", "ł": "w", "m": "m", "n": "n",
         "ń": "ɲ", "p": "p", "r": "r", "s": "s", "ś": "ɕ", "t": "t", "w": "v", "z": "z", "ź": "ʑ", "ż": "ʒ",
         "x": "ks", "q": "k", "v": "v", "ą": "ą", "ę": "ę"}
# dz is spoken as dʑ because eSpeak writes it so, and the voice learned eSpeak's phonemes.
DIGRAPHS = {"ch": "x", "cz": "tʃ", "sz": "ʃ", "rz": "Ʒ", "dż": "dʒ", "dź": "dʑ", "dz": "ʣ"}
SOFT_BEFORE_I = {"dz": "dʑ", "c": "tɕ", "s": "ɕ", "z": "ʑ", "n": "ɲ"}
SOFT_GLIDE = {"ɲ"}
LETTER_VOWELS = set("aeiouóyąę")

ONES = ["zero", "jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć"]
TEENS = ["dziesięć", "jedenaście", "dwanaście", "trzynaście", "czternaście", "piętnaście", "szesnaście",
         "siedemnaście", "osiemnaście", "dziewiętnaście"]
TENS = ["", "", "dwadzieścia", "trzydzieści", "czterdzieści", "pięćdziesiąt", "sześćdziesiąt",
        "siedemdziesiąt", "osiemdziesiąt", "dziewięćdziesiąt"]
HUNDREDS = ["", "sto", "dwieście", "trzysta", "czterysta", "pięćset", "sześćset", "siedemset", "osiemset",
            "dziewięćset"]


def number_words(n):
    if n < 10:
        return ONES[n]
    words = []
    if n >= 1000:
        k = n // 1000
        if k == 1:
            words.append("tysiąc")
        else:
            words.append(number_words(k))
            words.append("tysiące" if k % 10 in (2, 3, 4) and k % 100 not in (12, 13, 14) else "tysięcy")
        n %= 1000
    if n >= 100:
        words.append(HUNDREDS[n // 100])
        n %= 100
    if 10 <= n < 20:
        words.append(TEENS[n - 10])
    else:
        if n >= 20:
            words.append(TENS[n // 10])
        if n % 10:
            words.append(ONES[n % 10])
    return " ".join(words)


def letters_to_segments(word):
    segs = []
    i = 0
    while i < len(word):
        nxt = lambda k: word[i + k] if i + k < len(word) else ""
        matched = False
        for base, soft in SOFT_BEFORE_I.items():
            if word.startswith(base + "i", i):
                after = nxt(len(base) + 1)
                segs.append(soft)
                if after in LETTER_VOWELS:
                    if soft in SOFT_GLIDE:
                        segs.append("ʲ")
                    i += len(base) + 1
                else:
                    segs.append("i")
                    i += len(base) + 1
                matched = True
                break
        if matched:
            continue
        two = word[i:i + 2]
        if two in DIGRAPHS:
            segs.append(DIGRAPHS[two])
            i += 2
            continue
        ch = word[i]
        if ch == "i" and segs and segs[-1] not in VOWELS and nxt(1) in LETTER_VOWELS:
            segs.append("j" if segs[-1] == "b" or segs[-1] not in {"p", "f", "v", "m", "k", "ɡ"} else "ʲ")
            i += 1
            continue
        if ch == "i" and segs and segs[-1] in SOFT_BEFORE_VOWEL_I:
            segs.append("ʲ")
        if ch in PLAIN:
            segs.append(PLAIN[ch])
        i += 1
    return segs


def resolve_nasals(segs):
    out = []
    for k, s in enumerate(segs):
        if s not in ("ą", "ę"):
            out.append(s)
            continue
        vowel = "ɔ" if s == "ą" else "ɛ"
        nxt = next((x for x in segs[k + 1:] if x != "ʲ"), None)
        if nxt in ("p", "b"):
            out += [vowel, "m"]
        elif nxt in ("t", "d", "ts", "dz", "ʣ", "tʃ", "dʒ"):
            out += [vowel, "n"]
        elif nxt in ("k", "ɡ"):
            out += [vowel, "ŋ"]
        elif nxt in ("tɕ", "dʑ"):
            out += [vowel, "ɲ"]
        elif nxt in ("l", "w"):
            out.append(vowel)
        elif nxt is None and s == "ę":
            out.append("ɛ")
        else:
            out.append(vowel + "̃")
    return out


def assimilate(segs, devoice_end):
    segs = list(segs)
    idx = [k for k, s in enumerate(segs) if s != "ʲ"]
    if devoice_end:
        for k in reversed(idx):
            if segs[k] in VOICED_TO_VOICELESS:
                segs[k] = VOICED_TO_VOICELESS[segs[k]]
            elif segs[k] in OBSTRUENTS:
                continue
            else:
                break
    for a, b in zip(reversed(idx[:-1]), reversed(idx[1:])):
        x, y = segs[a], segs[b]
        transparent = y in ("v", "Ʒ")
        if x in OBSTRUENTS and y in OBSTRUENTS and not transparent:
            if y in VOICED_TO_VOICELESS and x in VOICELESS_TO_VOICED:
                segs[a] = VOICELESS_TO_VOICED[x]
            elif y in VOICELESS_TO_VOICED and x in VOICED_TO_VOICELESS:
                segs[a] = VOICED_TO_VOICELESS[x]
    for a, b in zip(idx[:-1], idx[1:]):
        x, y = segs[a], segs[b]
        if y in ("v", "Ʒ") and x in VOICELESS_TO_VOICED:
            segs[b] = VOICED_TO_VOICELESS[y]
    return segs


def stress(segs, word):
    vowel_at = [k for k, s in enumerate(segs) if s in VOWELS or s.endswith("̃")]
    if not vowel_at or (len(vowel_at) == 1 and word in CLITICS):
        return "".join(segs)
    marks = {vowel_at[-2] if len(vowel_at) >= 2 else vowel_at[0]: "ˈ"}
    if len(vowel_at) >= 4:
        marks[vowel_at[0]] = "ˌ"
    return "".join(marks.get(k, "") + s for k, s in enumerate(segs))


def word_segments(word):
    segs = resolve_nasals(letters_to_segments(word))
    return segs


def phonemize(text):
    text = re.sub(r"\d+", lambda m: " " + number_words(int(m.group())) + " ", text.lower())
    tokens = re.findall(r"[a-ząćęłńóśźż]+|[,.?!;:]", text)
    words = [(t, word_segments(t)) for t in tokens if t[0].isalpha()]
    out = []
    w = 0
    for t in tokens:
        if not t[0].isalpha():
            if out:
                out[-1] += t
            continue
        word, segs = words[w]
        following = words[w + 1][1] if w + 1 < len(words) else None
        if word in ("w", "z") and following:
            first = next((s for s in following if s != "ʲ"), "")
            segs = [VOICED_TO_VOICELESS[segs[0]]] if first in VOICELESS_TO_VOICED else segs
            out.append("".join(segs))
        else:
            out.append(stress(assimilate(segs, devoice_end=True), word))
        w += 1
    return " ".join(out).replace("Ʒ", "ʒ").replace("ʣ", "dʑ")


def phoneme_ids(phonemes, id_map):
    ids = [*id_map["^"], *id_map["_"]]
    for codepoint in unicodedata.normalize("NFD", phonemes):
        ids += [*id_map[codepoint], *id_map["_"]]
    return ids + id_map["$"]


if __name__ == "__main__":
    import sys

    args = sys.argv[1:]
    id_map = None
    if args[:1] == ["--ids"]:
        with open(args[1], encoding="utf-8") as f:
            id_map = json.load(f)["phoneme_id_map"]
        args = args[2:]
    for t in args:
        p = phonemize(t)
        print(t, "->", p)
        if id_map:
            print(" ".join(map(str, phoneme_ids(p, id_map))))
