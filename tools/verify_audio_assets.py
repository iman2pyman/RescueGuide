#!/usr/bin/env python3
"""Verify every checked-in Persian prompt maps to a valid SHA-256-named MP3."""
from __future__ import annotations
import hashlib
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
manifest = root / "tools" / "audio-lines.tsv"
audio_dir = root / "app" / "src" / "main" / "assets" / "audio"
errors: list[str] = []
records = manifest.read_text(encoding="utf-8").splitlines()
for line_no, line in enumerate(records, 1):
    if "\t" not in line:
        errors.append(f"line {line_no}: missing tab")
        continue
    expected, text = line.split("\t", 1)
    actual = hashlib.sha256(text.encode("utf-8")).hexdigest()
    if expected != actual:
        errors.append(f"line {line_no}: hash mismatch")
        continue
    asset = audio_dir / f"{expected}.mp3"
    if not asset.is_file() or asset.stat().st_size == 0:
        errors.append(f"line {line_no}: missing/empty {asset.name}")
        continue
    probe = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(asset)],
        capture_output=True, text=True,
    )
    if probe.returncode != 0 or not probe.stdout.strip():
        errors.append(f"line {line_no}: invalid MP3 {asset.name}")

if errors:
    raise SystemExit("\n".join(errors))
print(f"AUDIO_ASSETS_OK records={len(records)} unique_mp3={len(list(audio_dir.glob('*.mp3')))}")
