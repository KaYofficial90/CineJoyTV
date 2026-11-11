# MultiLogin Chromium — native build foundation

**Status: NOT a completed anti-detect browser.** This branch prepares a native identity-configuration component and an upstream Chromium Android build, and requires a large Linux runner. There is no finished GoLogin-equivalent APK here.

## What works
- Native C++ stable, per-profile identity settings model, with tests.
- Build script for upstream Chromium Android ARM64 baseline.
- Self-hosted GitHub Actions build job (at least 100 GiB FREE storage, preferably >=16 GiB RAM).

## What remains
- Native Chromium Android profile isolation / lifecycle.
- Native network/WebRTC proxy integration per profile.
- Native browser engine integration of identity settings into Blink/GPU/Android bindings and worker contexts.
- Comprehensive Canvas/WebGL/audio/font/screen/timezone consistency and leak testing.
- End-user APK packaging and physical phone validation.

These are not cosmetic features and are not solved by a baseline Chromium build. **Native identity model settings do not change browser fingerprints until wired into Chromium.**

Read the official Chromium Android build guide: https://chromium.googlesource.com/chromium/src/+/HEAD/docs/android_build_instructions.md

For a suitable Ubuntu Linux machine, install Chromium build dependencies and run:
```bash
cmake -S chromium-native/native -B /tmp/mlnative-build
cmake --build /tmp/mlnative-build -j2
ctest --test-dir /tmp/mlnative-build --output-on-failure
bash chromium-native/build-chromium-android.sh
```
The result is STOCK Chromium, not MultiLogin. The runner workflow requires `self-hosted,linux,x64,chromium-large` labels; standard GitHub Actions runners have only ~14GB.
