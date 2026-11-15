#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/res/drawable-nodpi"
mkdir -p "$DEST"
fetch() {
  local name="$1" url="$2" kind="$3"
  local out="$DEST/$name"
  curl --fail --location --retry 3 --retry-delay 2 --connect-timeout 15 --max-time 45 --user-agent "RescueGuide/0.34.0 licensed image fetcher" "$url" -o "$out"
  local info
  info="$(file -b "$out")"
  [[ "$info" == *"$kind"* ]] || { echo "Invalid image type for $name: $info" >&2; exit 1; }
}
# Unmodified originals. Author, source page and license are documented in docs/IMAGE_ASSET_LICENSES.md.
fetch guide_adult_airway.png "https://commons.wikimedia.org/wiki/Special:FilePath/CPR_Adult_Airway.png" "PNG image data"
fetch guide_adult_compressions.png "https://commons.wikimedia.org/wiki/Special:FilePath/CPR_Adult_Chest_Compression.png" "PNG image data"
fetch guide_adult_breathing.jpg "https://commons.wikimedia.org/wiki/Special:FilePath/Insulfation.jpg" "JPEG image data"
fetch guide_child_breathing.png "https://commons.wikimedia.org/wiki/Special:FilePath/CPR_Child_Blow_In_Air.png" "PNG image data"
fetch guide_child_compressions.png "https://commons.wikimedia.org/wiki/Special:FilePath/CPR_Child_Chest_Compression.png" "PNG image data"
fetch guide_infant_compressions.png "https://commons.wikimedia.org/wiki/Special:FilePath/CPR_Infant_Chest_Compression.png" "PNG image data"
fetch guide_infant_airway.png "https://commons.wikimedia.org/wiki/Special:FilePath/CPR_Infant_Closed_vs_Open_Airway.png" "PNG image data"
fetch guide_infant_breathing.png "https://commons.wikimedia.org/wiki/Special:FilePath/CPR_Infant_Mouth_To_Nose.png" "PNG image data"
fetch guide_infant_breathing_check.png "https://commons.wikimedia.org/wiki/Special:FilePath/CPR_Infant_Look_Listen_%26_Feel.png" "PNG image data"
fetch guide_aed_pad_position.png "https://commons.wikimedia.org/wiki/Special:FilePath/AED_pad_position.png" "PNG image data"
fetch guide_recovery_position.jpg "https://commons.wikimedia.org/wiki/Special:FilePath/Recovery_position.jpg" "JPEG image data"
fetch guide_choking_back_blows.jpg "https://commons.wikimedia.org/wiki/Special:FilePath/Back_blows_%28back_slaps%29_against_choking_for_adult_people.jpg" "JPEG image data"
fetch guide_abdominal_thrusts.jpg "https://commons.wikimedia.org/wiki/Special:FilePath/AbdominalThrust_HeimlichManoeuvre.jpg" "JPEG image data"
echo "Fetched $(find "$DEST" -maxdepth 1 -type f -name 'guide_*' | wc -l) licensed images."
