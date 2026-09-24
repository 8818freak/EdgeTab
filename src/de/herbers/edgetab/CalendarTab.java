package de.herbers.edgetab;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Kalender-Karte nach BB-Vorbild: Termine nach Tagen gruppiert (HEUTE, MORGEN,
 * dann Wochentag + Datum als Zwischenueberschrift). Jede Zeile mit farbigem
 * Balken in der Kalenderfarbe, Titel, Uhrzeit und Ort. Die Zeilen sind deckend
 * (eigener Hintergrund), auch wenn die Leiste durchscheinend ist - so bleibt
 * der Text ueberall lesbar.
 */
public class CalendarTab extends BaseTab {

    public CalendarTab(TabInstance inst) { super(inst, "Kalender", R.drawable.ic_calendar); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        float fs = Settings.fontScale(ctx);
        Runnable close = effectiveClose(closePanel);
        ScrollView scroll = new ScrollView(ctx);
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        if (ctx.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            list.addView(note(ctx, "Kalender-Berechtigung fehlt", "#FFB0B0", fs));
            return scroll;
        }

        int shown = 0;
        long now = System.currentTimeMillis();
        long in7 = now + 7L * 24 * 60 * 60 * 1000;
        String[] proj = {
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.DISPLAY_COLOR
        };
        try {
            Uri.Builder b = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(b, now);
            ContentUris.appendId(b, in7);
            Cursor c = ctx.getContentResolver().query(b.build(), proj, null, null,
                    CalendarContract.Instances.BEGIN + " ASC");
            long lastDay = -1;
            if (c != null) {
                while (c.moveToNext() && shown < 25) {
                    long id = c.getLong(0);
                    String title = c.getString(1);
                    long begin = c.getLong(2);
                    long end = c.getLong(3);
                    boolean allDay = c.getInt(4) != 0;
                    String loc = c.getString(5);
                    int color = c.getInt(6);

                    long day = dayIndex(begin);
                    if (day != lastDay) {
                        list.addView(dayHeader(ctx, begin, fs));
                        lastDay = day;
                    }
                    list.addView(entry(ctx, title == null ? "(ohne Titel)" : title,
                            begin, end, allDay, loc, color, id, fs, close));
                    shown++;
                }
                c.close();
            }
        } catch (Exception e) {
            list.addView(note(ctx, "Kalender nicht lesbar", "#FFB0B0", fs));
            return scroll;
        }
        if (shown == 0) {
            list.addView(note(ctx, "Keine Termine in den naechsten 7 Tagen", "#9E9E9E", fs));
        }
        return withFab(ctx, scroll, addEventFab(ctx, closePanel));
    }

    /** "Termin hinzufuegen" - startet den eigenen Termin-Editor der auf dem
     *  Geraet installierten Kalender-App (ACTION_INSERT), statt ein eigenes,
     *  schlankeres Formular nachzubauen. Mathias' Wunsch: "Der [eigene]
     *  Erstellen-Dialog ist nett, aber besser ist der des Kalenders selber.
     *  Da geht mehr. Und mehrere, verschiedene zu bedienende sind doof." -
     *  spart ausserdem die eigene WRITE_CALENDAR-Berechtigung, da die
     *  gestartete Kalender-App mit ihren eigenen Rechten schreibt.
     *  Schwebendes Stift-Symbol statt Textknopf - wie in BBs eigener
     *  Kalender-App (Mathias' Screenshot). */
    private View addEventFab(Context ctx, Runnable closePanel) {
        return fab(ctx, R.drawable.ic_fab_edit, "#3DA764", v -> {
            try {
                Intent i = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {}
            if (closePanel != null) closePanel.run();
        });
    }

    private long dayIndex(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR);
    }

    private View dayHeader(Context ctx, long begin, float fs) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        TextView h = new TextView(ctx);
        String label;
        if (DateUtils.isToday(begin)) label = "HEUTE";
        else if (DateUtils.isToday(begin - 86400000L)) label = "MORGEN";
        else label = DateUtils.formatDateTime(ctx, begin,
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_ABBREV_MONTH).toUpperCase();
        h.setText(label);
        h.setTextColor(Color.parseColor("#7FB0B0B0"));
        h.setTextSize(12 * fs);
        h.setPadding(2 * d, 14 * d, 0, 6 * d);
        return h;
    }

    private View entry(Context ctx, String title, long begin, long end, boolean allDay,
                       String loc, int color, long eventId, float fs, Runnable closePanel) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        // Deckender, abgerundeter Hintergrund je Eintrag.
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2C2C2E"));
        bg.setCornerRadius(10 * d);
        row.setBackground(bg);
        row.setPadding(0, 0, 10 * d, 0);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = 6 * d;
        row.setLayoutParams(rowLp);

        View bar = new View(ctx);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(5 * d,
                LinearLayout.LayoutParams.MATCH_PARENT);
        barLp.rightMargin = 10 * d;
        bar.setLayoutParams(barLp);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(color == 0 ? Color.parseColor("#2E9BE6") : color);
        barBg.setCornerRadii(new float[]{10*d,10*d,0,0,0,0,10*d,10*d});
        bar.setBackground(barBg);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(0, 10 * d, 0, 10 * d);

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(15 * fs);
        textCol.addView(t);

        String when = allDay ? "ganztägig"
                : DateUtils.formatDateTime(ctx, begin, DateUtils.FORMAT_SHOW_TIME)
                  + " – " + DateUtils.formatDateTime(ctx, end, DateUtils.FORMAT_SHOW_TIME);
        TextView w = new TextView(ctx);
        w.setText(when);
        w.setTextColor(Color.parseColor("#2E9BE6"));
        w.setTextSize(12 * fs);
        textCol.addView(w);

        if (loc != null && !loc.trim().isEmpty()) {
            TextView l = new TextView(ctx);
            l.setText(loc.trim());
            l.setTextColor(Color.parseColor("#9E9E9E"));
            l.setTextSize(12 * fs);
            textCol.addView(l);
        }

        row.addView(bar);
        row.addView(textCol);
        row.setClickable(true);
        row.setOnClickListener(v -> {
            try {
                Uri uri = ContentUris.withAppendedId(
                        CalendarContract.Events.CONTENT_URI, eventId);
                Intent i = new Intent(Intent.ACTION_VIEW).setData(uri)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                closePanel.run();
                ctx.startActivity(i);
            } catch (Exception ignored) {}
        });
        return row;
    }

    static TextView note(Context ctx, String text, String color, float fs) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(Color.parseColor(color));
        t.setTextSize(13 * fs);
        return t;
    }
}
