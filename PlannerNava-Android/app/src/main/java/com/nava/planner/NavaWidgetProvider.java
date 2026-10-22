package com.nava.planner;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NavaWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { updateAll(context, manager, ids); }
    static void updateAll(Context context, AppWidgetManager manager, int[] ids) {
        String raw = context.getSharedPreferences("nava_widget", Context.MODE_PRIVATE).getString("planner_data", "{}");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        int total = 0, done = 0; StringBuilder items = new StringBuilder();
        try {
            JSONObject root = new JSONObject(raw); JSONArray tasks = root.optJSONArray("tasks");
            if (tasks != null) for (int i=0; i<tasks.length(); i++) {
                JSONObject task = tasks.optJSONObject(i); if (task == null || !today.equals(task.optString("date"))) continue;
                total++; if (task.optBoolean("done")) { done++; continue; }
                if (items.length() < 1) items.append("• ").append(task.optString("title", "کار بدون عنوان"));
                else if (items.toString().split("\\n").length < 3) items.append("\n• ").append(task.optString("title", "کار بدون عنوان"));
            }
        } catch (Exception ignored) { }
        String summary = total == 0 ? "برای امروز کاری ثبت نشده" : (done + " از " + total + " کار انجام شده");
        String taskText = items.length() == 0 ? "وقتشه یک کار کوچک ثبت کنی ✨" : items.toString();
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.nava_widget);
            views.setTextViewText(R.id.widget_summary, summary);
            views.setTextViewText(R.id.widget_tasks, taskText);
            Intent launch = new Intent(context, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pi);
            manager.updateAppWidget(id, views);
        }
    }
}
