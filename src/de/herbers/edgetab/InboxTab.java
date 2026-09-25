package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Posteingang-Karte: zeigt die abgefangenen Benachrichtigungen der gewaehlten
 * Quell-Apps. Ungelesen fett, gelesen gedaempft.
 *
 *  - Tippen oeffnet die KONKRETE Mail (ueber den gemerkten contentIntent),
 *    nicht nur die App-Uebersicht; faellt nur zurueck auf die App, wenn der
 *    Intent nicht mehr verfuegbar ist.
 *  - Das X rechts loest die Loeschen-Aktion der Benachrichtigung aus (loescht
 *    also auch in der Quell-App) und entfernt sie aus Statusleiste und Ablage.
 *  - Groessere Vorschau (mehr Zeilen), damit man wie im Systemmenue lesen kann.
 */
public class InboxTab extends BaseTab {

    // Welche Eintraege gerade ihr Antwortfeld aufgeklappt haben - ueberlebt
    // einen Panel-Neuaufbau (wie MENU_OPEN/ADDING in anderen Karten).
    private static final Set<String> REPLYING = new HashSet<>();

    // Gewaehlter Kategorie-Schnellfilter (null = alle) - bewusst nicht in
    // Settings gespeichert, nur fuer die laufende Sitzung.
    private static String activeCategory = null;

