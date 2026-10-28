package com.kayofficial.cinejoytv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends Activity {

    private static final String HOME_URL = "https://cinejoy.pk/";
    private static GeckoRuntime sRuntime;

    private GeckoView geckoView;
    private GeckoSession session;
    private boolean canGoBack = false;
    private boolean pageFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersiveMode();

        geckoView = new GeckoView(this);
        geckoView.setBackgroundColor(Color.BLACK);
        geckoView.setFocusable(true);
        geckoView.setFocusableInTouchMode(true);
        setContentView(geckoView);

        GeckoSessionSettings sessionSettings = new GeckoSessionSettings.Builder()
                .allowJavascript(true)
                .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
                .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
                .displayMode(GeckoSessionSettings.DISPLAY_MODE_STANDALONE)
                .suspendMediaWhenInactive(false)
                .useTrackingProtection(false)
                .build();

        session = new GeckoSession(sessionSettings);

        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onCanGoBack(GeckoSession session, boolean value) {
                canGoBack = value;
            }
        });

        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFullScreen(GeckoSession session, boolean fullScreen) {
                pageFullscreen = fullScreen;
                enterImmersiveMode();
            }

            @Override
            public void onFirstContentfulPaint(GeckoSession session) {
                geckoView.requestFocus();
            }

            @Override
            public void onCrash(GeckoSession crashedSession) {
                recoverSession();
            }

            @Override
            public void onKill(GeckoSession killedSession) {
                recoverSession();
            }
        });

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(getApplicationContext());
        }

        session.open(sRuntime);
        geckoView.setSession(session);
        session.setActive(true);
        session.setFocused(true);
        session.loadUri(HOME_URL);
        geckoView.requestFocus();
    }

    private void recoverSession() {
        try {
            if (!session.isOpen()) {
                session.open(sRuntime);
            }
            session.loadUri(HOME_URL);
        } catch (Exception ignored) {
        }
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (pageFullscreen) {
                session.exitFullScreen();
                pageFullscreen = false;
                enterImmersiveMode();
                return true;
            }
            if (canGoBack) {
                session.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        if (session != null && session.isOpen()) {
            session.setActive(true);
            session.setFocused(true);
        }
    }

    @Override
    protected void onPause() {
        if (session != null && session.isOpen()) {
            session.setFocused(false);
            session.setActive(false);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (geckoView != null) {
            geckoView.releaseSession();
        }
        if (session != null && session.isOpen()) {
            session.close();
        }
        super.onDestroy();
    }
}
