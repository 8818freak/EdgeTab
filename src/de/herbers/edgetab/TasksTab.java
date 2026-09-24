package de.herbers.edgetab;

import android.content.ContentValues;
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
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.res.ColorStateList;

/**
 * Aufgaben-Karte über den offenen Content-Provider von <b>JTX Board</b>
 * (nicht per CalDAV - JTX Board haelt seine per DAVx5 synchronisierten
 * Aufgaben lokal in einer eigenen, offen dokumentierten Tabelle vor, genau
 * wie Android das mit dem Kalender macht). Authority
 * {@code at.techbee.jtx.provider}, Berechtigung
 * {@code at.techbee.jtx.permission.READ} - eine ganz normale ("dangerous"),
 * NICHT signaturgeschuetzte Laufzeit-Berechtigung, die JTX Board selbst
 * deklariert. Kein Server-Zugriff, kein CalDAV-Client in EdgeTab noetig.
 *
 * Wird JTX Board irgendwann durch etwas anderes ersetzt, muss nur diese
 * Klasse angepasst werden - der Rest der App bleibt unberuehrt.
 */
public class TasksTab extends BaseTab {

    private static final String JTX_PACKAGE = "at.techbee.jtx";
    private static final String JTX_PERMISSION = "at.techbee.jtx.permission.READ";
    private static final String JTX_WRITE_PERMISSION = "at.techbee.jtx.permission.WRITE";
    private static final Uri JTX_URI =
            Uri.parse("content://at.techbee.jtx.provider/icalobject");
    private static final Uri JTX_VIEW_URI =
            Uri.parse("content://at.techbee.jtx/icalobject");

    TasksTab(TabInstance inst) { super(inst, "Aufgaben", R.drawable.ic_tasks); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);

