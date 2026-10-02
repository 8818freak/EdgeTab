package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.service.notification.StatusBarNotification;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
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
    private static boolean searchOpen = false;      // Lupe angetippt -> Suchfeld sichtbar
    private static String inboxQuery = "";           // aktueller Filterbegriff
    private static int inboxScrollY = -1;            // Scroll-Position ueber Neuaufbau hinweg merken

    // Einmaliger Schnappschuss aller aktiven Benachrichtigungen je buildContent-
    // Lauf (Hauptthread, synchron) - statt je Zeile das System zu fragen.
    private StatusBarNotification[] snap;

    public InboxTab(TabInstance inst) { super(inst, R.string.tab_inbox, R.drawable.ic_inbox); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        float fs = Settings.fontScale(ctx);
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        // Scroll-Position dauerhaft merken - auch ueber das Schliessen des Panels
        // und das Umschalten auf ein anderes Programm hinweg (Mathias' Wunsch).
        // scrollReady[0] verhindert, dass die Scroll-Ereignisse waehrend des
        // Neuaufbaus (Position noch 0) den gemerkten Wert ueberschreiben, bevor
        // er wiederhergestellt ist.
        final boolean[] scrollReady = {false};
        scroll.setOnScrollChangeListener((v, sx, sy, ox, oy) -> { if (scrollReady[0]) inboxScrollY = sy; });

        // Beim Aufklappen/Antworten baut das Panel neu auf - dabei zuerst die
        // Scroll-Position merken, damit die Liste nicht an den Anfang springt.
        final Runnable refresh = () -> {
            inboxScrollY = scroll.getScrollY();
            if (refreshContent != null) refreshContent.run();
        };

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
        // Keine feste Anzahl-Grenze mehr: alle Eintraege der letzten N Tage
        // (einstellbar, bis 999) - siehe Settings.listDays.
        List<NotificationStore.Item> all = store.recentSince(Settings.listCutoff(ctx), null);

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
            list.addView(categoryChips(ctx, presentCats, fs, d, refresh));
        }

        list.addView(searchBar(ctx, fs, d, refresh, closePanel));

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
        snap = NotificationCollector.activeSnapshot();
        java.util.Set<String> live = NotificationCollector.liveIndexFrom(snap);

        // 1) Kandidaten sammeln: Benachrichtigungen + optional SMS/Anrufe.
        List<Entry> entries = new ArrayList<>();
        for (NotificationStore.Item it : all) {
            if (!sources.contains(it.pkg)) continue;
            if (!Settings.isChannelEnabled(ctx, it.pkg, it.channel)) continue;
            if (activeCategory != null && !activeCategory.equals(Settings.category(ctx, it.pkg))) continue;
            if (!inboxQuery.isEmpty() && !matchesQuery(it)) continue;
            entries.add(Entry.notif(it, it.pkg + "|" + (it.title == null ? "" : it.title.trim().toLowerCase())));
        }
        if (activeCategory == null) { // SMS/Anrufe haben keine Kategorie -> nur in "Alle"
            if (Settings.smsInInbox(ctx)
                    && ctx.checkSelfPermission(android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                entries.addAll(smsEntries(ctx));
            }
            if (Settings.callsInInbox(ctx)
                    && ctx.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                entries.addAll(callEntries(ctx));
            }
        }
        java.util.Collections.sort(entries, (a, b) -> Long.compare(b.time, a.time));

        // 2) zu Konversationen gruppieren (optional).
        boolean group = Settings.groupConversations(ctx);
        java.util.LinkedHashMap<String, List<Entry>> groups = new java.util.LinkedHashMap<>();
        if (group) {
            for (Entry e : entries) {
                List<Entry> g = groups.get(e.convKey);
                if (g == null) { g = new ArrayList<>(); groups.put(e.convKey, g); }
                g.add(e);
            }
        } else {
            int i = 0;
            for (Entry e : entries) { List<Entry> g = new ArrayList<>(); g.add(e); groups.put("_" + (i++), g); }
        }

        // 3) rendern, Tagesueberschrift nach dem neuesten Eintrag der Gruppe.
        int shown = 0;
        long lastDay = -1;
        for (List<Entry> g : groups.values()) {
            Entry newest = g.get(0);
            long day = dayIndex(newest.time);
            if (day != lastDay) { list.addView(dayHeader(ctx, newest.time, fs, d)); lastDay = day; }
            if (g.size() == 1) {
                list.addView(renderEntry(ctx, newest, pm, live, fs, d, store, close, refresh));
            } else {
                list.addView(conversationBlock(ctx, g, pm, live, fs, d, store, close, refresh));
            }
            if (++shown >= 40) break;
        }
        if (shown == 0) {
            list.addView(CalendarTab.note(ctx,
                    ctx.getString(R.string.inbox_nothing_yet), "#9E9E9E", fs));
        }
        // Scroll-Position nach dem Neuaufbau wiederherstellen; danach erst das
        // Merken scharf schalten (siehe scrollReady), damit das Zuruecksetzen auf
        // 0 beim Aufbau den gemerkten Wert nicht loescht.
        final int targetY = inboxScrollY;
        scroll.post(() -> {
            if (targetY > 0) scroll.scrollTo(0, targetY);
            scroll.post(() -> scrollReady[0] = true);
        });
        // Schwebendes Stift-Symbol wie bei Kalender/Kontakte - startet die
        // "mailto:"-Absicht, die Android an die als E-Mail-App eingerichtete
        // App weiterreicht (bei Mathias der Hub). Kein generisches "neue
        // Nachricht" fuer ALLE Quell-Apps moeglich (jede Messaging-App
        // braucht ihre eigene Compose-Ansicht) - E-Mail ist der einzige Fall
        // mit einer wirklich universellen System-Absicht.
        View compose = fab(ctx, R.drawable.ic_fab_edit, "#F5A623", v -> {
            try {
                Intent i = new Intent(Intent.ACTION_SENDTO)
                        .setData(Uri.parse("mailto:"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {}
            close.run();
        });
        // "Nach oben"-Knopf (erscheint nur beim Scrollen, Seite folgt dem
        // Andock-Rand) kommt aus dem gemeinsamen Helfer - siehe withScrollTop.
        return withScrollTop(ctx, scroll, compose);
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

    // ---------- Vereinheitlichte Eintraege (Benachrichtigung / SMS / Anruf) ----------

    private static final Set<String> CONV_OPEN = new HashSet<>();

    static final class Entry {
        long time; String convKey; int kind; // 0=notif, 1=sms, 2=call
        NotificationStore.Item item;         // kind 0
        long rowId;                          // kind 1/2 (sms _id / call _id)
        String addr, body; boolean incoming; // kind 1 (SMS)
        String callName, callNumber; int callType; boolean callNew; boolean callFailed; // kind 2
        int callNumberType; String callNumberLabel; // Kennung (Mobil/Arbeit/…)

        static Entry notif(NotificationStore.Item it, String convKey) {
            Entry e = new Entry(); e.kind = 0; e.item = it; e.time = it.posted; e.convKey = convKey; return e;
        }
    }

    private List<Entry> smsEntries(Context ctx) {
        List<Entry> out = new ArrayList<>();
        String q = inboxQuery.toLowerCase();
        try (android.database.Cursor c = ctx.getContentResolver().query(android.net.Uri.parse("content://sms"),
                new String[]{"_id", "address", "body", "date", "type"},
                "date >= ?", new String[]{String.valueOf(Settings.listCutoff(ctx))}, "date DESC")) {
            if (c != null) {
                while (c.moveToNext() && out.size() < 5000) {
                    String addr = c.getString(1), body = c.getString(2);
                    if (body == null) continue;
                    String name = TabPermHint.contactName(ctx, addr);
                    if (!q.isEmpty() && !body.toLowerCase().contains(q)
                            && (name == null || !name.toLowerCase().contains(q))
                            && (addr == null || !addr.toLowerCase().contains(q))) continue;
                    Entry e = new Entry();
                    e.kind = 1; e.rowId = c.getLong(0); e.addr = addr; e.body = body;
                    e.time = c.isNull(3) ? 0 : c.getLong(3);
                    e.incoming = c.getInt(4) == 1;
                    e.convKey = "sms|" + (name != null ? name : (addr == null ? "?" : addr));
                    out.add(e);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private List<Entry> callEntries(Context ctx) {
        List<Entry> out = new ArrayList<>();
        String q = inboxQuery.toLowerCase();
        try (android.database.Cursor c = ctx.getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI,
                new String[]{android.provider.CallLog.Calls._ID, android.provider.CallLog.Calls.NUMBER,
                        android.provider.CallLog.Calls.CACHED_NAME, android.provider.CallLog.Calls.TYPE,
                        android.provider.CallLog.Calls.DATE, android.provider.CallLog.Calls.NEW,
                        android.provider.CallLog.Calls.DURATION,
                        android.provider.CallLog.Calls.CACHED_NUMBER_TYPE,
                        android.provider.CallLog.Calls.CACHED_NUMBER_LABEL},
                android.provider.CallLog.Calls.DATE + " >= ?",
                new String[]{String.valueOf(Settings.listCutoff(ctx))},
                android.provider.CallLog.Calls.DATE + " DESC")) {
            if (c != null) {
                while (c.moveToNext() && out.size() < 5000) {
                    String num = c.getString(1), name = c.getString(2);
                    if (name == null || name.isEmpty()) name = TabPermHint.contactName(ctx, num);
                    if (!q.isEmpty() && (num == null || !num.toLowerCase().contains(q))
                            && (name == null || !name.toLowerCase().contains(q))) continue;
                    Entry e = new Entry();
                    e.kind = 2; e.rowId = c.getLong(0); e.callNumber = num; e.callName = name;
                    e.callType = c.getInt(3);
                    e.time = c.isNull(4) ? 0 : c.getLong(4);
                    e.callNew = c.getInt(5) == 1;
                    long dur = c.isNull(6) ? 0 : c.getLong(6);
                    e.callFailed = e.callType == android.provider.CallLog.Calls.OUTGOING_TYPE && dur == 0;
                    e.callNumberType = c.isNull(7) ? 0 : c.getInt(7);
                    e.callNumberLabel = c.getString(8);
                    e.convKey = "call|" + (num == null ? "?" : num);
                    out.add(e);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private View renderEntry(Context ctx, Entry e, PackageManager pm, java.util.Set<String> live,
                            float fs, int d, NotificationStore store, Runnable close, Runnable refreshContent) {
        if (e.kind == 1) return smsRow(ctx, e, fs, d, close, refreshContent);
        if (e.kind == 2) return callRow(ctx, e, fs, d, close, refreshContent);
        NotificationStore.Item it = e.item;
        return row(ctx, it, appLabel(pm, it.pkg), live.contains(it.nkey)
                || live.contains(NotificationCollector.signature(it.pkg, it.title, it.text)),
                fs, d, store, close, refreshContent);
    }

    /** Aufklappbarer Konversationsblock: neuester Eintrag + "N Nachrichten"-
     *  Umschalter; aufgeklappt werden alle Eintraege der Konversation gezeigt. */
    private View conversationBlock(Context ctx, List<Entry> g, PackageManager pm, java.util.Set<String> live,
                                   float fs, int d, NotificationStore store, Runnable close, Runnable refreshContent) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        String key = g.get(0).convKey;
        boolean open = CONV_OPEN.contains(key);

        // Immer den neuesten Eintrag zeigen.
        box.addView(renderEntry(ctx, g.get(0), pm, live, fs, d, store, close, refreshContent));

        TextView toggle = new TextView(ctx);
        toggle.setText((open ? "▾   " : "▸   ") + (g.size() - 1) + " weitere in dieser Konversation");
        toggle.setTextColor(Color.parseColor("#2E9BE6"));
        toggle.setTextSize(14 * fs);
        // Grosses, leicht treffbares Ziel (volle Breite, hohe Trefferflaeche).
        GradientDrawable tbg = new GradientDrawable();
        tbg.setColor(Color.parseColor("#22314A"));
        tbg.setCornerRadius(8 * d);
        toggle.setBackground(tbg);
        toggle.setPadding(14 * d, 12 * d, 14 * d, 12 * d);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = 2 * d; tlp.bottomMargin = 6 * d;
        toggle.setLayoutParams(tlp);
        toggle.setOnClickListener(v -> {
            if (open) CONV_OPEN.remove(key); else CONV_OPEN.add(key);
            if (refreshContent != null) refreshContent.run();
        });
        box.addView(toggle);

        if (open) {
            for (int i = 1; i < g.size(); i++) {
                View child = renderEntry(ctx, g.get(i), pm, live, fs, d, store, close, refreshContent);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.leftMargin = 10 * d;
                child.setLayoutParams(lp);
                box.addView(child);
            }
        }
        return box;
    }

    private static final Set<String> SMS_REPLYING = new HashSet<>();

    private View smsRow(Context ctx, Entry e, float fs, int d, Runnable close, Runnable refreshContent) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2C2C2E"));
        bg.setCornerRadius(10 * d);
        box.setBackground(bg);
        box.setPadding(10 * d, 8 * d, 10 * d, 8 * d);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxLp.bottomMargin = 6 * d;
        box.setLayoutParams(boxLp);

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
        String name = TabPermHint.contactName(ctx, e.addr);
        TextView who = new TextView(ctx);
        who.setText((e.incoming ? "" : "→ ") + (name != null ? name : (e.addr == null ? "?" : e.addr)));
        who.setTextColor(Color.parseColor("#8899AA"));
        who.setTextSize(11 * fs);
        who.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(who);
        // Antworten (nur bei empfangenen SMS sinnvoll).
        final String replyKey = "sms" + e.rowId;
        final boolean replying = SMS_REPLYING.contains(replyKey);
        final String addr = e.addr;
        if (e.incoming && addr != null && !addr.isEmpty()) {
            TextView reply = new TextView(ctx);
            reply.setText("↩");
            reply.setTextColor(Color.parseColor(replying ? "#2E9BE6" : "#8899AA"));
            reply.setTextSize(17 * fs);
            reply.setPadding(10 * d, 6 * d, 6 * d, 6 * d);
            reply.setOnClickListener(v -> {
                if (replying) SMS_REPLYING.remove(replyKey); else SMS_REPLYING.add(replyKey);
                if (refreshContent != null) refreshContent.run();
            });
            head.addView(reply);
        }
        box.addView(head);

        TextView body = new TextView(ctx);
        body.setText(e.body);
        body.setTextColor(Color.WHITE);
        body.setTextSize(13 * fs);
        box.addView(body);
        if (e.time > 0) {
            TextView t = new TextView(ctx);
            t.setText(Settings.formatTime(ctx, e.time));
            t.setTextColor(Color.parseColor("#8899AA"));
            t.setTextSize(11 * fs);
            box.addView(t);
        }

        if (replying) {
            LinearLayout rr = new LinearLayout(ctx);
            rr.setOrientation(LinearLayout.HORIZONTAL);
            rr.setGravity(Gravity.CENTER_VERTICAL);
            rr.setPadding(0, 8 * d, 0, 0);
            EditText input = new EditText(ctx);
            input.setHintTextColor(android.graphics.Color.parseColor("#9AA6B2"));
            input.setHint(R.string.reply_hint);
            input.setTextColor(Color.WHITE);
            input.setTextSize(13 * fs);
            input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            enableClipboardPaste(input);
            rr.addView(input);
            Button send = new Button(ctx);
            send.setText(R.string.send_action);
            send.setOnClickListener(v -> {
                String txt = input.getText().toString().trim();
                if (txt.isEmpty()) return;
                if (ctx.checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    Intent i = new Intent(ctx, MainActivity.class)
                            .putExtra("request_permission", android.Manifest.permission.SEND_SMS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    if (close != null) close.run();
                    return;
                }
                boolean ok = sendSms(ctx, addr, txt);
                android.widget.Toast.makeText(ctx, ok ? R.string.reply_sent : R.string.reply_failed,
                        android.widget.Toast.LENGTH_SHORT).show();
                SMS_REPLYING.remove(replyKey);
                if (refreshContent != null) refreshContent.run();
            });
            rr.addView(send);
            box.addView(rr);
        }

        box.setClickable(true);
        box.setOnClickListener(v -> {
            try {
                ctx.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:" + (addr == null ? "" : addr)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {}
            if (close != null) close.run();
        });
        return box;
    }

    private boolean sendSms(Context ctx, String addr, String text) {
        try {
            android.telephony.SmsManager sm = ctx.getSystemService(android.telephony.SmsManager.class);
            if (sm == null) sm = android.telephony.SmsManager.getDefault();
            java.util.ArrayList<String> parts = sm.divideMessage(text);
            sm.sendMultipartTextMessage(addr, null, parts, null, null);
            return true;
        } catch (Throwable t) { return false; }
    }

    private View callRow(Context ctx, Entry e, float fs, int d, Runnable close, Runnable refreshContent) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2C2C2E"));
        bg.setCornerRadius(10 * d);
        box.setBackground(bg);
        box.setPadding(10 * d, 8 * d, 10 * d, 8 * d);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxLp.bottomMargin = 6 * d;
        box.setLayoutParams(boxLp);

        boolean missed = e.callType == android.provider.CallLog.Calls.MISSED_TYPE;
        // Farbe: gruen = erfolgreich, rot = verpasst, blau = vergeblich (abgehend, niemanden erreicht).
        String callColor = missed ? "#E0533A" : (e.callFailed ? "#2E9BE6" : "#5BD68A");
        ImageView icon = new ImageView(ctx);
        icon.setImageResource(R.drawable.ic_call);
        icon.setColorFilter(Color.parseColor(callColor));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(20 * d, 20 * d);
        ilp.rightMargin = 10 * d;
        icon.setLayoutParams(ilp);
        box.addView(icon);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView who = new TextView(ctx);
        boolean haveName = e.callName != null && !e.callName.isEmpty();
        who.setText(haveName ? e.callName : (e.callNumber == null ? "?" : e.callNumber));
        who.setTextColor(Color.WHITE);
        who.setTextSize(14 * fs);
        col.addView(who);

        // Rufnummern-Kennung (Mobil/Arbeit/Privat …); bei bekannten Kontakten
        // zusaetzlich die tatsaechliche Nummer.
        String callKind = CallActions.typeLabel(ctx, e.callNumberType, e.callNumberLabel);
        StringBuilder idLine = new StringBuilder();
        if (!callKind.isEmpty()) idLine.append(callKind);
        if (haveName && e.callNumber != null && !e.callNumber.isEmpty()) {
            if (idLine.length() > 0) idLine.append(" · ");
            idLine.append(e.callNumber);
        }
        if (idLine.length() > 0) {
            TextView idv = new TextView(ctx);
            idv.setText(idLine.toString());
            idv.setTextColor(Color.parseColor("#8899AA"));
            idv.setTextSize(11 * fs);
            col.addView(idv);
        }

        String dir = missed ? "verpasst"
                : (e.callType == android.provider.CallLog.Calls.OUTGOING_TYPE
                    ? (e.callFailed ? "ausgehend · nicht erreicht" : "ausgehend") : "eingehend");
        String when = Settings.formatTime(ctx, e.time);
        TextView sub = new TextView(ctx);
        sub.setText((dir.isEmpty() ? "" : dir + " · ") + when);
        sub.setTextColor(Color.parseColor(callColor));
        sub.setTextSize(11 * fs);
        col.addView(sub);
        box.addView(col);

        // Verpasste, noch "neue" Anrufe als gesehen markieren (CallLog NEW/IS_READ).
        if (missed && e.callNew) {
            TextView seen = new TextView(ctx);
            seen.setText("✓");
            seen.setTextColor(Color.parseColor("#5BD68A"));
            seen.setTextSize(18 * fs);
            seen.setPadding(10 * d, 6 * d, 6 * d, 6 * d);
            final long id = e.rowId;
            seen.setOnClickListener(v -> {
                markCallSeen(ctx, id);
                if (refreshContent != null) refreshContent.run();
            });
            box.addView(seen);
        }

        // Kurzer Tipp: direkt anrufen. Langer Tipp: Menue (Wählen, SMS, Kontakt,
        // kopieren, löschen) ein-/ausklappen - wie in der Anrufliste.
        final String num = e.callNumber;
        final long rid = e.rowId;
        box.setClickable(true);
        box.setLongClickable(true);
        box.setOnClickListener(v -> CallActions.call(ctx, num, close));
        box.setOnLongClickListener(v -> {
            if (CallActions.MENU_OPEN.contains(rid)) CallActions.MENU_OPEN.remove(rid);
            else CallActions.MENU_OPEN.add(rid);
            if (refreshContent != null) refreshContent.run();
            return true;
        });
        if (!CallActions.MENU_OPEN.contains(rid)) return box;
        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(box);
        wrap.addView(CallActions.menu(ctx, rid, num, fs, d, close, refreshContent));
        return wrap;
    }

    private void markCallSeen(Context ctx, long id) {
        if (ctx.checkSelfPermission(android.Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(ctx, MainActivity.class)
                    .putExtra("request_permission", android.Manifest.permission.WRITE_CALL_LOG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return;
        }
        try {
            android.content.ContentValues cv = new android.content.ContentValues();
            cv.put(android.provider.CallLog.Calls.NEW, 0);
            cv.put(android.provider.CallLog.Calls.IS_READ, 1);
            ctx.getContentResolver().update(android.provider.CallLog.Calls.CONTENT_URI, cv,
                    android.provider.CallLog.Calls._ID + "=?", new String[]{String.valueOf(id)});
        } catch (Throwable ignored) {}
    }

    private boolean matchesQuery(NotificationStore.Item it) {
        String q = inboxQuery.toLowerCase();
        return (it.title != null && it.title.toLowerCase().contains(q))
                || (it.text != null && it.text.toLowerCase().contains(q));
    }

    /** Suchleiste: Lupe -> Suchfeld. "Hier" filtert diesen Posteingang live,
     *  "In Sucher" oeffnet (falls installiert) den Sucher mit dem Begriff fuer
     *  die Volltextsuche ueber alles; sonst bleibt es bei der lokalen Liste. */
    private View searchBar(Context ctx, float fs, int d, Runnable refreshContent, Runnable closePanel) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, 8 * d);

        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView lupe = new TextView(ctx);
        lupe.setText(searchOpen ? "🔍  Suche schließen" : "🔍  Posteingang durchsuchen");
        lupe.setTextColor(Color.parseColor("#8899AA"));
        lupe.setTextSize(13 * fs);
        lupe.setPadding(2 * d, 4 * d, 2 * d, 4 * d);
        lupe.setOnClickListener(v -> {
            searchOpen = !searchOpen;
            if (!searchOpen) inboxQuery = "";
            if (refreshContent != null) refreshContent.run();
        });
        topRow.addView(lupe);
        box.addView(topRow);

        if (!searchOpen) return box;

        final EditText input = new EditText(ctx);
        input.setHintTextColor(android.graphics.Color.parseColor("#9AA6B2"));
        input.setTextColor(android.graphics.Color.WHITE);
        input.setHint("Suchbegriff…");
        input.setText(inboxQuery);
        input.setSelection(inboxQuery.length());
        input.setTextColor(Color.WHITE);
        input.setTextSize(14 * fs);
        input.setSingleLine(true);
        enableClipboardPaste(input);
        box.addView(input);

        LinearLayout btns = new LinearLayout(ctx);
        btns.setOrientation(LinearLayout.HORIZONTAL);

        Button here = new Button(ctx);
        here.setText("Hier suchen");
        here.setOnClickListener(v -> {
            inboxQuery = input.getText().toString().trim();
            if (refreshContent != null) refreshContent.run();
        });
        btns.addView(here);

        Button inSucher = new Button(ctx);
        inSucher.setText("In Sucher suchen");
        inSucher.setOnClickListener(v -> {
            String q = input.getText().toString().trim();
            try {
                Intent i = new Intent()
                        .setClassName("de.herbers.sucher", "de.herbers.sucher.MainActivity")
                        .putExtra("query", q)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ctx.startActivity(i);
                if (closePanel != null) closePanel.run();
            } catch (Exception e) {
                android.widget.Toast.makeText(ctx, "Sucher ist nicht installiert – hier gefiltert.",
                        android.widget.Toast.LENGTH_SHORT).show();
                inboxQuery = q;
                if (refreshContent != null) refreshContent.run();
            }
        });
        btns.addView(inSucher);
        box.addView(btns);
        return box;
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

        // Kopf: [Icon] App-Name links, Zeit + Loeschen rechts
        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        // Benachrichtigungs-Icon (Statusleisten-Icon der App, eingefaerbt) -
        // nur solange die Benachrichtigung lebt (steckt in ihr). Auswertung aus
        // der gemeinsamen Bibliothek (Notifications.smallIcon).
        if (live) {
            StatusBarNotification sbnIcon = NotificationCollector.resolveFrom(snap, it);
            android.graphics.Bitmap ic = sbnIcon == null ? null
                    : de.herbers.common.Notifications.smallIcon(ctx, sbnIcon.getNotification());
            if (ic != null) {
                android.widget.ImageView iv = new android.widget.ImageView(ctx);
                iv.setImageBitmap(ic);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(15 * d, 15 * d);
                ilp.rightMargin = 6 * d;
                iv.setLayoutParams(ilp);
                head.addView(iv);
            }
        }

        TextView appTv = new TextView(ctx);
        // Nicht mehr in der Statusleiste: Tippen oeffnet nur noch die App,
        // X entfernt nur aus EdgeTab (kein Loeschen in der App moeglich).
        appTv.setText(live ? app : ctx.getString(R.string.inbox_app_only, app));
        appTv.setTextColor(Color.parseColor("#8899AA"));
        appTv.setTextSize(11 * fs);
        // Einzeilig + Ellipse: sonst quetscht ein langer Nachbar (frueher der
        // Zeitstempel) die Spalte auf 0 und der Text bricht Buchstabe fuer
        // Buchstabe senkrecht um.
        appTv.setSingleLine(true);
        appTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        appTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(appTv);

        // Zeitstempel (jetzt Datum+Uhrzeit, also lang): NICHT mehr in die
        // Kopfzeile neben den App-Namen (das quetschte ihn), sondern als eigene
        // schmale Zeile darunter (siehe unten, nach head).
        TextView time = new TextView(ctx);
        time.setText(Settings.formatTime(ctx, it.posted));
        time.setTextColor(Color.parseColor("#8899AA"));
        time.setTextSize(11 * fs);
        time.setSingleLine(true);

        // Antworten (↩): nur wenn die Benachrichtigung noch lebt UND eine
        // Antworten-Aktion mitbringt - entweder mit echter Direkteingabe
        // (RemoteInput, z.B. Telegram/Molly: klappt ein Textfeld auf) oder
        // ohne (z.B. BB Hubs "Allen antworten" - eine reine startActivity-
        // Aktion, per `dumpsys notification` bestaetigt keine RemoteInput:
        // oeffnet dann direkt Hubs eigenen Antwortbildschirm, wie ein Tipp
        // auf die Aktion in der System-Benachrichtigung selbst).
        String replyKey = String.valueOf(it.id);
        boolean replying = REPLYING.contains(replyKey);
        // Live ODER gemerkter Draht - so bleibt Antworten in allen Konversationen
        // moeglich, auch wenn die Benachrichtigung nicht mehr in der Leiste steht.
        boolean hasFreeform = NotificationCollector.canReplyFreeform(it);
        boolean hasAnyReply = NotificationCollector.canReplyAny(it);
        if (hasAnyReply) {
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

        // Als gelesen (✓): loest die "Als gelesen markieren"-Aktion der
        // System-Benachrichtigung aus (nur wenn die Quell-App eine solche
        // anbietet und die Benachrichtigung noch lebt) UND merkt den Eintrag in
        // EdgeTab als gesehen. Kein Oeffnen, kein Loeschen - Mathias' Wunsch.
        if (NotificationCollector.hasMarkReadAction(it)) {
            TextView markRead = new TextView(ctx);
            markRead.setText("✓");
            markRead.setTextColor(Color.parseColor("#8899AA"));
            markRead.setTextSize(17 * fs);
            markRead.setPadding(10 * d, 10 * d, 6 * d, 10 * d);
            markRead.setOnClickListener(v -> {
                NotificationCollector.markReadInApp(ctx, it);
                store.markSeen(it.id);
                if (refreshContent != null) refreshContent.run();
            });
            head.addView(markRead);
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

        // Zeitstempel als eigene, volle Zeile unter der Kopfzeile.
        if (it.posted > 0) row.addView(time);

        TextView title = new TextView(ctx);
        title.setText(it.title == null || it.title.isEmpty() ? ctx.getString(R.string.no_title) : it.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(14 * fs);
        if (!it.seen) title.setTypeface(null, Typeface.BOLD);
        row.addView(title);

        // Lebende Benachrichtigung einmal aufloesen - fuer reicheren Text
        // (InboxStyle/MessagingStyle/Zusatzzeile) UND das grosse Bild.
        StatusBarNotification liveSbn = live ? NotificationCollector.resolveFrom(snap, it) : null;
        String displayText = it.text == null ? "" : it.text;
        if (liveSbn != null) {
            String rich = de.herbers.common.Notifications.richText(liveSbn.getNotification());
            if (rich != null && rich.length() > displayText.length()) displayText = rich;
        }
        if (!displayText.isEmpty()) {
            TextView text = new TextView(ctx);
            text.setText(displayText);
            text.setTextColor(Color.parseColor(it.seen ? "#9A9A9A" : "#D0D0D0"));
            text.setTextSize(13 * fs);
            // Volle Laenge (Mathias' Wunsch: alles zeigen, was aus der
            // Benachrichtigung auslesbar ist).
            row.addView(text);
        }

        // Grosses Bild (BigPictureStyle, z.B. Foto in einer Chat-Nachricht) -
        // nur solange die Benachrichtigung noch lebt (das Bild steckt in ihr,
        // nicht in unserer Ablage). Auswertung kommt aus der gemeinsamen
        // Bibliothek (Notifications.bigPicture).
        if (liveSbn != null) {
            {
                android.graphics.Bitmap pic = de.herbers.common.Notifications.bigPicture(ctx, liveSbn.getNotification());
                if (pic != null) {
                    android.widget.ImageView iv = new android.widget.ImageView(ctx);
                    iv.setImageBitmap(pic);
                    iv.setAdjustViewBounds(true);
                    iv.setScaleType(android.widget.ImageView.ScaleType.FIT_START);
                    iv.setMaxHeight(220 * d);
                    LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    ivLp.topMargin = 8 * d;
                    iv.setLayoutParams(ivLp);
                    row.addView(iv);
                }
            }
        }

        // Aus EdgeTab gesendete Antworten als Verlauf unter der Nachricht.
        if (it.sentReplies != null && !it.sentReplies.isEmpty()) {
            for (String ln : it.sentReplies.split("\n")) {
                if (ln.isEmpty()) continue;
                int tab = ln.indexOf('\t');
                String when = "", body = ln;
                if (tab > 0) {
                    try {
                        long ts = Long.parseLong(ln.substring(0, tab));
                        when = Settings.formatTime(ctx, ts);
                    } catch (NumberFormatException ignored) {}
                    body = ln.substring(tab + 1);
                }
                TextView rv = new TextView(ctx);
                rv.setText("↩ " + body + (when.isEmpty() ? "" : "   " + when));
                rv.setTextColor(Color.parseColor("#5BD68A"));
                rv.setTextSize(13 * fs);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rlp.topMargin = 4 * d;
                rv.setLayoutParams(rlp);
                row.addView(rv);
            }
        }

        if (replying) {
            LinearLayout replyRow = new LinearLayout(ctx);
            replyRow.setOrientation(LinearLayout.HORIZONTAL);
            replyRow.setGravity(Gravity.CENTER_VERTICAL);
            replyRow.setPadding(0, 8 * d, 0, 0);

            EditText input = new EditText(ctx);
            input.setHintTextColor(android.graphics.Color.parseColor("#9AA6B2"));
            input.setHint(R.string.reply_hint);
            input.setTextColor(Color.WHITE);
            input.setTextSize(13 * fs);
            input.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            enableClipboardPaste(input);
            replyRow.addView(input);

            Button send = new Button(ctx);
            send.setText(R.string.send_action);
            send.setOnClickListener(v -> {
                String txt = input.getText().toString().trim();
                if (txt.isEmpty()) return;
                boolean ok = NotificationCollector.sendReply(ctx, it, txt);
                Toast.makeText(ctx, ok ? R.string.reply_sent : R.string.reply_failed,
                        Toast.LENGTH_SHORT).show();
                // Gesendete Antwort festhalten - bleibt unter der Nachricht
                // stehen (Verlauf) und schuetzt zugleich die Originalnachricht
                // vor dem Bestaetigungs-Neupost mancher Apps (siehe NotificationStore).
                if (ok) store.addReply(it.id, txt);
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
