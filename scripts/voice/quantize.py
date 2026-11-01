#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10"
# dependencies = ["onnxruntime==1.31.1", "onnx==1.23.2"]
# ///
"""Remake Darek's int8 voice from Piper's mc_speech model (ADR-0014, phase-1 section 3).

    uv run scripts/voice/quantize.py <work-dir>

Downloads the source model into <work-dir> unless it is already there, checks both hashes, and
fails if either differs. The output is what checkpoint V publishes as the release asset.
"""

import hashlib
import sys
import urllib.request
from pathlib import Path

SOURCE_URL = (
    "https://huggingface.co/rhasspy/piper-voices/resolve/main/"
    "pl/pl_PL/mc_speech/medium/pl_PL-mc_speech-medium.onnx"
)
SOURCE_SHA256 = "a6b043358bc81e6c111a5140606a21959ce7f34969b8b7207f62869787cc3907"
OUTPUT_NAME = "pl_PL-mc_speech-medium-int8.onnx"
OUTPUT_SHA256 = "4bca76cb9d30784bbfc06dcb7ced07fa67a2db1ea0c4a2d17bf5e06a06596e79"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def require(path: Path, expected: str) -> None:
    actual = sha256(path)
    if actual != expected:
        sys.exit(f"{path}: SHA-256 {actual}, expected {expected}")


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    work = Path(sys.argv[1])
    work.mkdir(parents=True, exist_ok=True)
    source = work / SOURCE_URL.rsplit("/", 1)[1]
    if not source.exists():
        urllib.request.urlretrieve(SOURCE_URL, source)
    require(source, SOURCE_SHA256)

    from onnxruntime.quantization import QuantType, quantize_dynamic

    output = work / OUTPUT_NAME
    quantize_dynamic(str(source), str(output), weight_type=QuantType.QUInt8)
    require(output, OUTPUT_SHA256)
    print(f"{output}  {OUTPUT_SHA256}")


if __name__ == "__main__":
    main()
