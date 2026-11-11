#pragma once
#include <cstdint>
#include <string>
namespace multilogin {
// Configuration only. No browser API spoofing until integrated into Chromium.
struct Identity {
    std::string profile_id, locale, timezone, accept_language;
    int cpu_cores=4, memory_gb=4, screen_width=1080, screen_height=2400;
    std::uint64_t canvas_seed=0, audio_seed=0, webgl_seed=0;
};
Identity GenerateIdentity(const std::string& profile_id, std::uint64_t persistent_seed, const std::string& country);
}  // namespace multilogin