    public InboxTab(TabInstance inst) { super(inst, R.string.tab_inbox, R.drawable.ic_inbox); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        float fs = Settings.fontScale(ctx);
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        String flat = android.provider.Settings.Secure.getString(
                ctx.getContentResolver(), "enabled_notification_listeners");
        if (flat == null || !flat.contains(ctx.getPackageName())) {
            list.addView(CalendarTab.note(ctx,
                    ctx.getString(R.string.notif_access_missing),
                    "#FFB0B0", fs));
            return scroll;
        }

        Set<String> sources = Settings.sources(ctx);
        if (sources.isEmpty()) {
            list.addView(CalendarTab.note(ctx,
                    ctx.getString(R.string.inbox_no_sources), "#9E9E9E", fs));
            return scroll;
        }

        NotificationStore store = NotificationStore.get(ctx);
        int keepDays = Settings.retentionDays(ctx);
        if (keepDays > 0) {
            store.pruneOlderThan(System.currentTimeMillis() - keepDays * 86400000L);
        }
        store.collapseDuplicates();
        List<NotificationStore.Item> all = store.recent(80, null);

        // Kategorie-Schnellfilter: nur Kategorien anbieten, die unter den
        // gerade aktiven Quellen ueberhaupt vorkommen (wie BlackBerry Hub+'s
        // Kategorie-Zuordnung je App, hier als Chip-Leiste statt Filter-Menue).
        java.util.LinkedHashSet<String> presentCats = new java.util.LinkedHashSet<>();
        for (String pkg : sources) {
            String cat = Settings.category(ctx, pkg);
            if (cat != null) presentCats.add(cat);
        }
        if (activeCategory != null && !presentCats.contains(activeCategory)) activeCategory = null;
        if (presentCats.size() > 1) {
            list.addView(categoryChips(ctx, presentCats, fs, d, refreshContent));
        }

        int unseen = 0;
        for (NotificationStore.Item it : all) if (sources.contains(it.pkg) && !it.seen) unseen++;
        if (unseen > 0) {
            TextView markAll = new TextView(ctx);
            markAll.setText(ctx.getString(R.string.mark_all_read, unseen));
            markAll.setTextColor(Color.parseColor("#2E9BE6"));
            markAll.setTextSize(13 * fs);
            markAll.setPadding(2 * d, 0, 0, 10 * d);
            markAll.setOnClickListener(v -> { store.markAllSeen(null); v.setVisibility(View.GONE); });
            list.addView(markAll);
        }

        PackageManager pm = ctx.getPackageManager();
        java.util.Set<String> live = NotificationCollector.liveIndex();
        int shown = 0;
        long lastDay = -1;
        for (NotificationStore.Item it : all) {
            if (!sources.contains(it.pkg)) continue;
            if (!Settings.isChannelEnabled(ctx, it.pkg, it.channel)) continue;
            if (activeCategory != null && !activeCategory.equals(Settings.category(ctx, it.pkg))) continue;
            long day = dayIndex(it.posted);
            if (day != lastDay) {
                list.addView(dayHeader(ctx, it.posted, fs, d));
                lastDay = day;
            }
            list.addView(row(ctx, it, appLabel(pm, it.pkg), live.contains(it.nkey)
                    || live.contains(NotificationCollector.signature(it.pkg, it.title, it.text)),
                    fs, d, store, close, refreshContent));
            if (++shown >= 40) break;
        }
        if (shown == 0) {
            list.addView(CalendarTab.note(ctx,
                    ctx.getString(R.string.inbox_nothing_yet), "#9E9E9E", fs));
        }
        // Schwebendes Stift-Symbol wie bei Kalender/Kontakte - startet die
        // "mailto:"-Absicht, die Android an die als E-Mail-App eingerichtete
        // App weiterreicht (bei Mathias der Hub). Kein generisches "neue
        // Nachricht" fuer ALLE Quell-Apps moeglich (jede Messaging-App
        // braucht ihre eigene Compose-Ansicht) - E-Mail ist der einzige Fall
        // mit einer wirklich universellen System-Absicht.
        return withFab(ctx, scroll, fab(ctx, R.drawable.ic_fab_edit, "#F5A623", v -> {
            try {
                Intent i = new Intent(Intent.ACTION_SENDTO)
                        .setData(Uri.parse("mailto:"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {}
            close.run();
        }));
    }

    /** Wie im Kalender-Tab: Tageskennung fuer die Gruppierung. */
    private long dayIndex(long millis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(java.util.Calendar.YEAR) * 1000L + c.get(java.util.Calendar.DAY_OF_YEAR);
    }

    /** Zwischenueberschrift HEUTE/GESTERN/VORGESTERN, sonst Wochentag+Datum -
     *  wie im Kalender-Tab, nur rueckwaerts statt vorwaerts (Mathias' Wunsch:
     *  "Nachrichten des Posteinganges [...] gruppieren, wie im Kalender"). */
    private View dayHeader(Context ctx, long posted, float fs, int d) {
        TextView h = new TextView(ctx);
        String label;
        if (DateUtils.isToday(posted)) label = ctx.getString(R.string.day_today).toUpperCase(java.util.Locale.getDefault());
        else if (DateUtils.isToday(posted + DateUtils.DAY_IN_MILLIS)) label = ctx.getString(R.string.day_yesterday).toUpperCase(java.util.Locale.getDefault());
        else if (DateUtils.isToday(posted + 2 * DateUtils.DAY_IN_MILLIS)) label = ctx.getString(R.string.day_before_yesterday).toUpperCase(java.util.Locale.getDefault());
        else label = DateUtils.formatDateTime(ctx, posted,
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_ABBREV_MONTH).toUpperCase(java.util.Locale.getDefault());
        h.setText(label);
        h.setTextColor(Color.parseColor("#7FB0B0B0"));
        h.setTextSize(12 * fs);
        h.setPadding(2 * d, 14 * d, 0, 6 * d);
        return h;
    }

    /** Chip-Leiste "Alle / Kommunikation / Einkaufen / ..." ueber der Liste -
     *  tippen filtert, nochmal tippen hebt den Filter wieder auf. */
    private View categoryChips(Context ctx, java.util.Set<String> cats, float fs, int d, Runnable refreshContent) {
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(ctx);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, 10 * d);
        row.addView(chip(ctx, ctx.getString(R.string.filter_all), 0, activeCategory == null, fs, d, () -> {
            activeCategory = null;
            if (refreshContent != null) refreshContent.run();
        }));
        for (String cat : cats) {
            row.addView(chip(ctx, Settings.categoryLabel(ctx, cat), Settings.categoryColor(cat), cat.equals(activeCategory), fs, d, () -> {
                activeCategory = cat.equals(activeCategory) ? null : cat;
                if (refreshContent != null) refreshContent.run();
            }));
        }
        hsv.addView(row);
        return hsv;
    }

    private View chip(Context ctx, String label, int color, boolean active, float fs, int d, Runnable onTap) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(active ? Color.WHITE : Color.parseColor("#B0B0B5"));
        tv.setTextSize(12 * fs);
        tv.setPadding(12 * d, 6 * d, 12 * d, 6 * d);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(14 * d);
        bg.setColor(active ? (color != 0 ? color : Color.parseColor("#2E9BE6")) : Color.parseColor("#2C2C2E"));
        tv.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = 8 * d;
        tv.setLayoutParams(lp);
        tv.setOnClickListener(v -> onTap.run());
        return tv;
    }

    private View row(Context ctx, NotificationStore.Item it, String app, boolean live,
                     float fs, int d,
                     NotificationStore store, Runnable closePanel, Runnable refreshContent) {
        // Aeusserer Rahmen mit farbigem Balken - wie im Kalender-Tab (dort
        // die Kalenderfarbe, hier eine stabile Farbe je Quell-App), Mathias'
        // Wunsch nach einheitlicherem Design ueber die Karten hinweg.
        LinearLayout outer = new LinearLayout(ctx);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(it.seen ? "#232325" : "#2C2C2E"));
        bg.setCornerRadius(10 * d);
        outer.setBackground(bg);
        outer.setPadding(0, 0, 10 * d, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 6 * d;
        outer.setLayoutParams(lp);

        View bar = new View(ctx);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(5 * d,
                LinearLayout.LayoutParams.MATCH_PARENT);
        barLp.rightMargin = 10 * d;
        bar.setLayoutParams(barLp);
        GradientDrawable barBg = new GradientDrawable();
        int catColor = Settings.categoryColor(Settings.category(ctx, it.pkg));
        barBg.setColor(catColor != 0 ? catColor : colorFor(it.pkg));
        barBg.setCornerRadii(new float[]{10*d,10*d,0,0,0,0,10*d,10*d});
        bar.setBackground(barBg);
        outer.addView(bar);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 10 * d, 0, 10 * d);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        outer.addView(row);

        // Kopf: App-Name links, Zeit + Loeschen rechts
        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView appTv = new TextView(ctx);
        // Nicht mehr in der Statusleiste: Tippen oeffnet nur noch die App,
        // X entfernt nur aus EdgeTab (kein Loeschen in der App moeglich).
        appTv.setText(live ? app : ctx.getString(R.string.inbox_app_only, app));
        appTv.setTextColor(Color.parseColor("#8899AA"));
        appTv.setTextSize(11 * fs);
        appTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(appTv);

        TextView time = new TextView(ctx);
        time.setText(DateUtils.getRelativeTimeSpanString(it.posted,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString());
        time.setTextColor(Color.parseColor("#8899AA"));
        time.setTextSize(11 * fs);
        head.addView(time);

        // Antworten (↩): nur wenn die Benachrichtigung noch lebt UND eine
        // Antworten-Aktion mitbringt - entweder mit echter Direkteingabe
        // (RemoteInput, z.B. Telegram/Molly: klappt ein Textfeld auf) oder
        // ohne (z.B. BB Hubs "Allen antworten" - eine reine startActivity-
        // Aktion, per `dumpsys notification` bestaetigt keine RemoteInput:
        // oeffnet dann direkt Hubs eigenen Antwortbildschirm, wie ein Tipp
        // auf die Aktion in der System-Benachrichtigung selbst).
        String replyKey = String.valueOf(it.id);
        boolean replying = REPLYING.contains(replyKey);
        boolean hasFreeform = NotificationCollector.resolveReplyAction(it) != null;
        boolean hasAnyReply = hasFreeform || NotificationCollector.resolveAnyReplyAction(it) != null;
        if (live && hasAnyReply) {
            TextView reply = new TextView(ctx);
            reply.setText("↩");
            reply.setTextColor(Color.parseColor(replying ? "#2E9BE6" : "#8899AA"));
            reply.setTextSize(17 * fs);
            reply.setPadding(10 * d, 10 * d, 6 * d, 10 * d);
            reply.setOnClickListener(v -> {
                if (hasFreeform) {
                    if (replying) REPLYING.remove(replyKey); else REPLYING.add(replyKey);
                    if (refreshContent != null) refreshContent.run();
                } else {
                    NotificationCollector.openReplyAction(ctx, it, closePanel);
                }
            });
            head.addView(reply);
        }

        // Loeschen (X): entfernt aus Statusleiste und Ablage. Grosszuegiges
        // Polster ringsum - die reine Glyphe war ein zu kleines Ziel zum Treffen.
        TextView del = new TextView(ctx);
        del.setText("✕");
        del.setTextColor(Color.parseColor("#8899AA"));
        del.setTextSize(17 * fs);
        del.setPadding(14 * d, 10 * d, 6 * d, 10 * d);
        del.setOnClickListener(v -> {
            // Vormerken; wirklich geloescht wird erst nach Ablauf der
            // Wiederherstellen-Frist (siehe UndoBar).
            UndoBar.schedule(ctx, it, outer);
        });
        head.addView(del);
        row.addView(head);

        TextView title = new TextView(ctx);
        title.setText(it.title == null || it.title.isEmpty() ? ctx.getString(R.string.no_title) : it.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(14 * fs);
        if (!it.seen) title.setTypeface(null, Typeface.BOLD);
        row.addView(title);

        if (it.text != null && !it.text.isEmpty()) {
            TextView text = new TextView(ctx);
            text.setText(it.text);
            text.setTextColor(Color.parseColor(it.seen ? "#9A9A9A" : "#D0D0D0"));
            text.setTextSize(13 * fs);
            text.setMaxLines(6);              // groessere Vorschau als bisher (war 2)
            row.addView(text);
        }

        if (replying) {
            LinearLayout replyRow = new LinearLayout(ctx);
            replyRow.setOrientation(LinearLayout.HORIZONTAL);
            replyRow.setGravity(Gravity.CENTER_VERTICAL);
            replyRow.setPadding(0, 8 * d, 0, 0);

            EditText input = new EditText(ctx);
            input.setHint(R.string.reply_hint);
            input.setTextColor(Color.WHITE);
            input.setTextSize(13 * fs);
            input.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            replyRow.addView(input);

            Button send = new Button(ctx);
            send.setText(R.string.send_action);
            send.setOnClickListener(v -> {
                String txt = input.getText().toString().trim();
                if (txt.isEmpty()) return;
                boolean ok = NotificationCollector.sendReply(ctx, it, txt);
                Toast.makeText(ctx, ok ? R.string.reply_sent : R.string.reply_failed,
                        Toast.LENGTH_SHORT).show();
                REPLYING.remove(replyKey);
                if (refreshContent != null) refreshContent.run();
            });
            replyRow.addView(send);
            row.addView(replyRow);
        }

        // Tippen: konkrete Mail oeffnen (contentIntent), sonst App. Erst starten,
        // DANN Panel schliessen (falls diese Karte das so eingestellt hat) - und
        // Hintergrund-Start ausdruecklich erlauben, sonst blockiert Android 14
        // den Start aus dem Dienst.
        outer.setClickable(true);
        outer.setOnClickListener(v -> {
            store.markSeen(it.id);
            Launcher.open(ctx, it);
            if (closePanel != null) closePanel.run();
        });
        return outer;
    }

    /** Der im Gerät eingestellte, uebersetzte App-Name statt des Paketnamens. */
    private String appLabel(PackageManager pm, String pkg) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(ai);
            if (label != null && label.length() > 0
                    && !label.toString().equals(pkg)) {
                return label.toString();
            }
        } catch (Exception ignored) {}
        // Notnagel: letzten Namensteil hübscher machen (com.blackberry.hub -> Hub)
        String tail = pkg.contains(".") ? pkg.substring(pkg.lastIndexOf('.') + 1) : pkg;
        if (tail.isEmpty()) return pkg;
        return Character.toUpperCase(tail.charAt(0)) + tail.substring(1);
    }
}
