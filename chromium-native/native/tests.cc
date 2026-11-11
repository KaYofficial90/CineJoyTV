#include "profile_identity.h"
#include <cassert>
#include <iostream>
#include <stdexcept>
int main() {
 const auto a=multilogin::GenerateIdentity("a",42,"NG");
 const auto again=multilogin::GenerateIdentity("a",42,"NG");
 const auto b=multilogin::GenerateIdentity("b",42,"NG");
 assert(a.locale=="en-NG"&&a.timezone=="Africa/Lagos");
 assert(a.canvas_seed==again.canvas_seed&&a.audio_seed==again.audio_seed);
 assert(a.canvas_seed!=b.canvas_seed&&a.webgl_seed!=b.webgl_seed);
 const auto jp=multilogin::GenerateIdentity("c",42,"JP");
 assert(jp.locale=="ja-JP"&&jp.timezone=="Asia/Tokyo");
 bool invalid=false;
 try { multilogin::GenerateIdentity("",42,"NG"); }catch(const std::invalid_argument&){invalid=true;}
 assert(invalid);
 std::cout<<"Native identity settings tests passed; no engine integrations yet.\n";
}
