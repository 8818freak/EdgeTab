package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

/**
 * Die Einstellungen als Inhalt einer Registerkarte - im Panel selbst, nicht
 * mehr als getrennter Vollbild-Dialog. So wie im Original, wo die Einstellungen
 * der oberste Punkt der Leiste sind.
 *
 * Seit 0.23 zweistufig: ein kurzes Kategorien-Menue statt einer langen,
 * durchgehenden Liste - Tippen auf eine Kategorie zeigt nur deren Einstellungen,
 * "Zurueck" fuehrt zum Menue zurueck. Aenderungen wirken weiterhin sofort.
 */
public class SettingsTab {

    private Context ctx;
    private Runnable closePanel;
    private Runnable refresh;

    /** Welche Kategorie gerade offen ist - null = Kategorien-Menue. Statisch,
     *  weil bei jedem Panel-Aufbau ein neues SettingsTab-Objekt entsteht. */
    private static String openCategory = null;
    private static boolean clearNotifConfirm = false; // Zwei-Tipp-Bestaetigung fuer Posteingang leeren

    private static final String CAT_POSITION = "position";
    private static final String CAT_HEADER   = "header";
    private static final String CAT_INBOX    = "inbox";
    private static final String CAT_TABS     = "tabs";
    private static final String CAT_SERVICE  = "service";
    private static final String CAT_PERMS    = "perms";
    private static final String CAT_ABOUT    = "about";

    /** Ob das Änderungsprotokoll gerade aufgeklappt ist - statisch wie
     *  openCategory, ueberlebt den Panel-Neuaufbau. */
    private static boolean changelogOpen = false;

    /** Scroll-Position ueber einen Panel-Neuaufbau hinweg merken (sonst
     *  springt die Ansicht bei jeder Aenderung - z.B. Reihenfolge per ▲▼ -
     *  nach ganz oben). Statisch, weil bei jedem Aufbau ein neues
     *  SettingsTab-Objekt entsteht. */
    private static int savedScrollY = -1;

    public View buildContent(Context ctx, Runnable closePanel, Runnable refresh) {
        this.ctx = ctx;
        this.closePanel = closePanel;
        this.refresh = refresh;
        float fs = Settings.fontScale(ctx);
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);

        ScrollView scroll = new ScrollView(ctx);
        // Eigener, UNDURCHSICHTIGER Hintergrund fuer die ganze Einstellungen-
        // Ansicht - haengt sonst wie jede andere Karte in der (je nach
        // Transparenz-Einstellung stark durchsichtigen) Leiste und ist bei den
        // vielen kleinen Zeilen/Knoepfchen hier kaum lesbar (Mathias' Screenshot
        // der Posteingang-Quellen: Wallpaper scheint durch, "schlecht lesbar").
        // Anders als die Karten-Inhalte selbst braucht eine Einstellungsseite
        // volle Lesbarkeit, keine Durchsicht-Optik.
        GradientDrawable settingsBg = new GradientDrawable();
        settingsBg.setColor(Color.parseColor("#FF1C1C1E"));
        scroll.setBackground(settingsBg);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(4 * d, 0, 4 * d, 0);
        scroll.addView(root);

        // Aenderungen an den Registerkarten bauen das Panel komplett neu auf
        // (refresh); dabei zuerst die aktuelle Scroll-Position merken.
        final Runnable tabRefresh = () -> {
            savedScrollY = scroll.getScrollY();
            if (refresh != null) refresh.run();
        };

        if (openCategory == null) {
            buildCategoryMenu(root, fs, d);
        } else if (CAT_POSITION.equals(openCategory)) {
            backHeader(root, ctx.getString(R.string.settings_cat_position_title), fs, d);
            buildPositionSection(root, fs, d);
        } else if (CAT_HEADER.equals(openCategory)) {
            backHeader(root, ctx.getString(R.string.settings_cat_header_title), fs, d);
            buildHeaderSection(root, fs, d);
        } else if (CAT_INBOX.equals(openCategory)) {
            backHeader(root, ctx.getString(R.string.tab_inbox), fs, d);
            buildInboxSection(root, fs, d, tabRefresh);
        } else if (CAT_TABS.equals(openCategory)) {
            backHeader(root, ctx.getString(R.string.settings_cat_tabs_title), fs, d);
            buildTabsSection(root, fs, d, tabRefresh);
        } else if (CAT_SERVICE.equals(openCategory)) {
            backHeader(root, ctx.getString(R.string.settings_cat_service_title), fs, d);
            buildServiceSection(root, fs, d);
        } else if (CAT_PERMS.equals(openCategory)) {
            backHeader(root, "Berechtigungen", fs, d);
            buildPermsSection(root, fs, d);
        } else if (CAT_ABOUT.equals(openCategory)) {
            backHeader(root, ctx.getString(R.string.settings_cat_about_title), fs, d);
            buildAboutSection(root, fs, d);
        } else {
            openCategory = null;
            buildCategoryMenu(root, fs, d);
        }

