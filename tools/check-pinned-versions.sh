#!/usr/bin/env bash
#
# Preflight for the NEUROFIT build.
#
# The author cannot reach Google's Maven repository from the machine where this
# project is written, so the versions pinned in gradle/libs.versions.toml cannot
# be verified there. They CAN be verified from a GitHub Actions runner, which is
# what this script does.
#
# For every pinned coordinate it fetches maven-metadata.xml, checks the pinned
# version is actually published, and on a miss prints the versions that DO exist
# so the fix is a single obvious edit rather than a guessing game.
#
# Exit codes: 0 = all pins exist (or metadata unreachable, which is not fatal),
#             1 = at least one pinned version does not exist.
#
set -uo pipefail

CATALOG="${1:-gradle/libs.versions.toml}"
BASE="https://dl.google.com/dl/android/maven2"
FAILED=0
UNREACHABLE=0

pin() {
  # pin <key> -> value of `key = "value"` from the [versions] block
  sed -n '/^\[versions\]/,/^\[/p' "$CATALOG" \
    | sed -n "s/^$1[[:space:]]*=[[:space:]]*\"\([^\"]*\)\".*/\1/p" | head -n 1
}

check() {
  local label="$1" path="$2" pinned="$3"
  if [ -z "$pinned" ]; then
    echo "  ?? $label: no pin found in $CATALOG (key missing)"
    return 0
  fi

  local meta
  if ! meta="$(curl -fsSL --max-time 45 --retry 3 --retry-delay 2 "$BASE/$path/maven-metadata.xml" 2>/dev/null)"; then
    echo "  ~~ $label: could not fetch metadata (network/policy). Skipping, not failing."
    UNREACHABLE=1
    return 0
  fi

  local versions
  versions="$(printf '%s' "$meta" | grep -o '<version>[^<]*</version>' | sed 's/<[^>]*>//g')"

  if printf '%s\n' "$versions" | grep -qxF "$pinned"; then
    echo "  OK $label = $pinned  (published)"
  else
    echo "  !! $label = $pinned  DOES NOT EXIST on Google Maven"
    echo "     newest published versions:"
    printf '%s\n' "$versions" | tail -n 12 | sed 's/^/       /'
    FAILED=1
  fi
}

echo "Verifying pinned versions in $CATALOG against Google Maven"
echo

check "AGP (agp)"                    "com/android/tools/build/gradle"        "$(pin agp)"
check "Compose BOM (composeBom)"     "androidx/compose/compose-bom"          "$(pin composeBom)"
check "core-ktx (coreKtx)"           "androidx/core/core-ktx"                "$(pin coreKtx)"
check "lifecycle (lifecycle)"        "androidx/lifecycle/lifecycle-runtime-ktx" "$(pin lifecycle)"
check "activity-compose"             "androidx/activity/activity-compose"    "$(pin activityCompose)"

echo
if [ "$FAILED" -ne 0 ]; then
  echo "One or more pinned versions do not exist. Edit gradle/libs.versions.toml to a version"
  echo "listed above and push again. Nothing else in the build needs to change."
  exit 1
fi
if [ "$UNREACHABLE" -ne 0 ]; then
  echo "Some metadata could not be fetched; those pins were not verified."
fi
echo "All reachable pins verified."
