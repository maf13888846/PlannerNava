package com.nava.planner;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.graphics.Color;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(117, 102, 217));
        getWindow().setNavigationBarColor(Color.rgb(35, 33, 49));
        ReminderScheduler.createNotificationChannel(this);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(246, 245, 251));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDatabaseEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new NavaBridge(this), "NavaBridge");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");

        String saved = getSharedPreferences("nava_widget", MODE_PRIVATE)
                .getString("planner_data", null);
        if (saved != null) ReminderScheduler.sync(this, saved);
    }

    public class NavaBridge {
        private final MainActivity activity;
        private final Context context;
        NavaBridge(MainActivity a) { activity = a; context = a.getApplicationContext(); }

        @JavascriptInterface public void sync(String json) {
            context.getSharedPreferences("nava_widget", Context.MODE_PRIVATE)
                    .edit().putString("planner_data", json).apply();
            ReminderScheduler.sync(context, json);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName component = new ComponentName(context, NavaWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(component);
            if (ids.length > 0) NavaWidgetProvider.updateAll(context, manager, ids);
        }

        @JavascriptInterface public void requestNotificationPermission() {
            activity.runOnUiThread(() -> {
                ReminderScheduler.createNotificationChannel(activity);
                if (Build.VERSION.SDK_INT >= 33
                        && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(
                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, 701);
                } else {
                    Toast.makeText(activity, "مجوز اعلان بررسی شد.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 701 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ReminderScheduler.createNotificationChannel(this);
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
