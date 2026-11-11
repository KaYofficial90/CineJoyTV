from pathlib import Path

root = Path("app/src/main/java/com/multilogin/browser")

def patch(filename, changes):
    f = root / filename
    original = f.read_text()
    updated = original
    for before, after in changes:
        if before not in updated:
            raise RuntimeError("Expected anchor missing: " + filename + " " + repr(before[:50]))
        updated = updated.replace(before, after, 1)
    f.write_text(updated)

patch("BrowserProfile.java", [
    ("import java.util.UUID;", "import java.util.UUID;\nimport java.security.SecureRandom;"),
    ("    public long lastOpened = 0;",
     """    public long lastOpened = 0;
    public int fingerprintSeed = new SecureRandom().nextInt(Integer.MAX_VALUE - 1) + 1;
    public boolean fingerprintEnabled = true;
    public String fingerprintLanguage = "Device";
    public String fingerprintTimezone = "Device";
    public int fingerprintCores = 4;
    public int fingerprintMemory = 4;"""),
    ("        copy.userAgent = userAgent;",
     """        copy.userAgent = userAgent;
        copy.fingerprintEnabled = fingerprintEnabled;
        copy.fingerprintLanguage = fingerprintLanguage;
        copy.fingerprintTimezone = fingerprintTimezone;
        copy.fingerprintCores = fingerprintCores;
        copy.fingerprintMemory = fingerprintMemory;"""),
    ('        json.put("lastOpened", lastOpened);',
     """        json.put("lastOpened", lastOpened);
        json.put("fingerprintSeed", fingerprintSeed);
        json.put("fingerprintEnabled", fingerprintEnabled);
        json.put("fingerprintLanguage", fingerprintLanguage);
        json.put("fingerprintTimezone", fingerprintTimezone);
        json.put("fingerprintCores", fingerprintCores);
        json.put("fingerprintMemory", fingerprintMemory);"""),
    ('        profile.lastOpened = json.optLong("lastOpened", 0L);',
     """        profile.lastOpened = json.optLong("lastOpened", 0L);
        profile.fingerprintSeed = json.optInt("fingerprintSeed", profile.fingerprintSeed);
        profile.fingerprintEnabled = json.optBoolean("fingerprintEnabled", true);
        profile.fingerprintLanguage = json.optString("fingerprintLanguage", "Device");
        profile.fingerprintTimezone = json.optString("fingerprintTimezone", "Device");
        profile.fingerprintCores = json.optInt("fingerprintCores", 4);
        profile.fingerprintMemory = json.optInt("fingerprintMemory", 4);"""),
])

