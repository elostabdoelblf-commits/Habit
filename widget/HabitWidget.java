package com.habitmentality.app;

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

public class HabitWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static void refreshAll(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, HabitWidget.class));
            if (ids != null && ids.length > 0) m.updateAppWidget(ids, build(c));
        } catch (Exception ignored) { }
    }

    static RemoteViews build(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.habit_widget);
        int done = 0, due = 0, pct = 0, streak = 0;
        String next = "افتح التطبيق لتحديث اليوم";
        boolean fresh = false;
        try {
            String raw = c.getSharedPreferences("hm_widget", Context.MODE_PRIVATE).getString("data", "");
            if (raw != null && raw.length() > 2) {
                JSONObject o = new JSONObject(raw);
                streak = o.optInt("streak", 0);
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                fresh = today.equals(o.optString("d"));
                if (fresh) {
                    done = o.optInt("done", 0);
                    due = o.optInt("due", 0);
                    pct = o.optInt("pct", 0);
                    JSONArray a = o.optJSONArray("next");
                    StringBuilder sb = new StringBuilder();
                    if (a != null) for (int i = 0; i < a.length(); i++) {
                        if (i > 0) sb.append("\n");
                        sb.append("• ").append(a.optString(i));
                    }
                    if (sb.length() > 0) next = sb.toString();
                    else next = due > 0 ? "🎉 خلّصت كل عادات النهاردة" : "مفيش عادات مستحقة النهاردة";
                }
            }
        } catch (Exception ignored) { }

        v.setTextViewText(R.id.w_count, fresh ? (done + " / " + due) : "— / —");
        v.setTextViewText(R.id.w_streak, "🔥 " + streak);
        v.setTextViewText(R.id.w_next, next);
        v.setProgressBar(R.id.w_progress, 100, fresh ? pct : 0, false);

        Intent li = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (li != null) {
            PendingIntent pi = PendingIntent.getActivity(c, 0, li,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.w_root, pi);
        }
        return v;
    }
}
