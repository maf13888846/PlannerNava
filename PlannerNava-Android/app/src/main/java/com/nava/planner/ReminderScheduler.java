package com.nava.planner;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ReminderScheduler {
    private static final String CHANNEL = "nava_task_reminders";
    private static final String PREFS = "nava_reminders";
    private static final String IDS = "scheduled_ids";
    private ReminderScheduler() {}

    public static void createNotificationChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "یادآوری‌های نوا", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("یادآوری کارهای زمان‌بندی‌شده در نوا");
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    public static synchronized void sync(Context c, String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray tasks = root.optJSONArray("tasks");
            if (tasks == null) tasks = new JSONArray();

            Set<String> old = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getStringSet(IDS, new HashSet<>());
            if (old != null) for (String id : new HashSet<>(old)) cancel(c, id);

            Set<String> current = new HashSet<>();
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            parser.setLenient(false);
            long now = System.currentTimeMillis();

            for (int i = 0; i < tasks.length(); i++) {
                JSONObject t = tasks.optJSONObject(i);
                if (t == null) continue;
                String id = t.optString("id", "");
                String title = t.optString("title", "کار برنامه‌ریزی‌شده");
                String date = t.optString("date", "");
                String time = t.optString("time", "");
                if (id.isEmpty() || !t.optBoolean("reminder", false)
                        || t.optBoolean("done", false) || date.isEmpty() || time.isEmpty()) continue;
                try {
                    Date when = parser.parse(date + " " + time);
                    if (when == null || when.getTime() <= now) continue;
                    schedule(c, id, title, when.getTime());
                    current.add(id);
                } catch (ParseException ignored) {}
            }
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putStringSet(IDS, current).apply();
        } catch (Exception ignored) {}
    }

    private static void schedule(Context c, String id, String title, long when) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, NavaReminderReceiver.class);
        i.setAction("com.nava.planner.REMINDER." + id);
        i.putExtra("task_id", id);
        i.putExtra("task_title", title);
        PendingIntent pi = PendingIntent.getBroadcast(c, id.hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        // May be delayed slightly by Android battery-saving rules.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
    }

    private static void cancel(Context c, String id) {
        Intent i = new Intent(c, NavaReminderReceiver.class);
        i.setAction("com.nava.planner.REMINDER." + id);
        PendingIntent pi = PendingIntent.getBroadcast(c, id.hashCode(), i,
                PendingIntent.FLAG_NO_CREATE | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        if (pi != null) {
            AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }

    public static void notifyTask(Context c, String id, String title) {
        createNotificationChannel(c);
        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        PendingIntent content = null;
        if (open != null) content = PendingIntent.getActivity(c, 9101, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("نوا | یادآوری کار")
                .setContentText(title).setAutoCancel(true);
        if (content != null) b.setContentIntent(content);
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_HIGH);
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try { nm.notify(id.hashCode(), b.build()); } catch (SecurityException ignored) {}
        }
    }
}
