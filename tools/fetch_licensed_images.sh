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
    --user-agent "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/130.0 Safari/537.36" \\
  --referer "https://www.anzcor.org/" -H "Accept: application/pdf,*/*" "$url" -o "$target"
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
guide_infant_airway.png CPR_Infant_Closed_vs_Open_Airway.png PNG
guide_infant_breathing.png CPR_Infant_Mouth_To_Nose.png PNG
guide_infant_breathing_check.png CPR_Infant_Look_Listen_%26_Feel.png PNG
guide_aed_pad_position.png AED_pad_position.png PNG
guide_recovery_position.jpg Recovery_position.jpg JPEG
guide_choking_back_blows.jpg Back_blows_%28back_slaps%29_against_choking_for_adult_people.jpg JPEG
guide_abdominal_thrusts.jpg AbdominalThrust_HeimlichManoeuvre.jpg JPEG
ASSETS

# The BruceBlaus infant compression graphic depicts the obsolete two-finger method,
# so it is deliberately NOT bundled. These two-thumb / one-hand photos are CC BY-NC 4.0.
sudo apt-get update -y
sudo apt-get install -y poppler-utils
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
curl --fail --location --retry 1 --connect-timeout 10 --max-time 60 \
  --user-agent "RescueGuide/0.34.0 licensed image fetcher" \
  "https://www.anzcor.org/assets/generated-anzcor-guideline-pdfs/guideline-12-1-paediatric-basic-life-support-254.pdf?v=1790644122" \
  -o "$TMP/anzcor-guideline-12-1.pdf"
pdfimages -f 12 -l 12 -j "$TMP/anzcor-guideline-12-1.pdf" "$TMP/two-thumb"
pdfimages -f 13 -l 13 -j "$TMP/anzcor-guideline-12-1.pdf" "$TMP/one-hand"
thumb="$(find "$TMP" -maxdepth 1 -type f -name 'two-thumb-*' | sort | head -n 1)"
onehand="$(find "$TMP" -maxdepth 1 -type f -name 'one-hand-*' | sort | head -n 1)"
[[ -n "$thumb" && -n "$onehand" ]] || { echo "Could not extract the licensed infant compression figures." >&2; exit 1; }
[[ "$(file -b "$thumb")" == *"JPEG image data"* ]] || { echo "Unexpected two-thumb image format." >&2; exit 1; }
[[ "$(file -b "$onehand")" == *"JPEG image data"* ]] || { echo "Unexpected one-hand image format." >&2; exit 1; }
cp "$thumb" "$DEST/guide_infant_compressions.jpg"
cp "$onehand" "$DEST/guide_infant_compressions_onehand.jpg"
rm -f "$DEST/guide_infant_compressions.png"
count="$(find "$DEST" -maxdepth 1 -type f -name 'guide_*' | wc -l)"
[[ "$count" -eq 14 ]] || { echo "Expected 14 licensed images, found $count" >&2; exit 1; }
echo "Fetched $count licensed image assets; infant compressions use two-thumb and one-hand illustrations, not two-finger."
