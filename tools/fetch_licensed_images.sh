#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/res/drawable-nodpi"
mkdir -p "$DEST"
fetch_one() {
  local name="$1" filename="$2" kind="$3"
  local target="$DEST/$name"
  local url="https://commons.wikimedia.org/wiki/Special:FilePath/$filename"
  curl --fail --location --retry 1 --retry-delay 1 --connect-timeout 10 --max-time 30 \
    --user-agent "RescueGuide/0.34.0 licensed image fetcher" "$url" -o "$target"
  local info
  info="$(file -b "$target")"
  [[ "$info" == *"$kind"* ]] || { echo "Invalid image type for $name: $info" >&2; exit 1; }
}
export DEST
export -f fetch_one
cat <<'ASSETS' | xargs -r -P 4 -n 3 bash -c 'fetch_one "$@"' _
guide_adult_airway.png CPR_Adult_Airway.png PNG
guide_adult_compressions.png CPR_Adult_Chest_Compression.png PNG
guide_adult_breathing.jpg Insulfation.jpg JPEG
guide_child_breathing.png CPR_Child_Blow_In_Air.png PNG
guide_child_compressions.png CPR_Child_Chest_Compression.png PNG
guide_infant_compressions.png CPR_Infant_Chest_Compression.png PNG
guide_infant_airway.png CPR_Infant_Closed_vs_Open_Airway.png PNG
guide_infant_breathing.png CPR_Infant_Mouth_To_Nose.png PNG
guide_infant_breathing_check.png CPR_Infant_Look_Listen_%26_Feel.png PNG
guide_aed_pad_position.png AED_pad_position.png PNG
guide_recovery_position.jpg Recovery_position.jpg JPEG
guide_choking_back_blows.jpg Back_blows_%28back_slaps%29_against_choking_for_adult_people.jpg JPEG
guide_abdominal_thrusts.jpg AbdominalThrust_HeimlichManoeuvre.jpg JPEG
ASSETS
count="$(find "$DEST" -maxdepth 1 -type f -name 'guide_*' | wc -l)"
[[ "$count" -eq 13 ]] || { echo "Expected 13 images, found $count" >&2; exit 1; }
echo "Fetched $count license-cleared image assets."
