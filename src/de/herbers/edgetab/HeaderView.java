package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Die Kopfzeile der Leiste, nach dem BB-Vorbild: gross die Uhrzeit, darunter
 * Wochentag und Datum. Rechts der Akkustand in Prozent mit Symbol, beim Laden
 * ein Blitz und die geschaetzte Restdauer bis voll.
 *
 * Bewusst deutlich groesser als Androids winzige Statusleiste. Ueber die
 * Einstellungen abschaltbar (Kopfzeile und Akku getrennt).
 *
 * Momentaufnahme beim Aufziehen - die Leiste ist ohnehin nur kurz offen.
 */
public class HeaderView {

    public View build(Context ctx) {
        float fs = Settings.fontScale(ctx);
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(4 * d, 0, 4 * d, 14 * d);

        // Links: Uhrzeit gross, darunter Wochentag + Datum.
        LinearLayout clock = new LinearLayout(ctx);
        clock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clock.setLayoutParams(clockLp);

        Date now = new Date();
        TextView time = new TextView(ctx);
        time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        time.setTextColor(Color.parseColor("#2E9BE6"));
        time.setTextSize(30 * fs);
        clock.addView(time);

        TextView date = new TextView(ctx);
        date.setText(new SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault()).format(now));
        date.setTextColor(Color.WHITE);
        date.setTextSize(13 * fs);
        clock.addView(date);

        row.addView(clock);

        // Rechts: Akku.
        if (Settings.showBattery(ctx)) {
            row.addView(buildBattery(ctx, fs, d));
        }
        return row;
    }

    private View buildBattery(Context ctx, float fs, int d) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.END);

        Intent bat = ctx.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int pct = -1;
        boolean charging = false;
        if (bat != null) {
            int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) pct = Math.round(level * 100f / scale);
            int status = bat.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
        }

        // Zeile: [Blitz beim Laden] "55 %" [Akkusymbol]
        LinearLayout line = new LinearLayout(ctx);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        TextView t = new TextView(ctx);
        t.setText((charging ? "\u26A1 " : "") + (pct < 0 ? "–" : pct + " %"));
        t.setTextColor(Color.WHITE);
        t.setTextSize(15 * fs);
        line.addView(t);

        android.widget.ImageView icon = new android.widget.ImageView(ctx);
        icon.setImageResource(R.drawable.ic_battery);
        icon.setColorFilter(batteryColor(pct, charging));
        int iw = Math.round(26 * fs) * d, ih = Math.round(14 * fs) * d;
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(iw, ih);
        ilp.leftMargin = 6 * d;
        icon.setLayoutParams(ilp);
        line.addView(icon);
        box.addView(line);

        // Ladedauer bis voll (nur beim Laden, ab Android 9).
        if (charging && Build.VERSION.SDK_INT >= 28) {
            try {
                BatteryManager bm = ctx.getSystemService(BatteryManager.class);
                long ms = bm.computeChargeTimeRemaining();
                if (ms > 0) {
                    long min = ms / 60000;
                    String s = min >= 60
                            ? ctx.getString(R.string.battery_full_in_hm, min / 60, min % 60)
                            : ctx.getString(R.string.battery_full_in_m, min);
                    TextView eta = new TextView(ctx);
                    eta.setText(s);
                    eta.setTextColor(Color.parseColor("#9E9E9E"));
                    eta.setTextSize(11 * fs);
                    box.addView(eta);
                }
            } catch (Exception ignored) {}
        }
        return box;
    }

    private int batteryColor(int pct, boolean charging) {
        if (charging) return Color.parseColor("#4CD964");     // gruen beim Laden
        if (pct >= 0 && pct <= 15) return Color.parseColor("#FF6B6B"); // rot bei niedrig
        return Color.WHITE;
    }
}
