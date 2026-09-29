package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.CallLog;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Anrufe-Karte: die letzten Anrufe aus der System-Anrufliste (CallLog). Zeigt
 * Name (falls bekannt, sonst Nummer), Richtung (ein-/ausgehend/verpasst) und
 * Zeit. Tippen oeffnet die Waehl-App mit der Nummer.
 */
public class CallsTab extends BaseTab {

    CallsTab(TabInstance inst) { super(inst, R.string.tab_calls, R.drawable.ic_call); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);

        if (ctx.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return TabPermHint.build(ctx, closePanel, d,
                    ctx.getString(R.string.calls_permission_missing), android.Manifest.permission.READ_CALL_LOG);
        }

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        List<Row> rows = query(ctx);
        if (rows.isEmpty()) {
            TextView none = new TextView(ctx);
            none.setText(R.string.calls_none);
            none.setTextColor(Color.parseColor("#9E9E9E"));
            none.setTextSize(14);
            none.setPadding(0, 12 * d, 0, 0);
            root.addView(none);
            return scroll;
        }
        for (Row r : rows) root.addView(row(ctx, r, close, d));
        return scroll;
    }

    private static final class Row { String number, name; int type; long date; }

    private List<Row> query(Context ctx) {
        List<Row> out = new ArrayList<>();
        try (Cursor c = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE},
                null, null, CallLog.Calls.DATE + " DESC")) {
            if (c != null) {
                while (c.moveToNext() && out.size() < 40) {
                    Row r = new Row();
                    r.number = c.getString(0);
                    r.name = c.getString(1);
                    r.type = c.getInt(2);
                    r.date = c.isNull(3) ? 0 : c.getLong(3);
                    out.add(r);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private View row(Context ctx, Row r, Runnable close, int d) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2C2C2E"));
        bg.setCornerRadius(10 * d);
        box.setBackground(bg);
        box.setPadding(10 * d, 8 * d, 10 * d, 8 * d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 6 * d;
        box.setLayoutParams(lp);

        ImageView icon = new ImageView(ctx);
        icon.setImageResource(R.drawable.ic_call);
        boolean missed = r.type == CallLog.Calls.MISSED_TYPE;
        icon.setColorFilter(Color.parseColor(missed ? "#E0533A" : "#2E9BE6"));
        int s = 24 * d;
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(s, s);
        ilp.rightMargin = 12 * d;
        icon.setLayoutParams(ilp);
        box.addView(icon);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView who = new TextView(ctx);
        String name = r.name != null && !r.name.isEmpty() ? r.name : TabPermHint.contactName(ctx, r.number);
        who.setText(name != null ? name : (r.number == null ? "?" : r.number));
        who.setTextColor(Color.WHITE);
        who.setTextSize(14);
        col.addView(who);
        String dir;
        switch (r.type) {
            case CallLog.Calls.INCOMING_TYPE: dir = "eingehend"; break;
            case CallLog.Calls.OUTGOING_TYPE: dir = "ausgehend"; break;
            case CallLog.Calls.MISSED_TYPE:   dir = "verpasst"; break;
            default: dir = "";
        }
        String when = r.date > 0 ? DateUtils.getRelativeTimeSpanString(r.date,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString() : "";
        TextView sub = new TextView(ctx);
        sub.setText((dir.isEmpty() ? "" : dir + " · ") + when);
        sub.setTextColor(Color.parseColor(missed ? "#E0866A" : "#8899AA"));
        sub.setTextSize(11);
        col.addView(sub);
        box.addView(col);

        final String num = r.number;
        box.setClickable(true);
        box.setOnClickListener(v -> {
            try {
                ctx.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + (num == null ? "" : num)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {}
            if (close != null) close.run();
        });
        return box;
    }
}