patch("ProfileEditorActivity.java", [
    ("import android.widget.EditText;", "import android.widget.EditText;\nimport android.widget.CheckBox;"),
    ("    private Spinner proxyType;",
     "    private Spinner proxyType, language, timezone, cores, memory;\n    private CheckBox experimental;"),
    ('        TextView note = Ui.text(this,"Changing the user agent does NOT spoof WebGL, Canvas, device hardware, timezone or the entire browser fingerprint.",12,Ui.MUTED,false);\n        root.addView(note,Ui.margins(this,-1,-2,0,10,0,20));',
     """        TextView identitySection = Ui.text(this,"EXPERIMENTAL BROWSER IDENTITY",12,Ui.ACCENT,true);
        root.addView(identitySection,Ui.margins(this,-1,-2,0,24,0,7));
        experimental = new CheckBox(this);
        experimental.setText("Enable stable per-profile JavaScript identity");
        experimental.setTextColor(Ui.TEXT);
        experimental.setChecked(profile.fingerprintEnabled);
        root.addView(experimental);
        language = select(root,"Reported browser language",new String[]{"Device","en-US","en-GB","fr-FR","de-DE","ja-JP"},profile.fingerprintLanguage);
        timezone = select(root,"Reported Intl timezone",new String[]{"Device","Africa/Lagos","Europe/London","Europe/Paris","Europe/Berlin","America/New_York","Asia/Tokyo"},profile.fingerprintTimezone);
        cores = select(root,"Reported CPU threads",new String[]{"2","4","8"},String.valueOf(profile.fingerprintCores));
        memory = select(root,"Reported memory (GB)",new String[]{"2","4","8"},String.valueOf(profile.fingerprintMemory));
        TextView note = Ui.text(this,"Each profile has a persistent Canvas variation. These JavaScript-only overrides do NOT spoof native WebGL, HTTP headers, WebRTC, hardware or DNS. This is NOT GoLogin-equivalent.",12,Ui.MUTED,false);
        root.addView(note,Ui.margins(this,-1,-2,0,10,0,20));"""),
    ("    private EditText field(",
     """    private Spinner select(LinearLayout root,String title,String[] choices,String value) {
        TextView label = Ui.text(this,title,13,Ui.MUTED,true);
        root.addView(label,Ui.margins(this,-1,-2,0,11,0,7));
        Spinner result = new Spinner(this);
        result.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));
        int index = 0;
        for (int i=0;i<choices.length;i++) if (choices[i].equals(value)) { index=i; break; }
        result.setSelection(index);
        root.addView(result,Ui.margins(this,-1,54,0,0,0,3));
        return result;
    }

    private EditText field("""),
    ("        profile.userAgent = agent.getText().toString().trim();",
     """        profile.userAgent = agent.getText().toString().trim();
        profile.fingerprintEnabled = experimental.isChecked();
        profile.fingerprintLanguage = language.getSelectedItem().toString();
        profile.fingerprintTimezone = timezone.getSelectedItem().toString();
        profile.fingerprintCores = Integer.parseInt(cores.getSelectedItem().toString());
        profile.fingerprintMemory = Integer.parseInt(memory.getSelectedItem().toString());"""),
])

patch("MainActivity.java", [
    ("        card.addView(link,Ui.margins(this,-1,-2,0,7,0,0));",
     """        card.addView(link,Ui.margins(this,-1,-2,0,7,0,0));
        TextView identity = Ui.text(this,profile.fingerprintEnabled
            ? "Experimental identity  #" + Integer.toHexString(profile.fingerprintSeed)
            : "Fingerprint override disabled",11,Ui.MUTED,false);
        card.addView(identity,Ui.margins(this,-1,-2,0,5,0,0));"""),
])

patch("BrowserActivity.java", [
    ("import android.os.Message;", "import android.os.Message;\nimport java.util.Collections;"),
    ("    private boolean shuttingDown;", "    private boolean shuttingDown;\n    private boolean identityReady;"),
    ("        applyProxyAndStart();", "        if (identityReady) applyProxyAndStart();"),
    ('        WebViewCompat.setProfile(browser,"mlb_" + profile.id);',
     """        WebViewCompat.setProfile(browser,"mlb_" + profile.id);
        if (profile.fingerprintEnabled) {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                error("This WebView lacks document-start identity support. Update Android System WebView or disable experimental identity.");
                return;
            }
            try {
                WebViewCompat.addDocumentStartJavaScript(browser,
                    FingerprintScript.build(profile),Collections.singleton("*"));
            } catch (Exception unsupported) {
                error("Unable to enable experimental identity: " + unsupported.getMessage());
                return;
            }
        }"""),
    ("        updateNavigationButtons();\n    }\n\n    private Button smallButton",
     "        updateNavigationButtons();\n        identityReady = true;\n    }\n\n    private Button smallButton"),
    ("        footnote.setGravity(Gravity.CENTER);",
     """        footnote.setText(footnote.getText() + (profile.fingerprintEnabled ? "  ·  EXPERIMENTAL JS IDENTITY" : ""));
        footnote.setGravity(Gravity.CENTER);"""),
])

patch_file = Path("app/build.gradle")
build_src = patch_file.read_text()
assert "versionCode 1" in build_src and "versionName '1.0.0'" in build_src
patch_file.write_text(build_src.replace("versionCode 1","versionCode 2").replace("versionName '1.0.0'","versionName '1.1.0'"))

