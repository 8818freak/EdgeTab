package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * SMS-Karte: die letzten SMS ueber content://sms, zu Konversationen je Kontakt
 * gruppiert (aufklappbar) - wie im Posteingang. Direktantwort (SEND_SMS) und
 * Oeffnen des Verlaufs in der Standard-SMS-App. Berechtigung (READ_SMS) wird
 * erst hier bei Bedarf verlangt.
 */
public class SmsTab extends BaseTab {

    private static final Set<String> CONV_OPEN = new HashSet<>();
    private static final Set<String> REPLYING = new HashSet<>();
    private static int scrollY = -1;

    SmsTab(TabInstance inst) { super(inst, R.string.tab_sms, R.drawable.ic_sms); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        float fs = Settings.fontScale(ctx);
        Runnable close = effectiveClose(closePanel);

        if (ctx.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return TabPermHint.build(ctx, closePanel, d,
                    ctx.getString(R.string.sms_permission_missing), android.Manifest.permission.READ_SMS);
        }

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);
        final Runnable refresh = () -> {
            scrollY = scroll.getScrollY();
            if (refreshContent != null) refreshContent.run();
        };

        List<Msg> msgs = query(ctx);
        if (msgs.isEmpty()) {
            TextView none = new TextView(ctx);
            none.setText(R.string.sms_none);
            none.setTextColor(Color.parseColor("#9E9E9E"));
            none.setTextSize(14 * fs);
            none.setPadding(0, 12 * d, 0, 0);
            root.addView(none);
            return scroll;
        }

        boolean group = Settings.groupConversations(ctx);
        LinkedHashMap<String, List<Msg>> convs = new LinkedHashMap<>();
        if (group) {
            for (Msg m : msgs) {
                List<Msg> g = convs.get(m.convKey);
                if (g == null) { g = new ArrayList<>(); convs.put(m.convKey, g); }
                g.add(m);
            }
        } else {
            int i = 0;
            for (Msg m : msgs) { List<Msg> g = new ArrayList<>(); g.add(m); convs.put("_" + (i++), g); }
        }

        for (List<Msg> g : convs.values()) {
            root.addView(conversation(ctx, g, fs, d, close, refresh));
        }

