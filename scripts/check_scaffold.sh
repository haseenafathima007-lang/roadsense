#!/usr/bin/env bash
# Quick sanity check of the scaffold itself. Run from repo root.
set -u
fail=0
for f in AGENTS.md README.md .gitignore .github/workflows/ci.yml docs/ROADMAP.md docs/MODEL_GUIDE.md ai-service/app/config/thresholds.yaml; do
  [ -f "$f" ] || { echo "MISSING $f"; fail=1; }
done
for n in 00 01 02 03 04 05 06 07 08 09 10 11; do
  p=$(ls docs/prompts/phase-$n-*.md 2>/dev/null | head -1)
  [ -n "$p" ] || { echo "MISSING prompt phase $n"; fail=1; continue; }
  for k in "MASTER PROMPT" "VERIFICATION PROMPT" "EXPECTED OUTPUTS"; do
    grep -q "$k" "$p" || { echo "$p lacks $k"; fail=1; }
  done
done
[ -f docs/03_image_only_java_redesign.md ] || echo "NOTE: docs/03_image_only_java_redesign.md not added yet (you must add it)"
[ $fail -eq 0 ] && echo "scaffold OK" || exit 1
