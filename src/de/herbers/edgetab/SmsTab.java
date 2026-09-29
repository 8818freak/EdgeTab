package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * SMS-Karte: die letzten SMS ueber den System-Provider (content://sms). Zeigt
 * Absender/Empfaenger (Kontaktname, falls bekannt), eine Textvorschau, Richtung
 * und Zeit. Tippen oeffnet den Verlauf in der Standard-SMS-App.
 *
 * <p>Berechtigung (READ_SMS) wird erst hier bei Bedarf verlangt (ueber
 * MainActivity, da ein Laufzeit-Dialog eine Activity braucht).
 */
public class SmsTab extends BaseTab {

    SmsTab(TabInstance inst) { super(inst, R.string.tab_sms, R.drawable.ic_sms); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);

        if (ctx.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return TabPermHint.build(ctx, closePanel, d,
                    ctx.getString(R.string.sms_permission_missing), android.Manifest.permission.READ_SMS);
        }

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        List<Row> rows = query(ctx);
        if (rows.isEmpty()) {
            TextView none = new TextView(ctx);
            none.setText(R.string.sms_none);
            none.setTextColor(Color.parseColor("#9E9E9E"));
            none.setTextSize(14);
            none.setPadding(0, 12 * d, 0, 0);
            root.addView(none);
            return scroll;
        }
        for (Row r : rows) root.addView(row(ctx, r, close, d));
        return scroll;
    }

    private static final class Row { String address, body; long date; boolean incoming; }

    private List<Row> query(Context ctx) {
        List<Row> out = new ArrayList<>();
        try (Cursor c = ctx.getContentResolver().query(Uri.parse("content://sms"),
                new String[]{"address", "body", "date", "type"}, null, null, "date DESC")) {
            if (c != null) {
                while (c.moveToNext() && out.size() < 40) {
                    Row r = new Row();
                    r.address = c.getString(0);
                    r.body = c.getString(1);
                    r.date = c.isNull(2) ? 0 : c.getLong(2);
                    r.incoming = c.getInt(3) == 1;
                    if (r.body != null) out.add(r);
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
        icon.setImageResource(R.drawable.ic_sms);
        icon.setColorFilter(Color.parseColor("#5BD68A"));
        int s = 24 * d;
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(s, s);
        ilp.rightMargin = 12 * d;
        icon.setLayoutParams(ilp);
        box.addView(icon);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView who = new TextView(ctx);
        String name = TabPermHint.contactName(ctx, r.address);
        who.setText((r.incoming ? "" : "→ ") + (name != null ? name : (r.address == null ? "?" : r.address)));
        who.setTextColor(Color.WHITE);
        who.setTextSize(14);
        col.addView(who);
        TextView body = new TextView(ctx);
        body.setText(r.body);
        body.setTextColor(Color.parseColor("#C0C0C0"));
        body.setTextSize(13);
        body.setMaxLines(2);
        col.addView(body);
        if (r.date > 0) {
            TextView t = new TextView(ctx);
            t.setText(DateUtils.getRelativeTimeSpanString(r.date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
            t.setTextColor(Color.parseColor("#8899AA"));
            t.setTextSize(11);
            col.addView(t);
        }
        box.addView(col);

        final String addr = r.address;
        box.setClickable(true);
        box.setOnClickListener(v -> {
            try {
                ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + (addr == null ? "" : addr)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {}
            if (close != null) close.run();
        });
        return box;
    }
}