        if (!isInstalled(ctx)) {
            return hint(ctx, "JTX Board ist nicht installiert.\nDiese Karte "
                    + "liest Aufgaben aus JTX Board (über DAVx5 synchronisiert).", null, d);
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

        // JTX Boards Content-Provider ist kontobezogen (dieselbe Route, ueber
        // die auch DAVx5 selbst synchronisiert) - ohne account_name/account_type
        // als Query-Parameter wirft er intern eine NullPointerException.
        // "caller_is_syncadapter=true" ist Pflicht (siehe SyncContentProvider.
        // isSyncAdapter() - wirft sonst eine IllegalArgumentException), pruefta
        // aber fuer nicht in JTX Board bekannte Apps wie EdgeTab nichts weiter
        // nach - reine Parameter-Anwesenheit, keine echte Berechtigungspruefung.
        Uri accountUri = JTX_URI.buildUpon()
                .appendQueryParameter("caller_is_syncadapter", "true")
                .appendQueryParameter("account_name", accountName)
                .appendQueryParameter("account_type", accountType)
                .build();
        boolean canWrite = ctx.checkSelfPermission(JTX_WRITE_PERMISSION)
                == PackageManager.PERMISSION_GRANTED;

        int shown = 0;
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(accountUri,
                    new String[]{"_id", "summary", "due", "percent", "status"},
                    "module = ? AND (status IS NULL OR status <> ?)",
                    new String[]{"TODO", "COMPLETED"},
                    "due IS NULL, due ASC");
            if (c != null) {
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    String summary = c.getString(1);
                    Long due = c.isNull(2) ? null : c.getLong(2);
                    Integer percent = c.isNull(3) ? null : c.getInt(3);
                    root.addView(taskRow(ctx, id, summary, due, percent, close, d,
                            canWrite, accountUri, refreshContent));
                    shown++;
                }
            }
        } catch (Exception e) {
            android.util.Log.w("EdgeTabTasks", "Abfrage fehlgeschlagen", e);
            return hint(ctx, "Aufgaben konnten nicht gelesen werden.\n"
                    + "Ist JTX Board aktuell genug?", null, d);
        } finally {
            if (c != null) c.close();
        }

        if (shown == 0) {
            TextView t = new TextView(ctx);
            t.setText("Keine offenen Aufgaben.");
            t.setTextColor(Color.parseColor("#9E9E9E"));
            t.setTextSize(14);
            root.addView(t);
        }
        // Schwebendes Stift-Symbol wie bei Kalender/Kontakte/Posteingang -
        // JTX Board hat (anders als Android bei Kalender/Kontakten) keine
        // universelle "neue Aufgabe"-Systemabsicht, darum oeffnet der Knopf
        // die App selbst statt eines konkreten leeren Formulars.
        return withFab(ctx, scroll, fab(ctx, R.drawable.ic_fab_edit, "#3DA764", v -> {
            try {
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(JTX_PACKAGE);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                }
            } catch (Exception ignored) {}
            if (close != null) close.run();
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
        t.setText("Für die Aufgaben-Karte fehlt die Berechtigung, JTX Boards "
                + "Aufgaben zu lesen.");
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
        t.setText("Für die Aufgaben-Karte fehlt noch die Auswahl deines "
                + "DAVx5-Kontos (JTX Board liest Aufgaben kontobezogen).");
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

    private View hint(Context ctx, String text, String colorOrNull, int d) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 12 * d, 0, 0);
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(Color.parseColor(colorOrNull != null ? colorOrNull : "#9E9E9E"));
        t.setTextSize(14);
        box.addView(t);
        return box;
    }

    private View taskRow(Context ctx, long id, String summary, Long due, Integer percent,
                          Runnable close, int d, boolean canWrite, Uri accountUri,
                          Runnable refreshContent) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2C2C2E"));
        bg.setCornerRadius(10 * d);
        row.setBackground(bg);
        row.setPadding(12 * d, 10 * d, 12 * d, 10 * d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 6 * d;
        row.setLayoutParams(lp);

        CheckBox done = new CheckBox(ctx);
        done.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#2E9BE6")));
        done.setPadding(0, 0, 8 * d, 0);
        done.setOnClickListener(v -> {
            if (!canWrite) {
                done.setChecked(false);
                Intent i = new Intent(ctx, MainActivity.class);
                i.putExtra("request_permissions", new String[]{JTX_PERMISSION, JTX_WRITE_PERMISSION});
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                if (close != null) close.run();
                return;
            }
            markDone(ctx, id, accountUri);
            if (refreshContent != null) refreshContent.run();
        });
        row.addView(done);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(ctx);
        title.setText(summary == null || summary.isEmpty() ? "(ohne Titel)" : summary);
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        col.addView(title);

        if (due != null || (percent != null && percent > 0)) {
            LinearLayout meta = new LinearLayout(ctx);
            meta.setOrientation(LinearLayout.HORIZONTAL);
            meta.setGravity(Gravity.CENTER_VERTICAL);
            meta.setPadding(0, 4 * d, 0, 0);

            if (due != null) {
                TextView dueTv = new TextView(ctx);
                boolean overdue = due < System.currentTimeMillis();
                dueTv.setText(DateUtils.getRelativeTimeSpanString(due,
                        System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString());
                dueTv.setTextColor(Color.parseColor(overdue ? "#FF8080" : "#2E9BE6"));
                dueTv.setTextSize(12);
                meta.addView(dueTv, new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            } else {
                meta.addView(new View(ctx), new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            }
            if (percent != null && percent > 0) {
                TextView pct = new TextView(ctx);
                pct.setText(percent + " %");
                pct.setTextColor(Color.parseColor("#9E9E9E"));
                pct.setTextSize(12);
                meta.addView(pct);
            }
            col.addView(meta);
        }
        row.addView(col);

        col.setClickable(true);
        col.setOnClickListener(v -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(JTX_VIEW_URI, String.valueOf(id)));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {}
            if (close != null) close.run();
        });
        return row;
    }

    /** Markiert eine Aufgabe direkt in JTX Boards Datenbank als erledigt.
     *  Das zugrunde liegende Update ist ein reines SQL-UPDATE ohne
     *  automatische Sync-Markierung - "dirty=1" muss selbst gesetzt werden,
     *  sonst würde DAVx5 die Erledigung nie zum Server hochladen.
     *  Wiederkehrende Aufgaben (RRULE) rücken dabei NICHT automatisch zum
     *  naechsten Termin vor - das macht nur JTX Boards eigene App. */
    private void markDone(Context ctx, long id, Uri accountUri) {
        Uri target = accountUri.buildUpon()
                .appendPath(String.valueOf(id))
                .build();
        // appendPath haengt HINTER die vorhandenen Query-Parameter an den
        // Pfad an (Uri.Builder trennt Pfad/Query intern) - das Ergebnis passt
        // trotzdem zum "icalobject/#"-Muster des UriMatchers.
        ContentValues cv = new ContentValues();
        cv.put("status", "COMPLETED");
        cv.put("percent", 100);
        cv.put("completed", System.currentTimeMillis());
        cv.put("dirty", 1);
        try {
            ctx.getContentResolver().update(target, cv, null, null);
        } catch (Exception e) {
            android.util.Log.w("EdgeTabTasks", "Abhaken fehlgeschlagen", e);
        }
    }
}
