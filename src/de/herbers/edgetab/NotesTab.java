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

    // ImapNotes3 zeigt hier keine Notizen an (kein offener Lese-Provider wie
    // bei JTX Board - eigenen IMAP-Client zu bauen waere Aufwand+Risiko fuer
    // Mathias' echte Server-Daten, siehe Uebergabe-MD). Statt dessen wie bei
    // Kalender/Kontakte an die App selbst weiterreichen: ihre eigene
    // Listen-Ansicht nimmt ACTION_SEND text/plain fuer eine neue Notiz an.
    private static final String IMAPNOTES_PACKAGE = "de.niendo.ImapNotes3";

    NotesTab(TabInstance inst) { super(inst, R.string.tab_notes, R.drawable.ic_notes); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);

        if (!isInstalled(ctx)) {
            return withNoteFab(ctx, close, hint(ctx, ctx.getString(R.string.notes_jtx_not_installed), d));
        }
        if (ctx.checkSelfPermission(JTX_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            return withNoteFab(ctx, close, permissionHint(ctx, closePanel, d));
        }
        String accountName = Settings.jtxAccountName(ctx);
        String accountType = Settings.jtxAccountType(ctx);
        if (accountName == null) {
            return withNoteFab(ctx, close, accountHint(ctx, closePanel, d));
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
            return withNoteFab(ctx, close, hint(ctx, ctx.getString(R.string.notes_read_failed), d));
        } finally {
            if (c != null) c.close();
        }

        if (shown == 0) {
            TextView t = new TextView(ctx);
            t.setText(R.string.notes_none_found);
            t.setTextColor(Color.parseColor("#9E9E9E"));
            t.setTextSize(14);
            root.addView(t);
        }
        return withFab(ctx, scroll, fab(ctx, R.drawable.ic_fab_edit, "#3DA764", v -> {
            newNote(ctx);
            close.run();
        }));
    }

    /** Neue Notiz anlegen: direkt an ImapNotes3 weiterreichen (dessen eigene
     *  Listen-Ansicht nimmt ACTION_SEND text/plain fuer eine neue Notiz an -
     *  wie Kalender/Kontakte-"+", nur eben an eine Drittanbieter-App statt an
     *  ein System-ACTION_INSERT). Faellt auf eine offene Auswahl zurueck,
     *  falls ImapNotes3 (noch) nicht installiert ist oder anders heisst. */
    private void newNote(Context ctx) {
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            Intent direct = new Intent(send).setPackage(IMAPNOTES_PACKAGE);
            if (direct.resolveActivity(ctx.getPackageManager()) != null) {
                ctx.startActivity(direct);
                return;
            }
        } catch (Exception ignored) {}
        try {
            ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.notes_create_chooser_title))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {}
    }

    private View withNoteFab(Context ctx, Runnable close, View content) {
        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(content);
        return withFab(ctx, scroll, fab(ctx, R.drawable.ic_fab_edit, "#3DA764", v -> {
            newNote(ctx);
            close.run();
        }));
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
        t.setText(R.string.notes_permission_missing);
        t.setTextColor(Color.parseColor("#CCCCCC"));
        t.setTextSize(14);
        t.setPadding(0, 0, 0, 12 * d);
        box.addView(t);

        Button allow = new Button(ctx);
        allow.setText(R.string.grant_access_button);
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
        t.setText(R.string.notes_account_missing);
        t.setTextColor(Color.parseColor("#CCCCCC"));
        t.setTextSize(14);
        t.setPadding(0, 0, 0, 12 * d);
        box.addView(t);

        Button pick = new Button(ctx);
        pick.setText(R.string.jtx_pick_account_button);
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
        title.setText(summary == null || summary.isEmpty() ? ctx.getString(R.string.no_title) : summary);
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
