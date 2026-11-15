#!/usr/bin/env python3
"""Generate bundled fa-IR neural MP3 prompts using Azure Speech.

Required environment variables:
  AZURE_SPEECH_KEY       Speech resource key
  AZURE_SPEECH_REGION    Resource region (e.g. westeurope)
Optional:
  AZURE_SPEECH_VOICE     fa-IR-FaridNeural (default) or fa-IR-DilaraNeural

No API key is stored in the repository or APK.
"""
from __future__ import annotations
import hashlib
import os
import sys
import time
import urllib.error
import urllib.request
import xml.sax.saxutils as xml
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "tools" / "audio-lines.tsv"
OUT = ROOT / "app" / "src" / "main" / "assets" / "audio"
KEY = os.environ.get("AZURE_SPEECH_KEY", "").strip()
REGION = os.environ.get("AZURE_SPEECH_REGION", "").strip()
VOICE = os.environ.get("AZURE_SPEECH_VOICE", "fa-IR-FaridNeural").strip()

if not KEY or not REGION:
    sys.exit("Missing AZURE_SPEECH_KEY or AZURE_SPEECH_REGION. Create an Azure AI Speech resource and set both environment variables.")
if VOICE not in {"fa-IR-FaridNeural", "fa-IR-DilaraNeural"}:
    sys.exit("Unsupported voice. Choose fa-IR-FaridNeural or fa-IR-DilaraNeural.")

endpoint = f"https://{REGION}.tts.speech.microsoft.com/cognitiveservices/v1"
OUT.mkdir(parents=True, exist_ok=True)
rows = []
for raw in MANIFEST.read_text(encoding="utf-8").splitlines():
    if not raw.strip():
        continue
    try:
        digest, text = raw.split("\t", 1)
    except ValueError:
        sys.exit(f"Invalid manifest row: {raw[:80]}")
    expected = hashlib.sha256(text.encode("utf-8")).hexdigest()
    if digest != expected:
        sys.exit(f"Hash mismatch in audio-lines.tsv for: {text[:50]}")
    rows.append((digest, text))

for index, (digest, spoken_text) in enumerate(rows, 1):
    target = OUT / f"{digest}.mp3"
    escaped = xml.escape(spoken_text)
    ssml = (
        '<speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" '
        'xml:lang="fa-IR">'
        f'<voice name="{VOICE}"><prosody rate="-5%" pitch="0%">'
        f'{escaped}</prosody></voice></speak>'
    ).encode("utf-8")
    request = urllib.request.Request(
        endpoint,
        data=ssml,
        headers={
            "Ocp-Apim-Subscription-Key": KEY,
            "Content-Type": "application/ssml+xml",
            "X-Microsoft-OutputFormat": "audio-24khz-48kbitrate-mono-mp3",
            "User-Agent": "RescueGuide-AudioAssetBuilder",
        },
        method="POST",
    )
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                audio = response.read()
            if len(audio) < 1024:
                raise RuntimeError("Speech service returned an unexpectedly small audio file")
            temp = target.with_suffix(".mp3.tmp")
            temp.write_bytes(audio)
            temp.replace(target)
            print(f"[{index}/{len(rows)}] {target.name} ({len(audio)} bytes)")
            break
        except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, RuntimeError) as exc:
            if attempt == 2:
                sys.exit(f"Failed generating prompt {index}: {exc}")
            time.sleep(2 ** attempt)

# Remove obsolete audio files that are no longer present in the active prompt catalog.
valid = {f"{digest}.mp3" for digest, _ in rows}
for existing in OUT.glob("*.mp3"):
    if existing.name not in valid:
        existing.unlink()
print(f"Generated {len(rows)} Persian neural voice prompts with {VOICE}.")
