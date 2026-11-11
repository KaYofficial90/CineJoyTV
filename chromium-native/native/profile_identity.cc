#include "profile_identity.h"
#include <array>
#include <stdexcept>
namespace multilogin {
namespace {
struct Region { const char *country,*locale,*timezone,*accept; };
constexpr std::array<Region,6> kRegions{{
 {"NG","en-NG","Africa/Lagos","en-NG,en;q=0.9"},
 {"US","en-US","America/New_York","en-US,en;q=0.9"},
 {"GB","en-GB","Europe/London","en-GB,en;q=0.9"},
 {"DE","de-DE","Europe/Berlin","de-DE,de;q=0.9,en;q=0.8"},
 {"FR","fr-FR","Europe/Paris","fr-FR,fr;q=0.9,en;q=0.8"},
 {"JP","ja-JP","Asia/Tokyo","ja-JP,ja;q=0.9,en;q=0.8"}
}};
std::uint64_t hash(const std::string& s) {
 std::uint64_t h=UINT64_C(14695981039346656037);
 for (unsigned char c: s) { h^=c; h*=UINT64_C(1099511628211); }
 return h;
}
std::uint64_t next(std::uint64_t& s) {
 std::uint64_t z=(s+=UINT64_C(0x9e3779b97f4a7c15));
 z=(z^(z>>30))*UINT64_C(0xbf58476d1ce4e5b9);
 z=(z^(z>>27))*UINT64_C(0x94d049bb133111eb);
 return z^(z>>31);
}
}
Identity GenerateIdentity(const std::string& id,std::uint64_t seed,const std::string& country) {
 if (id.empty()||seed==0) throw std::invalid_argument("Profile ID and seed must be non-empty/non-zero");
 const Region* r=nullptr;
 for(const auto& candidate:kRegions) if(country==candidate.country) {r=&candidate;break;}
 if(!r) throw std::invalid_argument("Unsupported country");
 std::uint64_t state=seed^hash(id);
 Identity out;out.profile_id=id;out.locale=r->locale;out.timezone=r->timezone;out.accept_language=r->accept;
 constexpr std::array<int,3> cores{{4,6,8}}, memory{{4,8,16}}, widths{{1080,1080,1440}}, heights{{2400,2340,3200}};
 out.cpu_cores=cores[next(state)%cores.size()];
 out.memory_gb=memory[next(state)%memory.size()];
 auto screen=next(state)%widths.size();out.screen_width=widths[screen];out.screen_height=heights[screen];
 out.canvas_seed=next(state);out.audio_seed=next(state);out.webgl_seed=next(state);
 return out;
}
}
