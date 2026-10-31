package com.habitmentality.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HmWidgets {

    static final int TEXT = 0xFFE8F0FF, MUTED = 0xFF9FB0CC, ACC = 0xFF3CC2BD;
    static final int GOOD = 0xFF34D399, MID = 0xFFFBBF24, BAD = 0xFFEF6F87;
    static final String[] DAYS = {"أحد", "اثن", "ثلا", "أرب", "خمي", "جمع", "سبت"};
    static final String[] MONTHS = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};

    static class Data {
        boolean fresh;
        int done, due, pct, streak;
        String d = "";
        String[] names = new String[0];
        boolean[] ok = new boolean[0];
        int[] wk = new int[0];
        int[] mo = new int[0];
    }

    static Data load(Context c) {
        Data x = new Data();
        try {
            String raw = c.getSharedPreferences("hm_widget", Context.MODE_PRIVATE).getString("data", "");
            if (raw == null || raw.length() < 3) return x;
            JSONObject o = new JSONObject(raw);
            x.d = o.optString("d");
            x.streak = o.optInt("streak");
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            x.fresh = today.equals(x.d);
            x.done = o.optInt("done");
            x.due = o.optInt("due");
            x.pct = o.optInt("pct");
            JSONArray l = o.optJSONArray("list");
            if (l != null) {
                x.names = new String[l.length()];
                x.ok = new boolean[l.length()];
                for (int i = 0; i < l.length(); i++) {
                    JSONArray r = l.optJSONArray(i);
                    x.names[i] = r == null ? "" : r.optString(0);
                    x.ok[i] = r != null && r.optInt(1) == 1;
                }
            }
            JSONArray w = o.optJSONArray("wk");
            if (w != null) {
                x.wk = new int[w.length()];
                for (int i = 0; i < w.length(); i++) x.wk[i] = w.optInt(i);
            }
            JSONArray m = o.optJSONArray("mo");
            if (m != null) {
                x.mo = new int[m.length()];
                for (int i = 0; i < m.length(); i++) x.mo[i] = m.optInt(i, -1);
            }
        } catch (Exception ignored) { }
        return x;
    }

    static int col(int pct) {
        return pct >= 70 ? GOOD : (pct >= 40 ? MID : BAD);
    }

    static Calendar parse(String d) {
        Calendar cal = Calendar.getInstance();
        try {
            String[] p = d.split("-");
            cal.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 12, 0, 0);
        } catch (Exception ignored) { }
        return cal;
    }

    static int txt(Canvas c, String s, float x, float y, int w, float size, int color,
                   boolean bold, boolean center, boolean rtl, int maxLines, boolean strike) {
        if (s == null || s.length() == 0) return 0;
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        if (strike) p.setFlags(p.getFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        StaticLayout l = StaticLayout.Builder.obtain(s, 0, s.length(), p, Math.max(1, w))
                .setAlignment(center ? Layout.Alignment.ALIGN_CENTER : Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(rtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
        c.save();
        c.translate(x, y);
        l.draw(c);
        c.restore();
        return l.getHeight();
    }

    static void bg(Canvas c, int W, int H, float dn) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xE6081020);
        float r = 20 * dn;
        c.drawRoundRect(new RectF(0, 0, W, H), r, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dn);
        p.setColor(0x33FFFFFF);
        c.drawRoundRect(new RectF(dn / 2, dn / 2, W - dn / 2, H - dn / 2), r, r, p);
    }

    // ---------- drawing ----------
    static void drawProgress(Canvas c, int W, int H, Data d, float dn) {
        bg(c, W, H, dn);
        float s = Math.min(W, H);
        float cx = W / 2f, cy = H / 2f - s * 0.05f, r = s * 0.31f, sw = s * 0.09f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(sw);
        p.setStrokeCap(Paint.Cap.ROUND);
        RectF rf = new RectF(cx - r, cy - r, cx + r, cy + r);
        p.setColor(0x33FFFFFF);
        c.drawArc(rf, 0, 360, false, p);
        if (d.fresh && d.pct > 0) {
            p.setColor(col(d.pct));
            c.drawArc(rf, -90, 360f * Math.min(100, d.pct) / 100f, false, p);
        }
        String mid = d.fresh ? (d.pct + "%") : "—";
        float fs = s * 0.19f;
        txt(c, mid, cx - r, cy - fs * 0.62f, (int) (2 * r), fs, TEXT, true, true, false, 1, false);
        String low = d.fresh ? (d.done + " / " + d.due) : "افتح التطبيق";
        txt(c, low, 0, H - s * 0.2f, W, s * 0.105f, MUTED, false, true, !d.fresh, 1, false);
    }

    static void drawWeek(Canvas c, int W, int H, Data d, float dn) {
        bg(c, W, H, dn);
        float pad = 14 * dn;
        txt(c, "نظرة أسبوعية", pad, pad * 0.7f, W - (int) (2 * pad), 14 * dn, TEXT, true, false, true, 1, false);
        int n = Math.min(7, d.wk.length);
        if (n == 0) {
            txt(c, "افتح التطبيق لتحديث البيانات", 0, H / 2f - 8 * dn, W, 13 * dn, MUTED, false, true, true, 1, false);
            return;
        }
        int sum = 0;
        for (int i = 0; i < n; i++) sum += d.wk[i];
        txt(c, (sum / n) + "%", pad, pad * 0.7f, (int) (W / 3f), 14 * dn, ACC, true, false, false, 1, false);
        float top = pad + 24 * dn, bottom = H - pad - 14 * dn;
        float step = (W - 2 * pad) / 7f, bw = step * 0.5f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Calendar base = parse(d.d);
        for (int i = 0; i < n; i++) {
            float x = W - pad - step * (i + 0.5f);
            int v = Math.max(0, Math.min(100, d.wk[i]));
            p.setColor(0x22FFFFFF);
            c.drawRoundRect(new RectF(x - bw / 2, top, x + bw / 2, bottom), 6 * dn, 6 * dn, p);
            float bh = Math.max(4 * dn, (bottom - top) * v / 100f);
            p.setColor(col(v));
            c.drawRoundRect(new RectF(x - bw / 2, bottom - bh, x + bw / 2, bottom), 6 * dn, 6 * dn, p);
            Calendar cal = (Calendar) base.clone();
            cal.add(Calendar.DAY_OF_MONTH, -(n - 1 - i));
            String lab = DAYS[cal.get(Calendar.DAY_OF_WEEK) - 1];
            txt(c, lab, x - step / 2, bottom + 2 * dn, (int) step, 10 * dn, MUTED, false, true, true, 1, false);
        }
    }

    static void drawMonth(Canvas c, int W, int H, Data d, float dn) {
        bg(c, W, H, dn);
        float pad = 12 * dn;
        Calendar cal = parse(d.d);
        int y = cal.get(Calendar.YEAR), mth = cal.get(Calendar.MONTH), today = cal.get(Calendar.DAY_OF_MONTH);
        txt(c, MONTHS[mth] + " " + y, pad, pad * 0.6f, W - (int) (2 * pad), 13 * dn, TEXT, true, false, true, 1, false);
        int days = d.mo.length;
        if (days == 0) {
            txt(c, "افتح التطبيق لتحديث البيانات", 0, H / 2f - 8 * dn, W, 13 * dn, MUTED, false, true, true, 1, false);
            return;
        }
        Calendar first = (Calendar) cal.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int col0 = first.get(Calendar.DAY_OF_WEEK) % 7;
        int rows = (col0 + days + 6) / 7;
        float gridTop = pad + 20 * dn, gridH = H - gridTop - pad * 0.6f;
        float cw = (W - 2 * pad) / 7f, ch = gridH / rows;
        float rad = Math.min(cw, ch) * 0.44f, fs = Math.min(ch * 0.5f, 11 * dn);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int day = 1; day <= days; day++) {
            int idx = col0 + day - 1;
            float cx = W - pad - cw * (idx % 7 + 0.5f);
            float cy = gridTop + ch * (idx / 7 + 0.5f);
            int v = d.mo[day - 1];
            boolean filled = v >= 0;
            p.setStyle(Paint.Style.FILL);
            p.setColor(filled ? (v == 0 ? 0x33FFFFFF : col(v)) : 0x14FFFFFF);
            c.drawCircle(cx, cy, rad, p);
            if (day == today) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.6f * dn);
                p.setColor(0xFFFFFFFF);
                c.drawCircle(cx, cy, rad, p);
            }
            int tc = (filled && v > 0) ? 0xFF081020 : MUTED;
            txt(c, String.valueOf(day), cx - cw / 2, cy - fs * 0.62f, (int) cw, fs, tc, filled && v > 0, true, false, 1, false);
        }
        if (!d.fresh) {
            txt(c, "افتح التطبيق للتحديث", pad, pad * 0.6f, (int) (W / 2.2f), 10 * dn, MUTED, false, false, false, 1, false);
        }
    }

    static void drawList(Canvas c, int W, int H, Data d, float dn) {
        bg(c, W, H, dn);
        float pad = 14 * dn;
        txt(c, "قائمة العادات", pad, pad * 0.7f, W - (int) (2 * pad), 14 * dn, TEXT, true, false, true, 1, false);
        if (d.fresh) txt(c, d.done + " / " + d.due, pad, pad * 0.7f, (int) (W / 3f), 14 * dn, ACC, true, false, false, 1, false);
        if (!d.fresh || d.names.length == 0) {
            String m = d.fresh ? "مفيش عادات مستحقة النهاردة" : "افتح التطبيق لتحديث اليوم";
            txt(c, m, 0, H / 2f - 4 * dn, W, 13 * dn, MUTED, false, true, true, 1, false);
            return;
        }
        float top = pad + 26 * dn, rowH = 24 * dn;
        int maxRows = Math.max(1, (int) ((H - top - pad * 0.4f) / rowH));
        int n = Math.min(d.names.length, maxRows);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float fs = 13 * dn;
        for (int i = 0; i < n; i++) {
            float y = top + i * rowH;
            float cx = W - pad - 8 * dn, cy = y + rowH / 2f;
            if (d.ok[i]) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(ACC);
                c.drawCircle(cx, cy, 8 * dn, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.8f * dn);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setColor(0xFF081020);
                c.drawLine(cx - 4 * dn, cy, cx - 1 * dn, cy + 3 * dn, p);
                c.drawLine(cx - 1 * dn, cy + 3 * dn, cx + 4 * dn, cy - 3 * dn, p);
            } else {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.6f * dn);
                p.setColor(MUTED);
                c.drawCircle(cx, cy, 7.5f * dn, p);
            }
            txt(c, d.names[i], pad, y + (rowH - fs * 1.2f) / 2f, (int) (W - 2 * pad - 24 * dn), fs,
                    d.ok[i] ? MUTED : TEXT, false, false, true, 1, d.ok[i]);
        }
    }

    static String grade(int p) {
        return p >= 90 ? "ممتاز" : (p >= 70 ? "قوي" : (p >= 40 ? "متوسط" : "ضعيف"));
    }

    static void drawDash(Canvas c, int W, int H, Data d, float dn) {
        Paint bgp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgp.setShader(new LinearGradient(0, 0, W, H, 0xF00B1426, 0xF0113044, Shader.TileMode.CLAMP));
        float r = 20 * dn;
        c.drawRoundRect(new RectF(0, 0, W, H), r, r, bgp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dn);
        p.setColor(0x33FFFFFF);
        c.drawRoundRect(new RectF(dn / 2, dn / 2, W - dn / 2, H - dn / 2), r, r, p);

        float pad = 12 * dn;
        int remain = Math.max(0, d.due - d.done);
        int w2 = W - (int) (2 * pad);
        txt(c, "لوحة اليوم", pad, 8 * dn, w2, 14 * dn, TEXT, true, false, true, 1, false);
        if (d.fresh) {
            String sub = d.due == 0 ? "لا عادات مستحقة اليوم"
                    : (remain == 0 ? grade(d.pct) + " • كل العادات خلصت" : grade(d.pct) + " • فاضل " + remain);
            txt(c, sub, pad, 26 * dn, w2, 11 * dn, d.due > 0 && remain == 0 ? GOOD : MUTED, false, false, true, 1, false);
            txt(c, d.d, pad, 9 * dn, (int) (W / 3f), 10 * dn, MUTED, false, false, false, 1, false);
        } else {
            txt(c, "افتح التطبيق لتحديث اليوم", pad, 26 * dn, w2, 11 * dn, MUTED, false, false, true, 1, false);
        }

        // three stat tiles (right to left): remaining, rating, streak
        float gap = 8 * dn, tileTop = 44 * dn, tileH = 52 * dn;
        float tw = (W - 2 * pad - 2 * gap) / 3f;
        String[] nums = {d.fresh ? String.valueOf(remain) : "—", d.fresh ? d.pct + "%" : "—", "🔥 " + d.streak};
        String[] labs = {"عادات متبقية", "تقييم اليوم", "يوم متتالي"};
        int[] cols = {remain > 0 ? MID : GOOD, d.fresh ? col(d.pct) : MUTED, 0xFFFB923C};
        for (int i = 0; i < 3; i++) {
            float right = W - pad - i * (tw + gap), left = right - tw;
            p.setStyle(Paint.Style.FILL);
            p.setColor((cols[i] & 0x00FFFFFF) | 0x2A000000);
            c.drawRoundRect(new RectF(left, tileTop, right, tileTop + tileH), 14 * dn, 14 * dn, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dn);
            p.setColor((cols[i] & 0x00FFFFFF) | 0x66000000);
            c.drawRoundRect(new RectF(left, tileTop, right, tileTop + tileH), 14 * dn, 14 * dn, p);
            txt(c, nums[i], left, tileTop + 6 * dn, (int) tw, 20 * dn, cols[i], true, true, false, 1, false);
            txt(c, labs[i], left, tileTop + 33 * dn, (int) tw, 10 * dn, MUTED, false, true, true, 1, false);
        }

        // progress bar
        float barTop = tileTop + tileH + 8 * dn, barH = 5 * dn;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x22FFFFFF);
        c.drawRoundRect(new RectF(pad, barTop, W - pad, barTop + barH), barH, barH, p);
        if (d.fresh && d.pct > 0) {
            float fw = (W - 2 * pad) * Math.min(100, d.pct) / 100f;
            p.setColor(col(d.pct));
            c.drawRoundRect(new RectF(W - pad - fw, barTop, W - pad, barTop + barH), barH, barH, p);
        }

        // today's habits, pending first
        float rowsTop = barTop + barH + 8 * dn, rowH = 22 * dn;
        int maxRows = (int) ((H - rowsTop - 6 * dn) / rowH);
        if (!d.fresh || d.names.length == 0) {
            if (maxRows > 0) {
                String m = d.fresh ? "مفيش عادات مستحقة النهاردة" : "افتح التطبيق لتحديث اليوم";
                txt(c, m, 0, rowsTop + 3 * dn, W, 12 * dn, MUTED, false, true, true, 1, false);
            }
            return;
        }
        int total = d.names.length;
        int[] order = new int[total];
        int k = 0;
        for (int i = 0; i < total; i++) if (!d.ok[i]) order[k++] = i;
        for (int i = 0; i < total; i++) if (d.ok[i]) order[k++] = i;
        int n = Math.min(total, Math.max(0, maxRows));
        float fs = 12 * dn;
        for (int i = 0; i < n; i++) {
            int idx = order[i];
            float y = rowsTop + i * rowH;
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x14FFFFFF);
            c.drawRoundRect(new RectF(pad, y, W - pad, y + rowH - 3 * dn), 8 * dn, 8 * dn, p);
            float cx = W - pad - 10 * dn, cy = y + (rowH - 3 * dn) / 2f;
            if (d.ok[idx]) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(ACC);
                c.drawCircle(cx, cy, 6.5f * dn, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.6f * dn);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setColor(0xFF081020);
                c.drawLine(cx - 3 * dn, cy, cx - 0.8f * dn, cy + 2.4f * dn, p);
                c.drawLine(cx - 0.8f * dn, cy + 2.4f * dn, cx + 3.2f * dn, cy - 2.6f * dn, p);
            } else {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.8f * dn);
                p.setColor(MID);
                c.drawCircle(cx, cy, 6 * dn, p);
            }
            txt(c, d.names[idx], pad + 8 * dn, y + (rowH - 3 * dn - fs * 1.2f) / 2f,
                    (int) (W - 2 * pad - 30 * dn), fs, d.ok[idx] ? MUTED : TEXT, false, false, true, 1, d.ok[idx]);
        }
        if (total > n && n > 0) {
            txt(c, "+" + (total - n), pad + 4 * dn, rowsTop + (n - 1) * rowH + 3 * dn, (int) (36 * dn), 10 * dn, MUTED, false, false, false, 1, false);
        }
    }

    // ---------- providers ----------
    public abstract static class Base extends AppWidgetProvider {
        abstract void draw(Canvas c, int W, int H, Data d, float dn);

        @Override
        public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
            for (int id : ids) upd(c, m, id, this);
        }

        @Override
        public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) {
            upd(c, m, id, this);
        }
    }

    public static class Dash extends Base {
        @Override
        void draw(Canvas c, int W, int H, Data d, float dn) { drawDash(c, W, H, d, dn); }
    }

    public static class Progress extends Base {
        @Override
        void draw(Canvas c, int W, int H, Data d, float dn) { drawProgress(c, W, H, d, dn); }
    }

    public static class Week extends Base {
        @Override
        void draw(Canvas c, int W, int H, Data d, float dn) { drawWeek(c, W, H, d, dn); }
    }

    public static class Lst extends Base {
        @Override
        void draw(Canvas c, int W, int H, Data d, float dn) { drawList(c, W, H, d, dn); }
    }

    public static class Month extends Base {
        @Override
        void draw(Canvas c, int W, int H, Data d, float dn) { drawMonth(c, W, H, d, dn); }
    }

    static void upd(Context c, AppWidgetManager m, int id, Base b) {
        try {
            float dn = c.getResources().getDisplayMetrics().density;
            Bundle o = m.getAppWidgetOptions(id);
            int wdp = Math.max(80, o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250));
            int hdp = Math.max(80, o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110));
            int W = (int) (wdp * dn), H = (int) (hdp * dn);
            while ((long) W * H > 1400000L) { W = W * 9 / 10; H = H * 9 / 10; }
            float dn2 = dn * W / (wdp * dn);
            Bitmap bm = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            b.draw(new Canvas(bm), W, H, load(c), dn2);
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.hm_widget_img);
            v.setImageViewBitmap(R.id.hm_img, bm);
            Intent li = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
            if (li != null) {
                v.setOnClickPendingIntent(R.id.hm_root, PendingIntent.getActivity(c, 0, li,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
            m.updateAppWidget(id, v);
        } catch (Exception ignored) { }
    }

    static void refreshAll(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            Class<?>[] ks = {Dash.class, Progress.class, Week.class, Lst.class, Month.class};
            Base[] bs = {new Dash(), new Progress(), new Week(), new Lst(), new Month()};
            for (int i = 0; i < ks.length; i++) {
                int[] ids = m.getAppWidgetIds(new ComponentName(c, ks[i]));
                if (ids != null) for (int id : ids) upd(c, m, id, bs[i]);
            }
        } catch (Exception ignored) { }
        HabitWidget.refreshAll(c);
    }

    static boolean pin(Context c, String type) {
        try {
            Class<?> k = "dash".equals(type) ? Dash.class
                    : "prog".equals(type) ? Progress.class
                    : "week".equals(type) ? Week.class
                    : "list".equals(type) ? Lst.class
                    : "month".equals(type) ? Month.class
                    : HabitWidget.class;
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            if (Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported()) {
                return m.requestPinAppWidget(new ComponentName(c, k), null, null);
            }
        } catch (Exception ignored) { }
        return false;
    }
}
