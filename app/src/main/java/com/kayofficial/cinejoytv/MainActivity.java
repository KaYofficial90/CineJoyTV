package com.kayofficial.cinejoytv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

    private static final String HOME_URL = "https://cinejoy.pk/";
    private FrameLayout root;
    private WebView webView;
    private ProgressBar progressBar;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersiveMode();
        buildUi();
        configureWebView();
        if (savedInstanceState == null) webView.loadUrl(HOME_URL);
        else webView.restoreState(savedInstanceState);
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        root.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8);
        progressParams.gravity = android.view.Gravity.TOP;
        root.addView(progressBar, progressParams);
        setContentView(root);
        webView.requestFocus();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        String ua = settings.getUserAgentString();
        if (ua != null) settings.setUserAgentString(ua.replace("; wv", "").replace("Version/4.0 ", ""));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.setSafeBrowsingEnabled(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectTvNavigation();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                root.addView(customView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                enterImmersiveMode();
            }

            @Override
            public void onHideCustomView() { hideCustomView(); }
        });
    }

    private void injectTvNavigation() {
        String js = "(function(){if(window.__cinejoyTvNavInstalled)return;window.__cinejoyTvNavInstalled=true;" +
                "function visible(el){var r=el.getBoundingClientRect();var s=getComputedStyle(el);return r.width>0&&r.height>0&&s.visibility!=='hidden'&&s.display!=='none';}" +
                "function items(){var q='a,button,input,select,textarea,[role=button],[onclick],[tabindex]';return Array.from(document.querySelectorAll(q)).filter(visible);}" +
                "function prep(){items().forEach(function(e){if(!e.hasAttribute('tabindex'))e.setAttribute('tabindex','0');e.style.outlineOffset='3px';});}" +
                "window.__tvMoveFocus=function(dir){prep();var es=items();if(!es.length){window.scrollBy(0,dir==='up'?-260:dir==='down'?260:0);return;}var a=document.activeElement;if(!es.includes(a)){es[0].focus();es[0].scrollIntoView({block:'center',inline:'center'});return;}var ar=a.getBoundingClientRect(),ax=ar.left+ar.width/2,ay=ar.top+ar.height/2,best=null,bscore=1e20;es.forEach(function(e){if(e===a)return;var r=e.getBoundingClientRect(),x=r.left+r.width/2,y=r.top+r.height/2,dx=x-ax,dy=y-ay;var ok=(dir==='left'&&dx<0)||(dir==='right'&&dx>0)||(dir==='up'&&dy<0)||(dir==='down'&&dy>0);if(!ok)return;var primary=(dir==='left'||dir==='right')?Math.abs(dx):Math.abs(dy);var secondary=(dir==='left'||dir==='right')?Math.abs(dy):Math.abs(dx);var score=primary+secondary*2.2;if(score<bscore){bscore=score;best=e;}});if(best){best.focus();best.scrollIntoView({block:'center',inline:'center',behavior:'smooth'});}else{window.scrollBy({top:dir==='up'?-260:dir==='down'?260:0,left:dir==='left'?-260:dir==='right'?260:0,behavior:'smooth'});}};" +
                "window.__tvActivate=function(){var a=document.activeElement;if(a&&a!==document.body){a.click();}else{var es=items();if(es[0]){es[0].focus();es[0].click();}}};prep();var s=document.createElement('style');s.innerHTML='*:focus{outline:3px solid white!important;}';document.head.appendChild(s);})();";
        webView.evaluateJavascript(js, null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN || customView != null) return super.dispatchKeyEvent(event);
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP: webView.evaluateJavascript("window.__tvMoveFocus&&window.__tvMoveFocus('up')", null); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: webView.evaluateJavascript("window.__tvMoveFocus&&window.__tvMoveFocus('down')", null); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: webView.evaluateJavascript("window.__tvMoveFocus&&window.__tvMoveFocus('left')", null); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: webView.evaluateJavascript("window.__tvMoveFocus&&window.__tvMoveFocus('right')", null); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: webView.evaluateJavascript("window.__tvActivate&&window.__tvActivate()", null); return true;
            default: return super.dispatchKeyEvent(event);
        }
    }

    private void hideCustomView() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        webView.setVisibility(View.VISIBLE);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        enterImmersiveMode();
        webView.requestFocus();
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) hideCustomView();
        else if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }
}
