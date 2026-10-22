package com.nava.planner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NavaReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            String saved = context.getSharedPreferences("nava_widget", Context.MODE_PRIVATE)
                    .getString("planner_data", null);
            if (saved != null) ReminderScheduler.sync(context, saved);
            return;
        }
        String id = intent.getStringExtra("task_id");
        String title = intent.getStringExtra("task_title");
        if (id != null && title != null) ReminderScheduler.notifyTask(context, id, title);
    }
}
