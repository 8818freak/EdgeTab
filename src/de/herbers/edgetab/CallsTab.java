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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Anrufe-Karte: die letzten Anrufe aus der System-Anrufliste (CallLog). Zeigt
 * Name (falls bekannt, sonst Nummer), Richtung (ein-/ausgehend/verpasst) und
 * Zeit. Tippen oeffnet die Waehl-App mit der Nummer.
 */
public class CallsTab extends BaseTab {

    private static boolean searchOpen = false;   // Lupe angetippt -> Suchfeld sichtbar
    private static String query = "";            // aktueller Filterbegriff

    CallsTab(TabInstance inst) { super(inst, R.string.tab_calls, R.drawable.ic_call); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        float fs = Settings.fontScale(ctx);
        Runnable close = effectiveClose(closePanel);

        if (ctx.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return TabPermHint.build(ctx, closePanel, d,
                    ctx.getString(R.string.calls_permission_missing), android.Manifest.permission.READ_CALL_LOG);
        }

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);
        final Runnable refresh = () -> { if (refreshContent != null) refreshContent.run(); };

        root.addView(searchBar(ctx, fs, d, refresh, close));

        List<Row> rows = query(ctx);
        // Lokaler Filter (Name/Nummer) - wie im Posteingang.
        String q = query.toLowerCase();
        if (!q.isEmpty()) {
            List<Row> f = new ArrayList<>();
            for (Row r : rows) {
                String nm = r.name != null && !r.name.isEmpty() ? r.name : TabPermHint.contactName(ctx, r.number);
                if ((nm != null && nm.toLowerCase().contains(q))
                        || (r.number != null && r.number.toLowerCase().contains(q))) f.add(r);
            }
            rows = f;
        }
        if (rows.isEmpty()) {
            TextView none = new TextView(ctx);
            none.setText(q.isEmpty() ? ctx.getString(R.string.calls_none) : "Keine Treffer.");
            none.setTextColor(Color.parseColor("#9E9E9E"));
            none.setTextSize(14 * fs);
            none.setPadding(0, 12 * d, 0, 0);
            root.addView(none);
            return scroll;
        }
        for (Row r : rows) {
            root.addView(row(ctx, r, close, fs, d, refresh));
            if (CallActions.MENU_OPEN.contains(r.id)) {
                root.addView(CallActions.menu(ctx, r.id, r.number, fs, d, close, refresh));
            }
        }
        return scroll;
    }

    /** Suchleiste: Lupe -> Feld. "Hier suchen" filtert diese Liste, "In Sucher
     *  suchen" oeffnet Sucher mit dem Begriff (Volltext ueber alles). */
    private View searchBar(Context ctx, float fs, int d, Runnable refresh, Runnable close) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, 8 * d);
        TextView lupe = new TextView(ctx);
        lupe.setText(searchOpen ? "🔍  Suche schließen" : "🔍  Anrufe durchsuchen");
        lupe.setTextColor(Color.parseColor("#8899AA"));
        lupe.setTextSize(13 * fs);
        lupe.setPadding(2 * d, 4 * d, 2 * d, 4 * d);
        lupe.setOnClickListener(v -> {
            searchOpen = !searchOpen;
            if (!searchOpen) query = "";
            refresh.run();
        });
        box.addView(lupe);
        if (!searchOpen) return box;

        final EditText input = new EditText(ctx);
        input.setHint("Suchbegriff…");
        input.setHintTextColor(Color.parseColor("#9AA6B2"));
        input.setTextColor(Color.WHITE);
        input.setText(query);
        input.setSelection(query.length());
        input.setTextSize(14 * fs);
        input.setSingleLine(true);
        enableClipboardPaste(input);
        box.addView(input);

        LinearLayout btns = new LinearLayout(ctx);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        Button here = new Button(ctx);
        here.setText("Hier suchen");
        here.setOnClickListener(v -> { query = input.getText().toString().trim(); refresh.run(); });
        btns.addView(here);
        Button inSucher = new Button(ctx);
        inSucher.setText("In Sucher suchen");
        inSucher.setOnClickListener(v -> {
            String qq = input.getText().toString().trim();
            try {
                ctx.startActivity(new Intent()
                        .setClassName("de.herbers.sucher", "de.herbers.sucher.MainActivity")
                        .putExtra("query", qq)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                if (close != null) close.run();
            } catch (Exception e) {
                Toast.makeText(ctx, "Sucher ist nicht installiert – hier gefiltert.", Toast.LENGTH_SHORT).show();
                query = qq;
                refresh.run();
            }
        });
        btns.addView(inSucher);
        box.addView(btns);
        return box;
    }

    private static final class Row {
        long id; String number, name; int type; long date; boolean failed;
        int numberType; String numberLabel; // Kennung aus dem Telefonbuch (Mobil/Arbeit/…)
    }

    private List<Row> query(Context ctx) {
        List<Row> out = new ArrayList<>();
        try (Cursor c = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION,
                        CallLog.Calls.CACHED_NUMBER_TYPE, CallLog.Calls.CACHED_NUMBER_LABEL},
                CallLog.Calls.DATE + " >= ?", new String[]{String.valueOf(Settings.listCutoff(ctx))},
                CallLog.Calls.DATE + " DESC")) {
            if (c != null) {
                while (c.moveToNext() && out.size() < 5000) {
                    Row r = new Row();
                    r.id = c.getLong(0);
                    r.number = c.getString(1);
                    r.name = c.getString(2);
                    r.type = c.getInt(3);
                    r.date = c.isNull(4) ? 0 : c.getLong(4);
                    long dur = c.isNull(5) ? 0 : c.getLong(5);
                    r.failed = r.type == CallLog.Calls.OUTGOING_TYPE && dur == 0;
                    r.numberType = c.isNull(6) ? 0 : c.getInt(6);
                    r.numberLabel = c.getString(7);
                    out.add(r);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private View row(Context ctx, Row r, Runnable close, float fs, int d, Runnable refresh) {
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
        // gruen = erfolgreich, rot = verpasst, blau = vergeblich (abgehend, niemanden erreicht)
        String callColor = missed ? "#E0533A" : (r.failed ? "#2E9BE6" : "#5BD68A");
        icon.setColorFilter(Color.parseColor(callColor));
        int s = 24 * d;
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(s, s);
        ilp.rightMargin = 12 * d;
        icon.setLayoutParams(ilp);
        box.addView(icon);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView who = new TextView(ctx);
        String name = r.name != null && !r.name.isEmpty() ? r.name : TabPermHint.contactName(ctx, r.number);
        boolean haveName = name != null && !name.isEmpty();
        who.setText(haveName ? name : (r.number == null ? "?" : r.number));
        who.setTextColor(Color.WHITE);
        who.setTextSize(14 * fs);
        col.addView(who);

        // Rufnummern-Kennung wie im Telefonbuch (Mobil/Arbeit/Privat …); bei
        // bekannten Kontakten zusaetzlich die tatsaechliche Nummer.
        String kind = CallActions.typeLabel(ctx, r.numberType, r.numberLabel);
        StringBuilder idLine = new StringBuilder();
        if (!kind.isEmpty()) idLine.append(kind);
        if (haveName && r.number != null && !r.number.isEmpty()) {
            if (idLine.length() > 0) idLine.append(" · ");
            idLine.append(r.number);
        }
        if (idLine.length() > 0) {
            TextView idv = new TextView(ctx);
            idv.setText(idLine.toString());
            idv.setTextColor(Color.parseColor("#8899AA"));
            idv.setTextSize(11 * fs);
            col.addView(idv);
        }

        String dir;
        switch (r.type) {
            case CallLog.Calls.INCOMING_TYPE: dir = "eingehend"; break;
            case CallLog.Calls.OUTGOING_TYPE: dir = r.failed ? "ausgehend · nicht erreicht" : "ausgehend"; break;
            case CallLog.Calls.MISSED_TYPE:   dir = "verpasst"; break;
            default: dir = "";
        }
        String when = Settings.formatTime(ctx, r.date);
        TextView sub = new TextView(ctx);
        sub.setText((dir.isEmpty() ? "" : dir + " · ") + when);
        sub.setTextColor(Color.parseColor(callColor));
        sub.setTextSize(11 * fs);
        col.addView(sub);
        box.addView(col);

        // Kurzer Tipp: direkt anrufen. Langer Tipp: Menue (Wählen, SMS, Kontakt,
        // kopieren, löschen) ein-/ausklappen.
        final String num = r.number;
        final long rid = r.id;
        box.setClickable(true);
        box.setLongClickable(true);
        box.setOnClickListener(v -> CallActions.call(ctx, num, close));
        box.setOnLongClickListener(v -> {
            if (CallActions.MENU_OPEN.contains(rid)) CallActions.MENU_OPEN.remove(rid);
            else CallActions.MENU_OPEN.add(rid);
            if (refresh != null) refresh.run();
            return true;
        });
        return box;
    }
}
