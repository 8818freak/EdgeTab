package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Notizen-Karte über denselben offenen Content-Provider von <b>JTX Board</b>
 * wie {@link TasksTab} - nur mit Modul-Filter auf {@code NOTE}/{@code JOURNAL}
 * statt {@code TODO}. Gleicher Provider, gleiche Konto-Kopplung, gleiche
 * Berechtigung - siehe die ausfuehrliche Erklaerung dort.
 *
 * Zeigt (und oeffnet zum Bearbeiten in JTX Board) nur Notizen, die dort
 * tatsaechlich vorhanden sind - ob JTX Board ueberhaupt Notizen/Journal-
 * Eintraege von Mathias' Server bekommt, haengt davon ab, ob die per DAVx5
 * verbundene Sammlung VJOURNAL ueberhaupt synchronisiert (serverseitige
 * Frage, unabhaengig von EdgeTab).
 */
public class NotesTab extends BaseTab {

    private static final String JTX_PACKAGE = "at.techbee.jtx";
    private static final String JTX_PERMISSION = "at.techbee.jtx.permission.READ";
    private static final Uri JTX_URI =
            Uri.parse("content://at.techbee.jtx.provider/icalobject");
    private static final Uri JTX_VIEW_URI =
            Uri.parse("content://at.techbee.jtx/icalobject");

    NotesTab(TabInstance inst) { super(inst, "Notizen", R.drawable.ic_notes); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);

        if (!isInstalled(ctx)) {
            return hint(ctx, "JTX Board ist nicht installiert.\nDiese Karte "
                    + "liest Notizen aus JTX Board (über DAVx5 synchronisiert).", d);
        }
        if (ctx.checkSelfPermission(JTX_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            return permissionHint(ctx, closePanel, d);
        }
        String accountName = Settings.jtxAccountName(ctx);
        String accountType = Settings.jtxAccountType(ctx);
        if (accountName == null) {
            return accountHint(ctx, closePanel, d);
        }

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        Uri accountUri = JTX_URI.buildUpon()
                .appendQueryParameter("caller_is_syncadapter", "true")
                .appendQueryParameter("account_name", accountName)
                .appendQueryParameter("account_type", accountType)
                .build();

        int shown = 0;
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(accountUri,
                    new String[]{"_id", "summary", "description", "lastmodified"},
                    "module = ? OR module = ?",
                    new String[]{"NOTE", "JOURNAL"},
                    "lastmodified DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    String summary = c.getString(1);
                    String description = c.getString(2);
                    long lastMod = c.isNull(3) ? 0 : c.getLong(3);
                    root.addView(noteRow(ctx, id, summary, description, lastMod, close, d));
                    shown++;
                }
            }
        } catch (Exception e) {
            android.util.Log.w("EdgeTabNotes", "Abfrage fehlgeschlagen", e);
            return hint(ctx, "Notizen konnten nicht gelesen werden.\n"
                    + "Ist JTX Board aktuell genug?", d);
        } finally {
            if (c != null) c.close();
        }

        if (shown == 0) {
            TextView t = new TextView(ctx);
            t.setText("Keine Notizen gefunden.\nHinweis: JTX Board muss dafür eine "
                    + "Sammlung mit Notizen/Journal synchronisieren - das ist eine "
                    + "Einstellung in DAVx5, keine in EdgeTab.");
            t.setTextColor(Color.parseColor("#9E9E9E"));
            t.setTextSize(14);
            root.addView(t);
        }
        return scroll;
    }

    private boolean isInstalled(Context ctx) {
        try { ctx.getPackageManager().getPackageInfo(JTX_PACKAGE, 0); return true; }
        catch (Exception e) { return false; }
    }

    private View permissionHint(Context ctx, Runnable closePanel, int d) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 12 * d, 0, 0);

        TextView t = new TextView(ctx);
        t.setText("Für die Notizen-Karte fehlt die Berechtigung, JTX Boards "
                + "Notizen zu lesen.");
        t.setTextColor(Color.parseColor("#CCCCCC"));
        t.setTextSize(14);
        t.setPadding(0, 0, 0, 12 * d);
        box.addView(t);

        Button allow = new Button(ctx);
        allow.setText("Zugriff erlauben");
        allow.setOnClickListener(v -> {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("request_permission", JTX_PERMISSION);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            if (closePanel != null) closePanel.run();
        });
        box.addView(allow);
        return box;
    }

    private View accountHint(Context ctx, Runnable closePanel, int d) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 12 * d, 0, 0);

        TextView t = new TextView(ctx);
        t.setText("Für die Notizen-Karte fehlt noch die Auswahl deines "
                + "DAVx5-Kontos (dasselbe wie bei den Aufgaben).");
        t.setTextColor(Color.parseColor("#CCCCCC"));
        t.setTextSize(14);
        t.setPadding(0, 0, 0, 12 * d);
        box.addView(t);

        Button pick = new Button(ctx);
        pick.setText("Konto auswählen");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("pick_jtx_account", true);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            if (closePanel != null) closePanel.run();
        });
        box.addView(pick);
        return box;
    }

    private View hint(Context ctx, String text, int d) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 12 * d, 0, 0);
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(Color.parseColor("#9E9E9E"));
        t.setTextSize(14);
        box.addView(t);
        return box;
    }

    private View noteRow(Context ctx, long id, String summary, String description,
                          long lastMod, Runnable close, int d) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2C2C2E"));
        bg.setCornerRadius(10 * d);
        row.setBackground(bg);
        row.setPadding(12 * d, 10 * d, 12 * d, 10 * d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 6 * d;
        row.setLayoutParams(lp);

        TextView title = new TextView(ctx);
        title.setText(summary == null || summary.isEmpty() ? "(ohne Titel)" : summary);
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        row.addView(title);

        if (description != null && !description.isEmpty()) {
            TextView desc = new TextView(ctx);
            desc.setText(description);
            desc.setTextColor(Color.parseColor("#AAAAAA"));
            desc.setTextSize(12.5f);
            desc.setMaxLines(3);
            desc.setPadding(0, 4 * d, 0, 0);
            row.addView(desc);
        }

        if (lastMod > 0) {
            TextView meta = new TextView(ctx);
            meta.setText(DateUtils.getRelativeTimeSpanString(lastMod,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString());
            meta.setTextColor(Color.parseColor("#2E9BE6"));
            meta.setTextSize(11);
            meta.setPadding(0, 4 * d, 0, 0);
            row.addView(meta);
        }

        row.setClickable(true);
        row.setOnClickListener(v -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(JTX_VIEW_URI, String.valueOf(id)));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {}
            if (close != null) close.run();
        });
        return row;
    }
}
