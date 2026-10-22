package com.nava.planner;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;

public class MainActivity extends Activity {
    private WebView webView;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(117,102,217));
        getWindow().setNavigationBarColor(Color.rgb(35,33,49));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(246,245,251));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDatabaseEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new NavaBridge(this), "NavaBridge");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
    public static class NavaBridge {
        private final Context context;
        NavaBridge(Context c) { context = c.getApplicationContext(); }
        @JavascriptInterface public void sync(String json) {
            context.getSharedPreferences("nava_widget", Context.MODE_PRIVATE).edit().putString("planner_data", json).apply();
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName component = new ComponentName(context, NavaWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(component);
            if (ids.length > 0) NavaWidgetProvider.updateAll(context, manager, ids);
        }
    }
}
