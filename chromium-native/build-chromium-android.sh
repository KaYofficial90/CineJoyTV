#!/usr/bin/env bash
# Upstream Chromium baseline only. Does NOT spoof fingerprints.
set -euo pipefail
WORKDIR="${WORKDIR:-$HOME/multilogin-chromium-build}"
mkdir -p "$WORKDIR"
FREE_KIB="$(df -Pk "$WORKDIR" | awk 'NR==2 {print $4}')"
[[ "$FREE_KIB" -ge $((100*1024*1024)) ]] || { echo "ERROR: Need >=100 GiB free storage" >&2; exit 20; }
[[ "$(uname -s)" == "Linux" && "$(uname -m)" == "x86_64" ]] || { echo "ERROR: Need x86-64 Linux" >&2; exit 21; }
MEM_KIB="$(awk '/^MemTotal:/ {print $2}' /proc/meminfo)"
[[ "$MEM_KIB" -ge $((8*1024*1024)) ]] || { echo "ERROR: Need >=8 GiB RAM" >&2; exit 22; }
if [[ ! -d "$WORKDIR/depot_tools/.git" ]]; then
 git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git "$WORKDIR/depot_tools"
fi
export PATH="$WORKDIR/depot_tools:$PATH"
cd "$WORKDIR"
mkdir -p chromium
cd chromium
if [[ ! -f .gclient ]]; then
 fetch --nohooks --no-history android
else
 gclient sync --no-history
fi
cd src
# Requires the official Chromium Android build dependencies already installed.
gclient runhooks
mkdir -p out/MultiLoginBaseline
cat > out/MultiLoginBaseline/args.gn <<'GN'
target_os = "android"
target_cpu = "arm64"
is_debug = false
is_component_build = false
symbol_level = 0
GN
gn gen out/MultiLoginBaseline
autoninja -C out/MultiLoginBaseline chrome_public_apk
mkdir -p "$WORKDIR/apk-output"
find out/MultiLoginBaseline/apks -type f -name '*.apk' -exec cp -v '{}' "$WORKDIR/apk-output" ';'
find "$WORKDIR/apk-output" -maxdepth 1 -type f -name '*.apk' | grep -q . || exit 24
echo "Built upstream Chromium baseline. Not MultiLogin."
