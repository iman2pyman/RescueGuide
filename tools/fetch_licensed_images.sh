#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/res/drawable-nodpi"
mkdir -p "$DEST"
fetch() {
  local name="$1" url="$2" kind="$3"
  local out="$DEST/$name"
  curl --fail --location --retry 4 --retry-delay 2 --user-agent "RescueGuide/0.34.0 licensed image fetcher" "$url" -o "$out"
  local info
  info="$(file -b "$out")"
  [[ "$info" == *"$kind"* ]] || { echo "Invalid image type for $name: $info" >&2; exit 1; }
}
# Unmodified originals. Author, source page and license are documented in docs/IMAGE_ASSET_LICENSES.md.
fetch guide_adult_airway.png "https://upload.wikimedia.org/wikipedia/commons/a/aa/CPR_Adult_Airway.png" "PNG image data"
fetch guide_adult_compressions.png "https://upload.wikimedia.org/wikipedia/commons/9/9a/CPR_Adult_Chest_Compression.png" "PNG image data"
fetch guide_adult_breathing.jpg "https://upload.wikimedia.org/wikipedia/commons/9/94/Insulfation.jpg" "JPEG image data"
fetch guide_child_breathing.png "https://upload.wikimedia.org/wikipedia/commons/2/27/CPR_Child_Blow_In_Air.png" "PNG image data"
fetch guide_child_compressions.png "https://upload.wikimedia.org/wikipedia/commons/e/e8/CPR_Child_Chest_Compression.png" "PNG image data"
fetch guide_infant_compressions.png "https://upload.wikimedia.org/wikipedia/commons/8/8a/CPR_Infant_Chest_Compression.png" "PNG image data"
fetch guide_infant_airway.png "https://upload.wikimedia.org/wikipedia/commons/4/43/CPR_Infant_Closed_vs_Open_Airway.png" "PNG image data"
fetch guide_infant_breathing.png "https://upload.wikimedia.org/wikipedia/commons/f/f4/CPR_Infant_Mouth_To_Nose.png" "PNG image data"
fetch guide_infant_breathing_check.png "https://upload.wikimedia.org/wikipedia/commons/3/36/CPR_Infant_Look_Listen_%26_Feel.png" "PNG image data"
fetch guide_aed_pad_position.png "https://upload.wikimedia.org/wikipedia/commons/9/94/AED_pad_position.png" "PNG image data"
fetch guide_recovery_position.jpg "https://upload.wikimedia.org/wikipedia/commons/3/34/Recovery_position.jpg" "JPEG image data"
fetch guide_choking_back_blows.jpg "https://upload.wikimedia.org/wikipedia/commons/1/16/Back_blows_%28back_slaps%29_against_choking_for_adult_people.jpg" "JPEG image data"
fetch guide_abdominal_thrusts.jpg "https://upload.wikimedia.org/wikipedia/commons/f/fb/AbdominalThrust_HeimlichManoeuvre.jpg" "JPEG image data"
echo "Fetched $(find "$DEST" -maxdepth 1 -type f -name 'guide_*' | wc -l) licensed images."