        if (savedScrollY >= 0) {
            final int y = savedScrollY;
            savedScrollY = -1;
            scroll.post(() -> scroll.scrollTo(0, y));
        }
        return scroll;
    }

    // ---------- Kategorien-Menue ----------

    private void buildCategoryMenu(LinearLayout root, float fs, int d) {
        root.addView(categoryRow(CAT_POSITION, ctx.getString(R.string.settings_cat_position_title),
                ctx.getString(R.string.settings_cat_position_subtitle), fs, d));
        root.addView(categoryRow(CAT_HEADER, ctx.getString(R.string.settings_cat_header_title),
                ctx.getString(R.string.settings_cat_header_subtitle), fs, d));
        root.addView(categoryRow(CAT_INBOX, ctx.getString(R.string.tab_inbox),
                ctx.getString(R.string.settings_cat_inbox_subtitle), fs, d));
        root.addView(categoryRow(CAT_TABS, ctx.getString(R.string.settings_cat_tabs_title),
                ctx.getString(R.string.settings_cat_tabs_subtitle), fs, d));
        root.addView(categoryRow(CAT_SERVICE, ctx.getString(R.string.settings_cat_service_title),
                ctx.getString(R.string.settings_cat_service_subtitle), fs, d));
        root.addView(categoryRow(CAT_PERMS, "Berechtigungen",
                "Status je Berechtigung, wofür sie gebraucht wird, direkt zur Systemeinstellung", fs, d));
        root.addView(categoryRow(CAT_ABOUT, ctx.getString(R.string.settings_cat_about_title),
                ctx.getString(R.string.settings_cat_about_subtitle), fs, d));
    }

    /** Eigene Karte "Berechtigungen": alle Berechtigungen mit Status + wofuer,
     *  ein Tipp fuehrt je Berechtigung in die passende Systemeinstellung.
     *  Dieselbe Definition wie die Erinnerung (Perms.list) - keine doppelte
     *  Pflege, keine doppelten Einstellungen. */
    private void buildPermsSection(LinearLayout root, float fs, int d) {
        TextView intro = new TextView(ctx);
        intro.setText("Welche Berechtigung wofür gebraucht wird – und ihr aktueller Stand. "
                + "Fehlt eine einmal erteilte, erinnert EdgeTab von selbst.");
        intro.setTextColor(Color.parseColor("#99AAB8"));
        intro.setTextSize(12.5f * fs);
        intro.setPadding(d * 4, 0, d * 4, d * 8);
        root.addView(intro);

        for (de.herbers.common.PermReminder.Perm perm : Perms.list(ctx)) {
            TextView name = new TextView(ctx);
            name.setText((perm.granted ? "✓  " : "✗  ") + perm.label + (perm.granted ? "" : "  –  fehlt"));
            name.setTextColor(perm.granted ? Color.parseColor("#5BD68A") : Color.parseColor("#E0533A"));
            name.setTextSize(14 * fs);
            name.setPadding(d * 8, d * 8, d * 8, d * 2);
            root.addView(name);
            if (perm.explanation != null && !perm.explanation.isEmpty()) {
                TextView why = new TextView(ctx);
                why.setText(perm.explanation);
                why.setTextColor(Color.parseColor("#99AAB8"));
                why.setTextSize(12 * fs);
                why.setPadding(d * 8, 0, d * 8, d * 4);
                root.addView(why);
            }
            Button go = new Button(ctx);
            go.setText(perm.granted ? "In den Einstellungen ändern" : "Jetzt erteilen");
            go.setTextSize(13 * fs);
            go.setOnClickListener(v -> {
                try {
                    Intent i = perm.settings;
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                } catch (Throwable ignored) {}
            });
            root.addView(go);

            // Zugehoerige, gespeicherte Daten gezielt loeschen: der Posteingang
            // (erfasste Benachrichtigungen). Zwei-Tipp-Bestaetigung, weil im
            // Overlay kein normaler Dialog moeglich ist.
            if ("notif".equals(perm.key)) {
                Button del = new Button(ctx);
                del.setText(clearNotifConfirm ? "Wirklich? Nochmal tippen zum Löschen"
                                              : "Erfasste Benachrichtigungen löschen");
                del.setTextColor(Color.parseColor(clearNotifConfirm ? "#FF7043" : "#E06666"));
                del.setTextSize(13 * fs);
                del.setOnClickListener(v -> {
                    if (clearNotifConfirm) {
                        try { NotificationStore.get(ctx).clearAll(); } catch (Throwable ignored) {}
                        clearNotifConfirm = false;
                        android.widget.Toast.makeText(ctx, "Posteingang geleert.", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        clearNotifConfirm = true;
                    }
                    if (refresh != null) refresh.run();
                });
                root.addView(del);
            }
        }
    }

    private View categoryRow(String key, String title, String subtitle, float fs, int d) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#22FFFFFF"));
        bg.setCornerRadius(8 * d);
        row.setBackground(bg);
        row.setPadding(14 * d, 12 * d, 14 * d, 12 * d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 8 * d;
        row.setLayoutParams(lp);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(15 * fs);
        textCol.addView(t);
        TextView sub = new TextView(ctx);
        sub.setText(subtitle);
        sub.setTextColor(Color.parseColor("#9E9E9E"));
        sub.setTextSize(12 * fs);
        textCol.addView(sub);
        row.addView(textCol);

        TextView chevron = new TextView(ctx);
        chevron.setText("›");
        chevron.setTextColor(Color.parseColor("#2E9BE6"));
        chevron.setTextSize(22);
        row.addView(chevron);

        row.setClickable(true);
        row.setOnClickListener(new OpenCategoryClick(key, refresh));
        return row;
    }

    private void backHeader(LinearLayout root, String title, float fs, int d) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, 10 * d);

        TextView back = new TextView(ctx);
        back.setText(R.string.back_button);
        back.setTextColor(Color.parseColor("#2E9BE6"));
        back.setTextSize(15 * fs);
        back.setPadding(0, 6 * d, 16 * d, 6 * d);
        back.setOnClickListener(v -> { openCategory = null; if (refresh != null) refresh.run(); });
        row.addView(back);

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(17 * fs);
        row.addView(t);
        root.addView(row);
    }

    /** Oeffnet eine Kategorie (statisch/benannt wegen d8). */
    private static final class OpenCategoryClick implements View.OnClickListener {
        private final String key; private final Runnable refresh;
        OpenCategoryClick(String key, Runnable refresh) { this.key = key; this.refresh = refresh; }
        public void onClick(View v) { openCategory = key; if (refresh != null) refresh.run(); }
    }

    // ---------- Position & Aussehen ----------

    private void buildPositionSection(LinearLayout root, float fs, int d) {
        LinearLayout sideRow = new LinearLayout(ctx);
        Button left = new Button(ctx); left.setText(R.string.side_left);
        Button rightBtn = new Button(ctx); rightBtn.setText(R.string.side_right);
        left.setOnClickListener(v -> { Settings.setEdgeRight(ctx, false); reload(); });
        rightBtn.setOnClickListener(v -> { Settings.setEdgeRight(ctx, true); reload(); });
        sideRow.addView(left); sideRow.addView(rightBtn);
        root.addView(sideRow);

        slider(root, ctx.getString(R.string.slider_handle_pos), -100, 100, Settings.handlePos(ctx), fs, false,
                v -> { Settings.setHandlePos(ctx, v); softRefresh(); });
        slider(root, ctx.getString(R.string.slider_handle_length), 80, 320, Settings.handleHeight(ctx), fs, false,
                v -> { Settings.setHandleHeight(ctx, v); softRefresh(); });
        slider(root, ctx.getString(R.string.slider_panel_width), 220, 420, Settings.panelWidth(ctx), fs, true,
                v -> Settings.setPanelWidth(ctx, v));
        slider(root, ctx.getString(R.string.slider_transparency), 0, 90, Settings.transparency(ctx), fs, true,
                v -> Settings.setTransparency(ctx, v));
        slider(root, ctx.getString(R.string.slider_font_scale), 80, 150, Settings.fontScalePercent(ctx), fs, true,
                v -> Settings.setFontScale(ctx, v));
        slider(root, ctx.getString(R.string.slider_icon_size), 18, 44, Settings.iconSize(ctx), fs, true,
                v -> Settings.setIconSize(ctx, v));

        section(root, ctx.getString(R.string.media_controls_section), fs);
        TextView mediaHint = new TextView(ctx);
        mediaHint.setText(R.string.media_controls_hint);
        mediaHint.setTextColor(Color.GRAY);
        mediaHint.setTextSize(12 * fs);
        mediaHint.setPadding(0, 0, 0, 6 * d);
        root.addView(mediaHint);
        LinearLayout mediaPosRow = new LinearLayout(ctx);
        mediaPosRow.setOrientation(LinearLayout.HORIZONTAL);
        Button posTop = new Button(ctx); posTop.setText(R.string.pos_top);
        Button posMid = new Button(ctx); posMid.setText(R.string.pos_middle);
        Button posBottom = new Button(ctx); posBottom.setText(R.string.pos_bottom);
        posTop.setOnClickListener(v -> { Settings.setMediaControlsPos(ctx, "top"); reload(); });
        posMid.setOnClickListener(v -> { Settings.setMediaControlsPos(ctx, "middle"); reload(); });
        posBottom.setOnClickListener(v -> { Settings.setMediaControlsPos(ctx, "bottom"); reload(); });
        mediaPosRow.addView(posTop); mediaPosRow.addView(posMid); mediaPosRow.addView(posBottom);
        root.addView(mediaPosRow);
    }

    // ---------- Kopfzeile ----------

    private void buildHeaderSection(LinearLayout root, float fs, int d) {
        CheckBox hdr = new CheckBox(ctx);
        hdr.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        hdr.setText("  " + ctx.getString(R.string.show_clock_checkbox));
        hdr.setTextColor(Color.WHITE);
        hdr.setTextSize(15 * fs);
        hdr.setChecked(Settings.showHeader(ctx));
        hdr.setOnCheckedChangeListener((v, on) -> { Settings.setShowHeader(ctx, on); });
        root.addView(hdr);
        CheckBox batt = new CheckBox(ctx);
        batt.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        batt.setText("  " + ctx.getString(R.string.show_battery_checkbox));
        batt.setTextColor(Color.WHITE);
        batt.setTextSize(15 * fs);
        batt.setChecked(Settings.showBattery(ctx));
        batt.setOnCheckedChangeListener((v, on) -> { Settings.setShowBattery(ctx, on); });
        root.addView(batt);
    }

    // ---------- Posteingang ----------

    /** Welche App(s) gerade ihr Kategorie-Raster aufgeklappt haben - wie
     *  PICKING/REPLYING in anderen Karten ueberlebt das den Panel-Neuaufbau. */
    private static final java.util.Set<String> CATEGORY_PICKING = new java.util.HashSet<>();

    /** Alle Apps, die grundsaetzlich als Posteingang-Quelle in Frage kommen -
     *  nicht mehr nur die, die schon mal tatsaechlich benachrichtigt haben
     *  (das zwang zum Abwarten, teils tagelang, bis ein seltener Kanal einmal
     *  etwas schickt, siehe Mathias). Vereinigung aus "hat ein Startsymbol"
     *  (wie ShortcutsTabs App-Auswahl - deckt praktisch jede normale App ab)
     *  und bereits tatsaechlich beobachteten Absendern (deckt die wenigen
     *  Faelle ohne eigenes Startsymbol ab, z.B. manche Download-Dienste). */
    private static java.util.List<String> allNotifyCapableApps(Context ctx) {
        java.util.LinkedHashSet<String> pkgs = new java.util.LinkedHashSet<>();
        // Startbare Apps (ohne die eigene) aus der gemeinsamen Bibliothek ...
        for (String[] row : de.herbers.common.Apps.launchable(ctx)) pkgs.add(row[0]);
        // ... plus bereits beobachtete Absender (auch ohne Startsymbol).
        pkgs.addAll(NotificationStore.get(ctx).distinctPackages());
        return new java.util.ArrayList<>(pkgs);
    }

    private void buildInboxSection(LinearLayout root, float fs, int d, Runnable sectionRefresh) {
        section(root, "Posteingang-Inhalt", fs);
        CheckBox smsCb = new CheckBox(ctx);
        smsCb.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        smsCb.setText("  SMS im Posteingang anzeigen");
        smsCb.setTextColor(Color.WHITE); smsCb.setTextSize(13 * fs);
        smsCb.setChecked(Settings.smsInInbox(ctx));
        smsCb.setOnCheckedChangeListener((v, on) -> { Settings.setSmsInInbox(ctx, on); sectionRefresh.run(); });
        root.addView(smsCb);
        CheckBox callsCb = new CheckBox(ctx);
        callsCb.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        callsCb.setText("  Anrufe im Posteingang anzeigen");
        callsCb.setTextColor(Color.WHITE); callsCb.setTextSize(13 * fs);
        callsCb.setChecked(Settings.callsInInbox(ctx));
        callsCb.setOnCheckedChangeListener((v, on) -> { Settings.setCallsInInbox(ctx, on); sectionRefresh.run(); });
        root.addView(callsCb);
        CheckBox convCb = new CheckBox(ctx);
        convCb.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        convCb.setText("  Nachrichten zu Konversationen gruppieren (aufklappbar)");
        convCb.setTextColor(Color.WHITE); convCb.setTextSize(13 * fs);
        convCb.setChecked(Settings.groupConversations(ctx));
        convCb.setOnCheckedChangeListener((v, on) -> { Settings.setGroupConversations(ctx, on); sectionRefresh.run(); });
        root.addView(convCb);
        TextView inboxHint = new TextView(ctx);
        inboxHint.setText("SMS/Anrufe gibt es weiterhin auch als eigene Karten. Antworten auf "
                + "SMS und „verpasst als gesehen“ bei Anrufen direkt im Posteingang.");
        inboxHint.setTextColor(Color.GRAY); inboxHint.setTextSize(12 * fs);
        inboxHint.setPadding(0, 0, 0, 8 * d);
        root.addView(inboxHint);

        // ---- Zeitanzeige + Listen-Alter ----
        section(root, "Zeitanzeige & Umfang", fs);
        CheckBox relCb = new CheckBox(ctx);
        relCb.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        relCb.setText("  Relative Zeit anzeigen (z. B. „vor 3 Min“)");
        relCb.setTextColor(Color.WHITE); relCb.setTextSize(13 * fs);
        relCb.setChecked(Settings.relTimeOn(ctx));
        relCb.setOnCheckedChangeListener((v, on) -> { Settings.setRelTimeOn(ctx, on); sectionRefresh.run(); });
        root.addView(relCb);
        dayInput(root, "Relativ anzeigen für Einträge der letzten … Tage", Settings.relTimeDays(ctx), fs, d,
                days -> Settings.setRelTimeDays(ctx, days));
        TextView relHint = new TextView(ctx);
        relHint.setText("Ältere (und bei ausgeschalteter Option alle) Einträge zeigen Datum + Uhrzeit, "
                + "z. B. „09:15 Uhr, 28.09.2026“. In Posteingang, SMs und Anrufen. 1–999 Tage.");
        relHint.setTextColor(Color.GRAY); relHint.setTextSize(12 * fs);
        relHint.setPadding(0, 0, 0, 8 * d);
        root.addView(relHint);
        dayInput(root, "Einträge zeigen der letzten … Tage (Posteingang, SMS, Anrufe)", Settings.listDays(ctx), fs, d,
                days -> Settings.setListDays(ctx, days));
        TextView listHint = new TextView(ctx);
        listHint.setText("Keine feste Anzahl-Grenze mehr – die Liste reicht so weit zurück, wie hier "
                + "eingestellt (1–999 Tage). Bei Benachrichtigungen zusätzlich durch die Aufbewahrung unten begrenzt.");
        listHint.setTextColor(Color.GRAY); listHint.setTextSize(12 * fs);
        listHint.setPadding(0, 0, 0, 8 * d);
        root.addView(listHint);

        section(root, ctx.getString(R.string.inbox_sources_section), fs);
        TextView srcHint = new TextView(ctx);
        java.util.List<String> pkgs = allNotifyCapableApps(ctx);
        srcHint.setText(R.string.inbox_sources_hint);
        srcHint.setTextColor(Color.GRAY);
        srcHint.setTextSize(12 * fs);
        srcHint.setPadding(0, 0, 0, 6 * d);
        root.addView(srcHint);

        android.content.pm.PackageManager pm = ctx.getPackageManager();
        NotificationStore store = NotificationStore.get(ctx);

        // Alphabetisch statt nach Nachrichten-Anzahl (wie der Provider sie
        // liefert) - ueber TreeMap statt Comparator, wie in ShortcutsTabs
        // App-Auswahl (d8-Fallstrick bei Lambda-Comparator dort umgangen).
        java.util.TreeMap<String, String> sortedPkgs = new java.util.TreeMap<>();
        java.util.Map<String, String> labels = new java.util.HashMap<>();
        int idx = 0;
        for (String pkg : pkgs) {
            String label;
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (Exception e) { label = pkg; }
            labels.put(pkg, label);
            sortedPkgs.put(label.toLowerCase() + "" + (idx++), pkg);
        }

        for (String pkg : sortedPkgs.values()) {
            String label = labels.get(pkg);
            final String fp = pkg;

            LinearLayout appRow = new LinearLayout(ctx);
            appRow.setOrientation(LinearLayout.HORIZONTAL);
            appRow.setGravity(Gravity.CENTER_VERTICAL);

            CheckBox cb = new CheckBox(ctx);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
            cb.setText("  " + label);
            cb.setTextColor(Color.WHITE);
            cb.setTextSize(14 * fs);
            cb.setChecked(Settings.isSourceEnabled(ctx, pkg));
            // Einzeilig + Auslassungspunkte - sonst kann ein sehr langer,
            // bindestrichloser Name (z.B. lange Systempaket-Namen) die
            // Gewichtsaufteilung sprengen und die Kategorie-Schaltflaeche
            // aus der Zeile draengen (Mathias' Beobachtung).
            cb.setSingleLine(true);
            cb.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cb.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            cb.setOnCheckedChangeListener((v, on) -> { Settings.setSourceEnabled(ctx, fp, on); });
            appRow.addView(cb);

            String cat = Settings.category(ctx, pkg);
            TextView catBtn = new TextView(ctx);
            catBtn.setText(cat != null ? Settings.categoryLabel(ctx, cat) : ctx.getString(R.string.category_pick_placeholder));
            catBtn.setTextSize(12 * fs);
            catBtn.setPadding(10 * d, 5 * d, 10 * d, 5 * d);
            catBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            GradientDrawable catBg = new GradientDrawable();
            catBg.setCornerRadius(12 * d);
            catBg.setColor(cat != null ? Settings.categoryColor(cat) : Color.parseColor("#3A3A3C"));
            catBtn.setBackground(catBg);
            catBtn.setTextColor(Color.WHITE);
            catBtn.setOnClickListener(v -> {
                if (CATEGORY_PICKING.contains(fp)) CATEGORY_PICKING.remove(fp);
                else CATEGORY_PICKING.add(fp);
                sectionRefresh.run();
            });
            appRow.addView(catBtn);
            root.addView(appRow);

            if (CATEGORY_PICKING.contains(pkg)) {
                root.addView(categoryGrid(fp, fs, d, sectionRefresh));
            }

            java.util.List<NotificationStore.Channel> chans = store.distinctChannels(pkg);
            if (chans.size() > 1) {
                for (NotificationStore.Channel ch : chans) {
                    CheckBox ccb = new CheckBox(ctx);
                    ccb.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
                    ccb.setText("     " + ch.name);
                    ccb.setTextColor(Color.parseColor("#B0B0B5"));
                    ccb.setTextSize(12 * fs);
                    ccb.setChecked(Settings.isChannelEnabled(ctx, pkg, ch.id));
                    final String fch = ch.id;
                    ccb.setOnCheckedChangeListener((v, on) -> Settings.setChannelEnabled(ctx, fp, fch, on));
                    root.addView(ccb);
                }
            }
        }

        section(root, ctx.getString(R.string.inbox_cleanup_section), fs);
        TextView keepHint = new TextView(ctx);
        keepHint.setText(R.string.inbox_cleanup_hint);
        keepHint.setTextColor(Color.GRAY);
        keepHint.setTextSize(12 * fs);
        root.addView(keepHint);
        Button keepBtn = new Button(ctx);
        keepBtn.setText(ctx.getString(R.string.retention_button, Settings.retentionLabel(ctx, Settings.retentionDays(ctx))));
        keepBtn.setOnClickListener(new RetentionClick(ctx, keepBtn));
        root.addView(keepBtn);
    }

    /** Raster mit den festen Kategorien plus "Keine" - wie BlackBerry Hub+
     *  Services' "Kategorie fuer App auswaehlen"-Dialog, hier eingeklappt
     *  direkt unter der Zeile statt als eigenes Dialogfenster (das Panel ist
     *  ein Overlay-Fenster ohne Activity, echte AlertDialogs passen dort
     *  nicht ins bisherige Bild). */
    private View categoryGrid(String pkg, float fs, int d, Runnable sectionRefresh) {
        android.widget.GridLayout grid = new android.widget.GridLayout(ctx);
        grid.setColumnCount(3);
        grid.setPadding(20 * d, 4 * d, 0, 10 * d);

        grid.addView(catCell(pkg, null, ctx.getString(R.string.category_none), "#3A3A3C", fs, d, sectionRefresh));
        for (String cat : Settings.CATEGORIES) {
            grid.addView(catCell(pkg, cat, Settings.categoryLabel(ctx, cat), null, fs, d, sectionRefresh));
        }
        return grid;
    }

    private View catCell(String pkg, String cat, String label, String colorHexOrNull,
                          float fs, int d, Runnable sectionRefresh) {
        TextView cell = new TextView(ctx);
        cell.setText(label);
        cell.setTextColor(Color.WHITE);
        cell.setTextSize(12 * fs);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(8 * d, 10 * d, 8 * d, 10 * d);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(10 * d);
        bg.setColor(colorHexOrNull != null ? Color.parseColor(colorHexOrNull) : Settings.categoryColor(cat));
        cell.setBackground(bg);
        android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
        lp.setMargins(4 * d, 4 * d, 4 * d, 4 * d);
        cell.setLayoutParams(lp);
        cell.setOnClickListener(v -> {
            Settings.setCategory(ctx, pkg, cat);
            CATEGORY_PICKING.remove(pkg);
            sectionRefresh.run();
        });
        return cell;
    }

    // ---------- Registerkarten ----------

    private void buildTabsSection(LinearLayout root, float fs, int d, Runnable tabRefresh) {
        TextView tabsHint = new TextView(ctx);
        tabsHint.setText(R.string.tabs_section_hint);
        tabsHint.setTextColor(Color.GRAY);
        tabsHint.setTextSize(12 * fs);
        tabsHint.setPadding(0, 0, 0, 8 * d);
        root.addView(tabsHint);

        List<TabInstance> tabList = Tabs.load(ctx);
        for (int i = 0; i < tabList.size(); i++) {
            root.addView(tabRow(tabList.get(i), i, tabList.size(), fs, d, tabRefresh));
        }

        Button addWidgetTab = new Button(ctx);
        addWidgetTab.setText(R.string.add_widget_tab);
        addWidgetTab.setOnClickListener(v -> { Tabs.addWidgetTab(ctx); tabRefresh.run(); });
        root.addView(addWidgetTab);

        Button addShortcutsTab = new Button(ctx);
        addShortcutsTab.setText(R.string.add_shortcuts_tab);
        addShortcutsTab.setOnClickListener(v -> { Tabs.addShortcutsTab(ctx); tabRefresh.run(); });
        root.addView(addShortcutsTab);

        Button addMediaTab = new Button(ctx);
        addMediaTab.setText(R.string.add_media_tab);
        addMediaTab.setOnClickListener(v -> { Tabs.addMediaTab(ctx); tabRefresh.run(); });
        root.addView(addMediaTab);

        Button addFramesTab = new Button(ctx);
        addFramesTab.setText(R.string.add_frames_tab);
        addFramesTab.setOnClickListener(v -> { Tabs.addFramesTab(ctx); tabRefresh.run(); });
        root.addView(addFramesTab);

        Button addSmsTab = new Button(ctx);
        addSmsTab.setText(R.string.add_sms_tab);
        addSmsTab.setOnClickListener(v -> { Tabs.addSmsTab(ctx); tabRefresh.run(); });
        root.addView(addSmsTab);

        Button addCallsTab = new Button(ctx);
        addCallsTab.setText(R.string.add_calls_tab);
        addCallsTab.setOnClickListener(v -> { Tabs.addCallsTab(ctx); tabRefresh.run(); });
        root.addView(addCallsTab);

        TextView framesHint = new TextView(ctx);
        framesHint.setText(R.string.add_frames_hint);
        framesHint.setTextColor(Color.GRAY);
        framesHint.setTextSize(11 * fs);
        framesHint.setPadding(0, 0, 0, 8 * d);
        root.addView(framesHint);

        section(root, ctx.getString(R.string.tasks_notes_section), fs);
        String jtxAccount = Settings.jtxAccountName(ctx);
        TextView jtxHint = new TextView(ctx);
        jtxHint.setText(jtxAccount != null
                ? ctx.getString(R.string.jtx_account_used, jtxAccount)
                : ctx.getString(R.string.jtx_account_none));
        jtxHint.setTextColor(Color.GRAY);
        jtxHint.setTextSize(12 * fs);
        jtxHint.setPadding(0, 0, 0, 6 * d);
        root.addView(jtxHint);
        Button jtxBtn = new Button(ctx);
        jtxBtn.setText(jtxAccount != null ? R.string.jtx_account_change : R.string.jtx_account_setup);
        jtxBtn.setOnClickListener(v -> {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("pick_jtx_account", true);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            if (closePanel != null) closePanel.run();
        });
        root.addView(jtxBtn);
    }

    // ---------- Dienst ----------

    private void buildServiceSection(LinearLayout root, float fs, int d) {
        Button stop = new Button(ctx);
        stop.setText(R.string.stop_service_button);
        stop.setOnClickListener(v ->
                ctx.stopService(new Intent(ctx, EdgeService.class)));
        root.addView(stop);

        section(root, ctx.getString(R.string.section_backup), fs);
        TextView backupDesc = new TextView(ctx);
        backupDesc.setText(R.string.backup_description);
        backupDesc.setTextColor(Color.parseColor("#9E9E9E"));
        backupDesc.setTextSize(12 * fs);
        backupDesc.setPadding(0, 0, 0, 8 * d);
        root.addView(backupDesc);
        LinearLayout backupRow = new LinearLayout(ctx);
        backupRow.setOrientation(LinearLayout.HORIZONTAL);
        Button exportBtn = new Button(ctx);
        exportBtn.setText(R.string.button_backup_export);
        exportBtn.setOnClickListener(new BackupClick(ctx, closePanel, true));
        backupRow.addView(exportBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button importBtn = new Button(ctx);
        importBtn.setText(R.string.button_backup_import);
        importBtn.setOnClickListener(new BackupClick(ctx, closePanel, false));
        backupRow.addView(importBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(backupRow);

        TextView ver = new TextView(ctx);
        String vn;
        try { vn = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName; }
        catch (Exception e) { vn = "?"; }
        ver.setText("\n" + ctx.getString(R.string.version_line, vn));
        ver.setTextColor(Color.GRAY);
        ver.setTextSize(12 * fs);
        root.addView(ver);
    }

    // ---------- Über EdgeTab (Version, Lizenz, Änderungsprotokoll) ----------

    private void buildAboutSection(LinearLayout root, float fs, int d) {
        String vn;
        try { vn = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName; }
        catch (Exception e) { vn = "?"; }
        TextView ver = new TextView(ctx);
        ver.setText(ctx.getString(R.string.version_line, vn));
        ver.setTextColor(Color.parseColor("#2E9BE6"));
        ver.setTextSize(16 * fs);
        ver.setPadding(0, 0, 0, 10 * d);
        root.addView(ver);

        TextView desc = new TextView(ctx);
        desc.setText(R.string.about_description);
        desc.setTextColor(Color.parseColor("#CCCCCC"));
        desc.setTextSize(13 * fs);
        desc.setPadding(0, 0, 0, 4 * d);
        root.addView(desc);

        section(root, ctx.getString(R.string.about_license_title), fs);
        TextView lic = new TextView(ctx);
        lic.setText(R.string.about_license_text);
        lic.setTextColor(Color.parseColor("#9E9E9E"));
        lic.setTextSize(12 * fs);
        root.addView(lic);

        section(root, ctx.getString(R.string.about_changelog_title), fs);
        Button toggle = new Button(ctx);
        toggle.setText(changelogOpen ? R.string.about_changelog_hide : R.string.about_changelog_show);
        toggle.setOnClickListener(v -> { changelogOpen = !changelogOpen; refresh.run(); });
        root.addView(toggle);

        if (changelogOpen) {
            String md = readAsset("CHANGELOG.md");
            LinearLayout box = new LinearLayout(ctx);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(0, 8 * d, 0, 0);
            if (md == null) {
                TextView err = new TextView(ctx);
                err.setText(R.string.about_changelog_unavailable);
                err.setTextColor(Color.parseColor("#FFB0B0"));
                err.setTextSize(12 * fs);
                box.addView(err);
            } else {
                renderMarkdown(box, md, fs, d);
            }
            root.addView(box);
        }

        // Fehlerprotokoll - bewusst ganz unten, neueste Eintraege zuerst,
        // Schalter/Knopf darueber, ein-/ausschaltbar (in allen Apps gleich).
        section(root, ctx.getString(R.string.about_crashlog_title), fs);
        TextView intro = new TextView(ctx);
        intro.setText(R.string.about_crashlog_intro);
        intro.setTextColor(Color.parseColor("#9E9E9E"));
        intro.setTextSize(12 * fs);
        intro.setPadding(0, 0, 0, 6 * d);
        root.addView(intro);

        CheckBox showLog = new CheckBox(ctx);
        showLog.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        showLog.setText("  Protokoll anzeigen");
        showLog.setTextColor(Color.WHITE);
        showLog.setTextSize(13 * fs);
        showLog.setChecked(de.herbers.common.DiagLog.isDisplayEnabled(ctx));
        showLog.setOnCheckedChangeListener((v, on) -> {
            de.herbers.common.DiagLog.setDisplayEnabled(ctx, on); refresh.run(); });
        root.addView(showLog);

        if (de.herbers.common.DiagLog.isDisplayEnabled(ctx)) {
            Button clear = new Button(ctx);
            clear.setText(R.string.about_crashlog_clear);
            clear.setOnClickListener(v -> { de.herbers.common.DiagLog.clear(ctx); refresh.run(); });
            root.addView(clear);

            String crash = de.herbers.common.DiagLog.readNewestFirst(ctx);
            TextView log = new TextView(ctx);
            log.setText((crash == null || crash.isEmpty()) ? "(noch leer)" : crash);
            log.setTextColor((crash == null || crash.isEmpty()) ? Color.GRAY : Color.parseColor("#CCCCCC"));
            log.setTextSize(11 * fs);
            log.setTypeface(android.graphics.Typeface.MONOSPACE);
            log.setTextIsSelectable(true);
            log.setPadding(0, 6 * d, 0, 6 * d);
            root.addView(log);
        }
    }

    private String readAsset(String name) {
        try (java.io.InputStream in = ctx.getAssets().open(name)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) { return null; }
    }

    /** Sehr schlichter Markdown-Renderer - nur genug, um das eigene, immer
     *  gleich aufgebaute CHANGELOG.md lesbar darzustellen (Überschriften,
     *  Aufzählungspunkte, Absätze). Kein allgemeiner Markdown-Parser. */
    private void renderMarkdown(LinearLayout root, String md, float fs, int d) {
        for (String line : md.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            TextView tv = new TextView(ctx);
            if (t.startsWith("## ")) {
                tv.setText(t.substring(3));
                tv.setTextColor(Color.parseColor("#2E9BE6"));
                tv.setTextSize(14 * fs);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setPadding(0, 14 * d, 0, 4 * d);
            } else if (t.startsWith("# ")) {
                tv.setText(t.substring(2));
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(16 * fs);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setPadding(0, 4 * d, 0, 6 * d);
            } else if (t.startsWith("- ")) {
                tv.setText("•  " + t.substring(2));
                tv.setTextColor(Color.parseColor("#CCCCCC"));
                tv.setTextSize(12 * fs);
                tv.setPadding(10 * d, 2 * d, 0, 2 * d);
            } else {
                tv.setText(t);
                tv.setTextColor(Color.parseColor("#9E9E9E"));
                tv.setTextSize(12 * fs);
                tv.setPadding(0, 2 * d, 0, 2 * d);
            }
            root.addView(tv);
        }
    }

    /** Griff mit neuen Werten neu bauen, ohne das Panel zu schliessen. */
    private void softRefresh() {
        ctx.startService(new Intent(ctx, EdgeService.class).putExtra("refresh", true));
    }

    /** Panel komplett neu aufbauen (z.B. nach Seitenwechsel). */
    private void reload() {
        softRefresh();
        if (refresh != null) refresh.run();
    }

    /** Eine Registerkarte als eigene "Karte" in den Einstellungen: An/Aus,
     *  Icon (tippen zum Aendern), Name (bearbeitbar), Tippen-schliesst-Leiste,
     *  Reihenfolge, und - nur bei zusaetzlich angelegten - Loeschen. */
    private View tabRow(TabInstance t, int index, int total, float fs, int d, Runnable tabRefresh) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#22FFFFFF"));
        bg.setCornerRadius(8 * d);
        card.setBackground(bg);
        card.setPadding(10 * d, 8 * d, 10 * d, 8 * d);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = 8 * d;
        card.setLayoutParams(cardLp);

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox cb = new CheckBox(ctx);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        cb.setTextColor(android.graphics.Color.WHITE);
        cb.setChecked(t.enabled);
        cb.setOnCheckedChangeListener((v, on) -> {
            Tabs.update(ctx, t.id, ti -> ti.enabled = on);
            tabRefresh.run();
        });
        head.addView(cb);

        Tab live = Tabs.build(ctx, t);
        ImageView icon = new ImageView(ctx);
        android.graphics.Bitmap bmp = t.iconPath != null ? decodeIcon(t.iconPath) : null;
        if (bmp != null) {
            icon.setImageBitmap(bmp);
        } else {
            icon.setImageResource(live.iconRes());
            icon.setColorFilter(Color.parseColor("#B0B0B0"));
        }
        int is = 22 * d;
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(is, is);
        ilp.leftMargin = 4 * d; ilp.rightMargin = 8 * d;
        icon.setLayoutParams(ilp);
        icon.setOnClickListener(new IconClick(ctx, closePanel, t.id));
        head.addView(icon);

        EditText name = new EditText(ctx);
        name.setHintTextColor(android.graphics.Color.parseColor("#9AA6B2"));
        name.setText(t.name);
        name.setHint(live.title(ctx));
        name.setTextColor(Color.WHITE);
        name.setTextSize(14 * fs);
        name.setSingleLine(true);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        name.addTextChangedListener(new RenameWatcher(ctx, t.id));
        head.addView(name);
        card.addView(head);

        LinearLayout row2 = new LinearLayout(ctx);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        row2.setPadding(0, 4 * d, 0, 0);

        CheckBox closeCb = new CheckBox(ctx);
        closeCb.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        closeCb.setText("  " + ctx.getString(R.string.close_on_tap_checkbox));
        closeCb.setTextColor(Color.parseColor("#B0B0B0"));
        closeCb.setTextSize(12 * fs);
        closeCb.setChecked(t.closeOnTap);
        closeCb.setOnCheckedChangeListener((v, on) -> Tabs.update(ctx, t.id, ti -> ti.closeOnTap = on));
        row2.addView(closeCb, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView up = arrow(index > 0 ? "▲" : "");
        if (index > 0) up.setOnClickListener(new MoveClick(ctx, t.id, -1, tabRefresh));
        row2.addView(up);
        TextView down = arrow(index < total - 1 ? "▼" : "");
        if (index < total - 1) down.setOnClickListener(new MoveClick(ctx, t.id, 1, tabRefresh));
        row2.addView(down);

        if (Tabs.isDeletable(t.id)) {
            TextView del = new TextView(ctx);
            del.setText("  " + ctx.getString(R.string.tab_delete_action));
            del.setTextColor(Color.parseColor("#E06666"));
            del.setTextSize(12 * fs);
            del.setPadding(10 * d, 0, 0, 0);
            del.setOnClickListener(v -> { Tabs.remove(ctx, t.id); tabRefresh.run(); });
            row2.addView(del);
        }
        card.addView(row2);
        return card;
    }

    private TextView arrow(String s) {
        TextView t = new TextView(ctx);
        t.setText("  " + s + "  ");
        t.setTextColor(Color.parseColor(s.isEmpty() ? "#444444" : "#2E9BE6"));
        t.setTextSize(14);
        return t;
    }

    private android.graphics.Bitmap decodeIcon(String path) {
        try { return android.graphics.BitmapFactory.decodeFile(path); }
        catch (Throwable t) { return null; }
    }

    /** Speichert den Namen bei jeder Aenderung (keine Panel-Neuzeichnung, sonst
     *  verliert das Eingabefeld den Fokus). Benannt statt anonym (d8). */
    private static final class RenameWatcher implements TextWatcher {
        private final Context ctx; private final String tabId;
        RenameWatcher(Context ctx, String tabId) { this.ctx = ctx; this.tabId = tabId; }
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) {}
        public void afterTextChanged(Editable e) {
            String v = e.toString();
            Tabs.update(ctx, tabId, t -> t.name = v.trim().isEmpty() ? null : v);
        }
    }

    /** Oeffnet die Bildauswahl fuer eine Registerkarte (ueber MainActivity,
     *  da eine echte Activity fuer den Datei-Picker noetig ist). */
    private static final class IconClick implements View.OnClickListener {
        private final Context ctx; private final Runnable closePanel; private final String tabId;
        IconClick(Context ctx, Runnable closePanel, String tabId) {
            this.ctx = ctx; this.closePanel = closePanel; this.tabId = tabId;
        }
        public void onClick(View v) {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("pick_icon", tabId);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            if (closePanel != null) closePanel.run();
        }
    }

    /** Startet Sichern (export=true) oder Wiederherstellen (export=false)
     *  ueber MainActivity, da eine echte Activity fuer den Datei-Picker
     *  noetig ist (siehe IconClick). Benannt statt anonym (d8). */
    private static final class BackupClick implements View.OnClickListener {
        private final Context ctx; private final Runnable closePanel; private final boolean export;
        BackupClick(Context ctx, Runnable closePanel, boolean export) {
            this.ctx = ctx; this.closePanel = closePanel; this.export = export;
        }
        public void onClick(View v) {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra(export ? "backup_export" : "backup_import", true);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            if (closePanel != null) closePanel.run();
        }
    }

    /** Verschiebt eine Registerkarte in der Reihenfolge. Benannt statt anonym (d8). */
    private static final class MoveClick implements View.OnClickListener {
        private final Context ctx; private final String id; private final int delta; private final Runnable after;
        MoveClick(Context ctx, String id, int delta, Runnable after) {
            this.ctx = ctx; this.id = id; this.delta = delta; this.after = after;
        }
        public void onClick(View v) {
            Tabs.move(ctx, id, delta);
            if (after != null) after.run();
        }
    }

    private void section(LinearLayout root, String s, float fs) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        TextView t = new TextView(ctx);
        t.setText(s.toUpperCase(java.util.Locale.getDefault()));
        t.setTextColor(Color.parseColor("#2E9BE6"));
        t.setTextSize(13 * fs);
        t.setPadding(0, 18 * d, 0, 6 * d);
        root.addView(t);
    }

    private interface IntSink { void set(int v); }

    /** Beschriftetes Zahlenfeld fuer eine Tages-Angabe (1..999). Schreibt beim
     *  Tippen direkt in die Einstellung (der Setter begrenzt selbst). */
    private void dayInput(LinearLayout root, String label, int value, float fs, int d, IntSink sink) {
        TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextColor(Color.parseColor("#DDDDDD"));
        t.setTextSize(13 * fs);
        t.setPadding(0, 10 * d, 0, 0);
        root.addView(t);
        EditText in = new EditText(ctx);
        in.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        in.setText(String.valueOf(value));
        in.setTextColor(Color.WHITE);
        in.setHintTextColor(Color.parseColor("#9AA6B2"));
        in.setTextSize(13 * fs);
        in.addTextChangedListener(new DayWatcher(sink));
        root.addView(in);
    }

    /** Uebernimmt eine getippte Tages-Zahl in die Einstellung (leer = ignorieren);
     *  Begrenzung 1..999 macht der jeweilige Setter. Benannt statt anonym (d8). */
    private static final class DayWatcher implements TextWatcher {
        private final IntSink sink;
        DayWatcher(IntSink sink) { this.sink = sink; }
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) {}
        public void afterTextChanged(Editable e) {
            String t = e.toString().trim();
            if (t.isEmpty()) return;
            try { sink.set(Integer.parseInt(t)); } catch (NumberFormatException ignored) {}
        }
    }

    private void slider(LinearLayout root, String label, int min, int max,
                        int value, float fs, boolean livePanel, IntSink sink) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextColor(Color.parseColor("#DDDDDD"));
        t.setTextSize(13 * fs);
        t.setPadding(0, 10 * d, 0, 0);
        root.addView(t);

        SeekBar sb = new SeekBar(ctx);
        sb.setMax(max - min);
        sb.setProgress(value - min);
        // Beim Loslassen: livePanel -> ganzes Panel neu zeichnen (Breite,
        // Transparenz, Schrift werden sofort sichtbar); sonst nur Griff neu.
        Runnable onStop = livePanel
                ? () -> { if (refresh != null) refresh.run(); }
                : this::softRefresh;
        sb.setOnSeekBarChangeListener(new SliderListener(min, sink, onStop));
        sb.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sb);
    }

    /** Benannt statt anonym - umgeht einen Fehler im mitgelieferten d8. */
    private static final class SliderListener implements SeekBar.OnSeekBarChangeListener {
        private final int min; private final IntSink sink; private final Runnable onStop;
        SliderListener(int min, IntSink sink, Runnable onStop) {
            this.min = min; this.sink = sink; this.onStop = onStop;
        }
        public void onProgressChanged(SeekBar s, int p, boolean u) { sink.set(min + p); }
        public void onStartTrackingTouch(SeekBar s) {}
        public void onStopTrackingTouch(SeekBar s) { if (onStop != null) onStop.run(); }
    }

    /** Schaltet die Aufbewahrungsdauer durch die Vorgaben. */
    private static final class RetentionClick implements View.OnClickListener {
        private final Context ctx; private final Button btn;
        RetentionClick(Context ctx, Button btn) { this.ctx = ctx; this.btn = btn; }
        public void onClick(View v) {
            int cur = Settings.retentionDays(ctx);
            int[] ch = Settings.RETENTION_CHOICES;
            int idx = 0;
            for (int i = 0; i < ch.length; i++) if (ch[i] == cur) { idx = i; break; }
            int next = ch[(idx + 1) % ch.length];
            Settings.setRetentionDays(ctx, next);
            btn.setText(ctx.getString(R.string.retention_button, Settings.retentionLabel(ctx, next)));
        }
    }
}