(root / "FingerprintScript.java").write_text(r'''package com.multilogin.browser;

import org.json.JSONObject;

/** JavaScript-only WebView identity overrides. Not full browser fingerprint isolation. */
public final class FingerprintScript {
    private FingerprintScript() { }
    public static String build(BrowserProfile p) {
        return SCRIPT.replace("__FP_SEED__",Integer.toString(p.fingerprintSeed))
            .replace("__FP_LANGUAGE__",JSONObject.quote(p.fingerprintLanguage))
            .replace("__FP_TIMEZONE__",JSONObject.quote(p.fingerprintTimezone))
            .replace("__FP_CORES__",Integer.toString(p.fingerprintCores))
            .replace("__FP_MEMORY__",Integer.toString(p.fingerprintMemory));
    }
    private static final String SCRIPT = """
        (() => {
          'use strict';
          const seed = __FP_SEED__, locale = __FP_LANGUAGE__;
          const zone = __FP_TIMEZONE__, cores = __FP_CORES__, memory = __FP_MEMORY__;
          if (globalThis.__mlbExperimentalIdentityLoaded) return;
          Object.defineProperty(globalThis,'__mlbExperimentalIdentityLoaded',{value:true});
          function spoof(o,k,v) {
            try { Object.defineProperty(o,k,{configurable:true,enumerable:true,get:()=>v}); }
            catch (_) {}
          }
          if (locale !== 'Device') {
            spoof(Navigator.prototype,'language',locale);
            spoof(Navigator.prototype,'languages',Object.freeze([locale]));
          }
          spoof(Navigator.prototype,'hardwareConcurrency',cores);
          spoof(Navigator.prototype,'deviceMemory',memory);
          if (zone !== 'Device' && globalThis.Intl && Intl.DateTimeFormat) {
            const original = Intl.DateTimeFormat.prototype.resolvedOptions;
            try {
              Intl.DateTimeFormat.prototype.resolvedOptions = function () {
                const result = original.call(this);
                if (result && result.timeZone) result.timeZone = zone;
                return result;
              };
            } catch (_) {}
          }
          // Stable per-profile Canvas perturbation. Native GPU fingerprints are unchanged.
          const cp = globalThis.HTMLCanvasElement && HTMLCanvasElement.prototype;
          const cx = globalThis.CanvasRenderingContext2D && CanvasRenderingContext2D.prototype;
          if (!cp || !cx) return;
          const getData = cx.getImageData, putData = cx.putImageData;
          const originalURL = cp.toDataURL, originalBlob = cp.toBlob;
          if (!getData || !putData) return;
          function jitter(data,w,h) {
            const count = Math.floor(data.length/4);
            if (!count) return;
            const pixel = (((seed*2654435761)>>>0)+w*31+h*17)%count;
            const i = pixel*4;
            if (data[i+3] !== 0) data[i+(seed%3)] = (data[i+(seed%3)]+1+(seed%2))&255;
          }
          try {
            cx.getImageData = function(sx,sy,sw,sh,...rest) {
              const image = getData.call(this,sx,sy,sw,sh,...rest);
              jitter(image.data,image.width,image.height);
              return image;
            };
            function copyCanvas(source) {
              if (!source.width || !source.height || source.width*source.height>16000000) return null;
              const copy = document.createElement('canvas');
              copy.width=source.width;copy.height=source.height;
              const ctx = copy.getContext('2d');
              if (!ctx) return null;
              ctx.drawImage(source,0,0);
              const image = getData.call(ctx,0,0,copy.width,copy.height);
              jitter(image.data,image.width,image.height);
              putData.call(ctx,image,0,0);
              return copy;
            }
            cp.toDataURL = function (...args) {
              try {
                const copy = copyCanvas(this);
                if (copy) return originalURL.apply(copy,args);
              } catch (_) {}
              return originalURL.apply(this,args);
            };
            if (typeof originalBlob === 'function')
              cp.toBlob = function (...args) {
                try {
                  const copy = copyCanvas(this);
                  if (copy) return originalBlob.apply(copy,args);
                } catch (_) {}
                return originalBlob.apply(this,args);
              };
          } catch (_) {}
        })();
        """;
}
''');
print("Identity upgrade applied.")