        if (scrollY > 0) { final int y = scrollY; scrollY = -1; scroll.post(() -> scroll.scrollTo(0, y)); }
        return scroll;
    }

    private static final class Msg { String address, name, body; long date; boolean incoming; String convKey; }

    private List<Msg> query(Context ctx) {
        List<Msg> out = new ArrayList<>();
        try (Cursor c = ctx.getContentResolver().query(Uri.parse("content://sms"),
                new String[]{"address", "body", "date", "type"},
                "date >= ?", new String[]{String.valueOf(Settings.listCutoff(ctx))}, "date DESC")) {
            if (c != null) {
                while (c.moveToNext() && out.size() < 5000) {
                    Msg m = new Msg();
                    m.address = c.getString(0);
                    m.body = c.getString(1);
                    if (m.body == null) continue;
                    m.date = c.isNull(2) ? 0 : c.getLong(2);
                    m.incoming = c.getInt(3) == 1;
                    m.name = TabPermHint.contactName(ctx, m.address);
                    m.convKey = m.name != null ? m.name : (m.address == null ? "?" : m.address);
                    out.add(m);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    /** Eine Konversation: neueste Nachricht, Antworten, Aufklappen der aelteren. */
    private View conversation(Context ctx, List<Msg> g, float fs, int d, Runnable close, Runnable refresh) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2C2C2E"));
        bg.setCornerRadius(10 * d);
        box.setBackground(bg);
        box.setPadding(10 * d, 8 * d, 10 * d, 8 * d);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.bottomMargin = 6 * d;
        box.setLayoutParams(blp);

        Msg newest = g.get(0);
        final String key = newest.convKey;
        final String addr = newest.address;
        boolean open = CONV_OPEN.contains(key);
        boolean replying = REPLYING.contains(key);

        // Kopf: Name + Antworten-Symbol.
        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(ctx);
        icon.setImageResource(R.drawable.ic_sms);
        icon.setColorFilter(Color.parseColor("#5BD68A"));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(20 * d, 20 * d);
        ilp.rightMargin = 8 * d;
        icon.setLayoutParams(ilp);
        head.addView(icon);
        TextView who = new TextView(ctx);
        who.setText(newest.name != null ? newest.name : (addr == null ? "?" : addr));
        who.setTextColor(Color.WHITE);
        who.setTextSize(14 * fs);
        who.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(who);
        if (addr != null && !addr.isEmpty()) {
            TextView reply = new TextView(ctx);
            reply.setText("↩");
            reply.setTextColor(Color.parseColor(replying ? "#2E9BE6" : "#8899AA"));
            reply.setTextSize(18 * fs);
            reply.setPadding(12 * d, 8 * d, 8 * d, 8 * d);
            reply.setOnClickListener(v -> {
                if (replying) REPLYING.remove(key); else REPLYING.add(key);
                refresh.run();
            });
            head.addView(reply);
        }
        box.addView(head);

        // Neueste Nachricht (volle Laenge).
        box.addView(msgLine(ctx, newest, fs, d));

        // Antwortfeld.
        if (replying && addr != null) {
            LinearLayout rr = new LinearLayout(ctx);
            rr.setOrientation(LinearLayout.HORIZONTAL);
            rr.setGravity(Gravity.CENTER_VERTICAL);
            rr.setPadding(0, 8 * d, 0, 4 * d);
            EditText input = new EditText(ctx);
            input.setHint(R.string.reply_hint);
            input.setHintTextColor(Color.parseColor("#9AA6B2"));
            input.setTextColor(Color.WHITE);
            input.setTextSize(13 * fs);
            input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            rr.addView(input);
            Button send = new Button(ctx);
            send.setText(R.string.send_action);
            send.setOnClickListener(v -> {
                String txt = input.getText().toString().trim();
                if (txt.isEmpty()) return;
                if (ctx.checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ctx.startActivity(new Intent(ctx, MainActivity.class)
                            .putExtra("request_permission", android.Manifest.permission.SEND_SMS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    if (close != null) close.run();
                    return;
                }
                boolean ok = TabPermHint.sendSms(ctx, addr, txt);
                Toast.makeText(ctx, ok ? R.string.reply_sent : R.string.reply_failed, Toast.LENGTH_SHORT).show();
                REPLYING.remove(key);
                refresh.run();
            });
            rr.addView(send);
            box.addView(rr);
        }

        // Aufklappen der aelteren Nachrichten (grosses Ziel).
        if (g.size() > 1) {
            TextView toggle = new TextView(ctx);
            toggle.setText((open ? "▾   " : "▸   ") + (g.size() - 1) + " weitere Nachrichten");
            toggle.setTextColor(Color.parseColor("#2E9BE6"));
            toggle.setTextSize(13 * fs);
            GradientDrawable tbg = new GradientDrawable();
            tbg.setColor(Color.parseColor("#22314A"));
            tbg.setCornerRadius(8 * d);
            toggle.setBackground(tbg);
            toggle.setPadding(12 * d, 10 * d, 12 * d, 10 * d);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = 6 * d;
            toggle.setLayoutParams(tlp);
            toggle.setOnClickListener(v -> { if (open) CONV_OPEN.remove(key); else CONV_OPEN.add(key); refresh.run(); });
            box.addView(toggle);
            if (open) {
                for (int i = 1; i < g.size(); i++) box.addView(msgLine(ctx, g.get(i), fs, d));
            }
        }

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

    private View msgLine(Context ctx, Msg m, float fs, int d) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 4 * d, 0, 4 * d);
        TextView body = new TextView(ctx);
        body.setText((m.incoming ? "" : "→ ") + m.body);
        body.setTextColor(Color.parseColor("#E0E0E0"));
        body.setTextSize(13 * fs);
        col.addView(body);
        if (m.date > 0) {
            TextView t = new TextView(ctx);
            t.setText(Settings.formatTime(ctx, m.date));
            t.setTextColor(Color.parseColor("#8899AA"));
            t.setTextSize(11 * fs);
            col.addView(t);
        }
        return col;
    }
}
