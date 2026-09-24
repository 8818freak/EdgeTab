package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
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

    private static final String CAT_POSITION = "position";
    private static final String CAT_HEADER   = "header";
    private static final String CAT_INBOX    = "inbox";
    private static final String CAT_TABS     = "tabs";
    private static final String CAT_SERVICE  = "service";

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
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
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
            backHeader(root, "Position & Aussehen", fs, d);
            buildPositionSection(root, fs, d);
        } else if (CAT_HEADER.equals(openCategory)) {
            backHeader(root, "Kopfzeile", fs, d);
            buildHeaderSection(root, fs, d);
        } else if (CAT_INBOX.equals(openCategory)) {
            backHeader(root, "Posteingang", fs, d);
            buildInboxSection(root, fs, d);
        } else if (CAT_TABS.equals(openCategory)) {
            backHeader(root, "Registerkarten", fs, d);
            buildTabsSection(root, fs, d, tabRefresh);
        } else if (CAT_SERVICE.equals(openCategory)) {
            backHeader(root, "Dienst", fs, d);
            buildServiceSection(root, fs, d);
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
        root.addView(categoryRow(CAT_POSITION, "Position & Aussehen",
                "Seite, Griff, Breite, Transparenz, Schrift", fs, d));
        root.addView(categoryRow(CAT_HEADER, "Kopfzeile",
                "Uhr, Datum, Akku", fs, d));
        root.addView(categoryRow(CAT_INBOX, "Posteingang",
                "Quellen, Aufbewahrung", fs, d));
        root.addView(categoryRow(CAT_TABS, "Registerkarten",
                "Ein-/ausschalten, umbenennen, Icon, Reihenfolge", fs, d));
        root.addView(categoryRow(CAT_SERVICE, "Dienst",
                "Leiste beenden, Version", fs, d));
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
        back.setText("‹ Zurück");
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
        Button left = new Button(ctx); left.setText("◀ Links");
        Button rightBtn = new Button(ctx); rightBtn.setText("Rechts ▶");
        left.setOnClickListener(v -> { Settings.setEdgeRight(ctx, false); reload(); });
        rightBtn.setOnClickListener(v -> { Settings.setEdgeRight(ctx, true); reload(); });
        sideRow.addView(left); sideRow.addView(rightBtn);
        root.addView(sideRow);

        slider(root, "Position (Höhe des Griffs)", -100, 100, Settings.handlePos(ctx), fs, false,
                v -> { Settings.setHandlePos(ctx, v); softRefresh(); });
        slider(root, "Grifflänge", 80, 320, Settings.handleHeight(ctx), fs, false,
                v -> { Settings.setHandleHeight(ctx, v); softRefresh(); });
        slider(root, "Breite der Leiste", 220, 420, Settings.panelWidth(ctx), fs, true,
                v -> Settings.setPanelWidth(ctx, v));
        slider(root, "Transparenz", 0, 90, Settings.transparency(ctx), fs, true,
                v -> Settings.setTransparency(ctx, v));
        slider(root, "Schriftgröße (%)", 80, 150, Settings.fontScalePercent(ctx), fs, true,
                v -> Settings.setFontScale(ctx, v));
        slider(root, "Größe der Registerkarten-Icons", 18, 44, Settings.iconSize(ctx), fs, true,
                v -> Settings.setIconSize(ctx, v));

        section(root, "Mediensteuerung", fs);
        TextView mediaHint = new TextView(ctx);
        mediaHint.setText("Feste Position der Zurück/Play/Vor-Knöpfe in der Mediensteuerung-Karte "
                + "- bleibt beim Scrollen an derselben Stelle, für einhändige Bedienung.");
        mediaHint.setTextColor(Color.GRAY);
        mediaHint.setTextSize(12 * fs);
        mediaHint.setPadding(0, 0, 0, 6 * d);
        root.addView(mediaHint);
        LinearLayout mediaPosRow = new LinearLayout(ctx);
        mediaPosRow.setOrientation(LinearLayout.HORIZONTAL);
        Button posTop = new Button(ctx); posTop.setText("Oben");
        Button posMid = new Button(ctx); posMid.setText("Mitte");
        Button posBottom = new Button(ctx); posBottom.setText("Unten");
        posTop.setOnClickListener(v -> { Settings.setMediaControlsPos(ctx, "top"); reload(); });
        posMid.setOnClickListener(v -> { Settings.setMediaControlsPos(ctx, "middle"); reload(); });
        posBottom.setOnClickListener(v -> { Settings.setMediaControlsPos(ctx, "bottom"); reload(); });
        mediaPosRow.addView(posTop); mediaPosRow.addView(posMid); mediaPosRow.addView(posBottom);
        root.addView(mediaPosRow);
    }

    // ---------- Kopfzeile ----------

    private void buildHeaderSection(LinearLayout root, float fs, int d) {
        CheckBox hdr = new CheckBox(ctx);
        hdr.setText("  Uhr, Datum und Wochentag anzeigen");
        hdr.setTextColor(Color.WHITE);
        hdr.setTextSize(15 * fs);
        hdr.setChecked(Settings.showHeader(ctx));
        hdr.setOnCheckedChangeListener((v, on) -> { Settings.setShowHeader(ctx, on); });
        root.addView(hdr);
        CheckBox batt = new CheckBox(ctx);
        batt.setText("  Akkustand anzeigen");
        batt.setTextColor(Color.WHITE);
        batt.setTextSize(15 * fs);
        batt.setChecked(Settings.showBattery(ctx));
        batt.setOnCheckedChangeListener((v, on) -> { Settings.setShowBattery(ctx, on); });
        root.addView(batt);
    }

    // ---------- Posteingang ----------

    private void buildInboxSection(LinearLayout root, float fs, int d) {
        section(root, "Posteingang-Quellen", fs);
        TextView srcHint = new TextView(ctx);
        java.util.List<String> pkgs = NotificationStore.get(ctx).distinctPackages();
        if (pkgs.isEmpty()) {
            srcHint.setText("Noch keine Benachrichtigungen empfangen. "
                    + "Sobald Apps benachrichtigen, erscheinen sie hier zur Auswahl.");
        } else {
            srcHint.setText("Welche Apps im Posteingang erscheinen sollen:");
        }
        srcHint.setTextColor(Color.GRAY);
        srcHint.setTextSize(12 * fs);
        srcHint.setPadding(0, 0, 0, 6 * d);
        root.addView(srcHint);

        android.content.pm.PackageManager pm = ctx.getPackageManager();
        for (String pkg : pkgs) {
            String label;
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (Exception e) { label = pkg; }
            CheckBox cb = new CheckBox(ctx);
            cb.setText("  " + label);
            cb.setTextColor(Color.WHITE);
            cb.setTextSize(14 * fs);
            cb.setChecked(Settings.isSourceEnabled(ctx, pkg));
            final String fp = pkg;
            cb.setOnCheckedChangeListener((v, on) -> { Settings.setSourceEnabled(ctx, fp, on); });
            root.addView(cb);
        }

        section(root, "Posteingang aufräumen", fs);
        TextView keepHint = new TextView(ctx);
        keepHint.setText("Ältere Einträge automatisch entfernen nach:");
        keepHint.setTextColor(Color.GRAY);
        keepHint.setTextSize(12 * fs);
        root.addView(keepHint);
        Button keepBtn = new Button(ctx);
        keepBtn.setText("Aufbewahren: " + Settings.retentionLabel(Settings.retentionDays(ctx)));
        keepBtn.setOnClickListener(new RetentionClick(ctx, keepBtn));
        root.addView(keepBtn);
    }

    // ---------- Registerkarten ----------

    private void buildTabsSection(LinearLayout root, float fs, int d, Runnable tabRefresh) {
        TextView tabsHint = new TextView(ctx);
        tabsHint.setText("Ein-/ausschalten, umbenennen, eigenes Bild wählen, Reihenfolge "
                + "ändern (▲▼) und einstellen, ob die Leiste nach Tippen darin schließt. "
                + "Von \"Widget\" und \"Verknüpfungen\" lassen sich beliebig viele "
                + "Registerkarten anlegen.");
        tabsHint.setTextColor(Color.GRAY);
        tabsHint.setTextSize(12 * fs);
        tabsHint.setPadding(0, 0, 0, 8 * d);
        root.addView(tabsHint);

        List<TabInstance> tabList = Tabs.load(ctx);
        for (int i = 0; i < tabList.size(); i++) {
            root.addView(tabRow(tabList.get(i), i, tabList.size(), fs, d, tabRefresh));
        }

        Button addWidgetTab = new Button(ctx);
        addWidgetTab.setText("+ Neue Widget-Registerkarte");
        addWidgetTab.setOnClickListener(v -> { Tabs.addWidgetTab(ctx); tabRefresh.run(); });
        root.addView(addWidgetTab);

        Button addShortcutsTab = new Button(ctx);
        addShortcutsTab.setText("+ Neue Verknüpfungen-Registerkarte");
        addShortcutsTab.setOnClickListener(v -> { Tabs.addShortcutsTab(ctx); tabRefresh.run(); });
        root.addView(addShortcutsTab);

        Button addMediaTab = new Button(ctx);
        addMediaTab.setText("+ Neue Mediensteuerung-Registerkarte");
        addMediaTab.setOnClickListener(v -> { Tabs.addMediaTab(ctx); tabRefresh.run(); });
        root.addView(addMediaTab);

        section(root, "Aufgaben & Notizen", fs);
        String jtxAccount = Settings.jtxAccountName(ctx);
        TextView jtxHint = new TextView(ctx);
        jtxHint.setText(jtxAccount != null
                ? "Verwendetes DAVx5-Konto: " + jtxAccount
                : "Noch kein DAVx5-Konto eingerichtet.");
        jtxHint.setTextColor(Color.GRAY);
        jtxHint.setTextSize(12 * fs);
        jtxHint.setPadding(0, 0, 0, 6 * d);
        root.addView(jtxHint);
        Button jtxBtn = new Button(ctx);
        jtxBtn.setText(jtxAccount != null ? "JTX-Board-Konto ändern" : "JTX-Board-Konto einrichten");
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
        stop.setText("Leiste beenden");
        stop.setOnClickListener(v ->
                ctx.stopService(new Intent(ctx, EdgeService.class)));
        root.addView(stop);

        TextView ver = new TextView(ctx);
        String vn;
        try { vn = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName; }
        catch (Exception e) { vn = "?"; }
        ver.setText("\nEdgeTab " + vn);
        ver.setTextColor(Color.GRAY);
        ver.setTextSize(12 * fs);
        root.addView(ver);
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
        cb.setChecked(t.enabled);
        cb.setOnCheckedChangeListener((v, on) -> {
            Tabs.update(ctx, t.id, ti -> ti.enabled = on);
            tabRefresh.run();
        });
        head.addView(cb);

        Tab live = Tabs.build(t);
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
        closeCb.setText("  Nach Tippen schließen");
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
            del.setText("  Löschen");
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
        t.setText(s.toUpperCase());
        t.setTextColor(Color.parseColor("#2E9BE6"));
        t.setTextSize(13 * fs);
        t.setPadding(0, 18 * d, 0, 6 * d);
        root.addView(t);
    }

    private interface IntSink { void set(int v); }

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
            btn.setText("Aufbewahren: " + Settings.retentionLabel(next));
        }
    }
}
