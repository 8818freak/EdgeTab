package de.herbers.edgetab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Der Dauerdienst hinter EdgeTab. Rand-Griff + Panel mit Registerkarten.
 *
 * Nach Mathias' Vorgaben und dem BB-Vorbild:
 *  - Die Icon-Spalte sitzt auf der Wisch-Seite (am selben Rand wie der Griff),
 *    damit man einhaendig bedienen kann.
 *  - Einstellungen sind eine eigene Karte, ganz oben und abgesetzt.
 *  - Aufziehen per Tippen ODER Wischen zur Mitte; Zurueckwischen schliesst,
 *    wie im Original ("Wischen Sie in Richtung Bildschirmmitte", erneutes
 *    Hineinwischen bringt zurueck).
 */
public class EdgeService extends Service {

    private WindowManager wm;
    private View handleView;
    private View panelView;
    private SwipePanel panel;
    private boolean panelOpen = false;
    private int currentTab = 0;
    private boolean showingSettings = false;

    private static final String CHANNEL = "edgetab_service";
    private static final String SETTINGS_TAB = "\u2699settings";

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startAsForeground();
        addHandle();
        WidgetHostHolder.startListening(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("refresh", false)) rebuildHandle();
        if (intent != null && intent.getBooleanExtra("open", false)) openPanel();
        return START_STICKY;
    }

    private void startAsForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL,
                    getString(R.string.svc_channel), NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.svc_running))
                .setSmallIcon(R.drawable.ic_calendar).setOngoing(true).build();
        startForeground(1, n);
    }

    // ---- Rand-Griff ----

    private void addHandle() {
        boolean right = Settings.edgeRight(this);
        handleView = buildHandleShape(right);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                dp(14), dp(Settings.handleHeight(this)),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = (right ? Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL;
        lp.y = (int) (Settings.handlePos(this) / 100f * dp(200));

        // Tippen oeffnet. Zusaetzlich Wischen zur Mitte (wie im Original), ohne
        // mit der System-Zurueck-Geste zu kollidieren: wir werten nur eine klar
        // nach innen gerichtete Bewegung als Oeffnen.
        handleView.setOnClickListener(v -> openPanel());
        handleView.setOnTouchListener(new SwipeOpen(this, right));
        wm.addView(handleView, lp);

        // Ohne das hier stiehlt Androids System-Wischgeste (Zurueck/Vorwaerts
        // am Bildschirmrand) die Beruehrung oft schon, bevor sie den Griff
        // erreicht - besonders am linken Rand, wo die Zurueck-Geste liegt.
        // Erklaert den Griff bei Android als "das brauche ich, bitte nicht
        // fuer System-Gesten verwenden".
        handleView.post(() -> {
            if (handleView.getWidth() > 0 && handleView.getHeight() > 0) {
                handleView.setSystemGestureExclusionRects(java.util.Collections.singletonList(
                        new android.graphics.Rect(0, 0, handleView.getWidth(), handleView.getHeight())));
            }
        });
    }

    /**
     * Spitz zulaufender Rand-Griff nach dem Vorbild der BlackBerry
     * Registerkarte Produktivitaet: zwei Spitzen (oben/unten, aus derselben
     * Grafik - unten nur um 180° gedreht) um einen geraden, dehnbaren
     * Mittelteil mit drei Punkten als Zieh-Andeutung. Weiss mit schmalem
     * Schattensaum zur Bildschirmmitte, gezeichnet fuer den rechten Rand -
     * fuer links wird das ganze Stueck per setScaleX gespiegelt.
     */
    private View buildHandleShape(boolean right) {
        int w = dp(14);
        LinearLayout handle = new LinearLayout(this);
        handle.setOrientation(LinearLayout.VERTICAL);
        handle.setElevation(dp(3));

        View top = new View(this);
        top.setBackgroundResource(R.drawable.handle_tip);
        top.setLayoutParams(new LinearLayout.LayoutParams(w, w));
        handle.addView(top);

        FrameLayout middle = new FrameLayout(this);
        middle.setBackgroundResource(R.drawable.handle_middle);
        middle.setLayoutParams(new LinearLayout.LayoutParams(w, 0, 1f));
        ImageView dots = new ImageView(this);
        dots.setImageResource(R.drawable.ic_handle_dots);
        FrameLayout.LayoutParams dotsLp = new FrameLayout.LayoutParams(dp(4), dp(16));
        dotsLp.gravity = Gravity.CENTER;
        middle.addView(dots, dotsLp);
        handle.addView(middle);

        View bottom = new View(this);
        bottom.setBackgroundResource(R.drawable.handle_tip);
        bottom.setScaleY(-1f);
        bottom.setLayoutParams(new LinearLayout.LayoutParams(w, w));
        handle.addView(bottom);

        if (!right) handle.setScaleX(-1f);
        return handle;
    }

    /** Wischen zur Mitte oeffnet. Statisch gehalten, um einen Fehler im
     *  mitgelieferten d8 mit inneren Klassen zu umgehen. */
    private static final class SwipeOpen implements View.OnTouchListener {
        private final EdgeService svc;
        private final boolean right;
        private float downX;
        SwipeOpen(EdgeService svc, boolean right) { this.svc = svc; this.right = right; }
        public boolean onTouch(View v, MotionEvent e) {
            int a = e.getAction();
            if (a == MotionEvent.ACTION_DOWN) { downX = e.getRawX(); return false; }
            if (a == MotionEvent.ACTION_UP) {
                float dx = e.getRawX() - downX;
                boolean inward = right ? dx < -svc.dp(20) : dx > svc.dp(20);
                if (inward) { svc.openPanel(); return true; }
            }
            return false;
        }
    }

    private void rebuildHandle() {
        try { if (handleView != null) wm.removeView(handleView); } catch (Exception ignored) {}
        addHandle();
    }

    // ---- Panel ----

    private void openPanel() {
        if (panelOpen) return;
        panelOpen = true;
        boolean right = Settings.edgeRight(this);

        FrameLayout root = new FrameLayout(this);
        root.setOnClickListener(v -> closePanel());
        root.setBackgroundColor(Color.parseColor("#66000000"));

        panel = new SwipePanel(this, right, this::closePanel);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.panel_bg);
        panel.setClickable(true);
        panel.setPadding(dp(14), dp(18), dp(14), dp(18));
        panel.getBackground().setAlpha((int) ((100 - Settings.transparency(this)) / 100f * 255));

        List<Tab> tabs = Tabs.buildActive(this);
        if (!showingSettings && currentTab >= tabs.size()) currentTab = 0;

        // Kopfzeile oben (Uhr, Datum, Akku) - optional.
        // Darueber legt sich bei Bedarf die Wiederherstellen-Leiste.
        FrameLayout top = new FrameLayout(this);
        if (Settings.showHeader(this)) {
            top.addView(new HeaderView().build(this));
        }
        top.addView(UndoBar.build(this, Settings.fontScale(this), this::closePanel));
        panel.addView(top);

        // Darunter: [Icon-Spalte] neben [Inhalt], je nach Seite gespiegelt.
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout iconCol = buildIconColumn(tabs, right);
        FrameLayout content = new FrameLayout(this);
        renderContent(content, tabs);

        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(iconColumnWidthDp()),
                ViewGroup.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);

        // Icon-Spalte auf die Wisch-/Rand-Seite: rechter Rand -> Spalte rechts.
        if (right) { body.addView(content, contentLp); body.addView(iconCol, iconLp); }
        else       { body.addView(iconCol, iconLp); body.addView(content, contentLp); }
        panel.addView(body);

        int wPx = dp(Settings.panelWidth(this)) + dp(iconColumnWidthDp());
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(wPx,
                ViewGroup.LayoutParams.MATCH_PARENT);
        plp.gravity = right ? Gravity.END : Gravity.START;
        root.addView(panel, plp);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        panelView = root;
        wm.addView(panelView, lp);

        panel.setTranslationX(right ? wPx : -wPx);
        panel.animate().translationX(0).setDuration(180).start();
        // Das Zuwischen erledigt SwipePanel selbst (onInterceptTouchEvent),
        // damit die Geste vor den Kacheln/dem Scrollbereich erkannt wird.
    }

    /**
     * Das Panel als eigenes Layout, das die Schliess-Wischgeste VOR den Kacheln
     * abfaengt. Ein frueherer OnTouchListener auf Panel/Wurzel schlug fehl, weil
     * die Kacheln und der ScrollView die Beruehrung vorher verbrauchten.
     */
    static final class SwipePanel extends LinearLayout {
        private final boolean right;
        private final Runnable onClose;
        private final float threshold;
        private float downX, downY;
        SwipePanel(Context c, boolean right, Runnable onClose) {
            super(c);
            this.right = right;
            this.onClose = onClose;
            this.threshold = 48 * c.getResources().getDisplayMetrics().density;
        }
        @Override public boolean onInterceptTouchEvent(MotionEvent e) {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) { downX = e.getX(); downY = e.getY(); return false; }
            if (a == MotionEvent.ACTION_MOVE) {
                float dx = e.getX() - downX, dy = e.getY() - downY;
                // deutlich waagerechte Bewegung aus dem Bild heraus -> abfangen
                if (Math.abs(dx) > threshold && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                    boolean outward = right ? dx > 0 : dx < 0;
                    if (outward) {
                        if (onClose != null) post(onClose);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** Breite der Icon-Spalte, mitwachsend mit der eingestellten Icongroesse
     *  (+24dp Polster/Hintergrundflaeche - passt bei Standard-28dp genau auf
     *  die bisherigen 52dp). */
    private int iconColumnWidthDp() { return Settings.iconSize(this) + 24; }

    private LinearLayout buildIconColumn(List<Tab> tabs, boolean right) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(0, dp(18), 0, dp(18));

        // Einstellungen ganz oben, abgesetzt (eigene "Karte", wie im Original).
        // Bewusst das schlichte "⚙"-Zeichen statt des Vektor-Icons - Mathias'
        // Wunsch, dass ALLE Einstellungs-Zahnraeder in der App (auch dieses
        // hier) gleich aussehen, und die anderen (ShortcutsTab/ContactsTab)
        // waren zuerst da.
        View gear = gearGlyphView(showingSettings);
        gear.setOnClickListener(v -> { showingSettings = true; refreshPanel(); });
        col.addView(gear);

        View sep = new View(this);
        LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(dp(24), Math.max(1, dp(1)));
        sepLp.topMargin = dp(10); sepLp.bottomMargin = dp(14);
        sep.setLayoutParams(sepLp);
        sep.setBackgroundColor(Color.parseColor("#44FFFFFF"));
        col.addView(sep);

        // Fuellt den Platz zwischen Einstellungen (oben) und den restlichen
        // Karten (unten) - Mathias' Wunsch, alle Karten am unteren Rand zu
        // versammeln statt sie direkt unter den Einstellungen aufzureihen.
        View spacer = new View(this);
        col.addView(spacer, new LinearLayout.LayoutParams(dp(1), 0, 1f));

        for (int i = 0; i < tabs.size(); i++) {
            final int idx = i;
            Tab t = tabs.get(i);
            ImageView iv = iconView(t.iconRes(), t.customIconPath(), !showingSettings && i == currentTab);
            iv.setOnClickListener(v -> { showingSettings = false; currentTab = idx; refreshPanel(); });
            col.addView(iv);
        }
        return col;
    }

    /** Das Haupteinstellungen-Zahnrad ganz oben - gleiche Box-Groesse/Abstaende
     *  wie iconView(), aber als Text statt Bild (siehe Kommentar oben). */
    private View gearGlyphView(boolean active) {
        TextView tv = new TextView(this);
        tv.setText("⚙");
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#B0B0B0"));
        tv.setTextSize(Settings.iconSize(this) * 0.62f);
        int s = dp(Settings.iconSize(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s + dp(12), s + dp(10));
        lp.topMargin = dp(5); lp.bottomMargin = dp(5);
        tv.setLayoutParams(lp);
        if (active) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#2E9BE6"));
            bg.setCornerRadius(dp(10));
            tv.setBackground(bg);
        }
        return tv;
    }

    /** Aktiver Tab liegt hinterlegt auf einer Flaeche, wie im BB-Original.
     *  Eigene (Foto-)Icons werden nicht eingefaerbt, nur Standard-Vektoricons. */
    private ImageView iconView(int res, String customIconPath, boolean active) {
        ImageView iv = new ImageView(this);
        android.graphics.Bitmap bmp = customIconPath != null ? loadIcon(customIconPath) : null;
        if (bmp != null) {
            iv.setImageBitmap(bmp);
            iv.clearColorFilter();
        } else {
            iv.setImageResource(res);
            iv.setColorFilter(Color.parseColor(active ? "#FFFFFF" : "#B0B0B0"));
        }
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int s = dp(Settings.iconSize(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s + dp(12), s + dp(10));
        lp.topMargin = dp(5); lp.bottomMargin = dp(5);
        iv.setLayoutParams(lp);
        iv.setPadding(dp(6), dp(5), dp(6), dp(5));
        if (active) {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(Color.parseColor("#2E9BE6"));
            bg.setCornerRadius(dp(10));
            iv.setBackground(bg);
        }
        return iv;
    }

    private android.graphics.Bitmap loadIcon(String path) {
        try { return android.graphics.BitmapFactory.decodeFile(path); }
        catch (Throwable t) { return null; }
    }

    private void renderContent(FrameLayout content, List<Tab> tabs) {
        content.removeAllViews();
        float fs = Settings.fontScale(this);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(this);
        header.setTextColor(Color.WHITE);
        header.setTextSize(18 * fs);
        header.setPadding(0, 0, 0, dp(12));

        // Der eigentliche Karteninhalt bekommt bewusst die GESAMTE restliche
        // Hoehe (Gewicht 1) statt WRAP_CONTENT: eine reine ScrollView macht das
        // ohnehin richtig (fuellt + scrollt), aber ein FrameLayout - wie es die
        // Mediensteuerung fuer ihre feste Oben/Mitte/Unten-Positionierung nutzt
        // - waechst unter WRAP_CONTENT nur auf die Groesse seines Inhalts und
        // hat dann gar keinen Platz, in dem "oben/mitte/unten" ueberhaupt einen
        // sichtbaren Unterschied machen koennte (Mathias' Fund: Einstellung
        // schien wirkungslos).
        LinearLayout.LayoutParams fillLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);

        if (showingSettings) {
            header.setText("Einstellungen");
            wrap.addView(header);
            wrap.addView(new SettingsTab().buildContent(this, this::closePanel, this::refreshPanel), fillLp);
        } else if (tabs.isEmpty()) {
            header.setText("Keine Karten aktiv");
            wrap.addView(header);
            TextView t = new TextView(this);
            t.setText("Aktiviere Karten oben im Zahnrad.");
            t.setTextColor(Color.parseColor("#9E9E9E"));
            t.setTextSize(14 * fs);
            wrap.addView(t);
        } else {
            Tab t = tabs.get(currentTab);
            header.setText(t.title(this));
            wrap.addView(header);
            wrap.addView(t.buildContent(this, this::closePanel, this::refreshPanel), fillLp);
        }
        // Auch "wrap" selbst braucht eine EXAKTE Hoehe (nicht WRAP_CONTENT) -
        // sonst hat das Gewicht-1-Kind oben gar keine echte Restflaeche, die
        // es fuellen koennte (LinearLayout-Gewichte brauchen eine definierte
        // Elternhoehe, um etwas zu verteilen).
        content.addView(wrap, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /** Panel neu aufbauen (nach Tab-Wechsel oder Einstellungsaenderung). */
    private void refreshPanel() {
        closePanel();
        openPanel();
    }

    private void closePanel() {
        if (!panelOpen || panelView == null) return;
        UndoBar.detach();
        panelOpen = false;
        try { wm.removeView(panelView); } catch (Exception ignored) {}
        panelView = null;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        WidgetHostHolder.stopListening(this);
        try { if (handleView != null) wm.removeView(handleView); } catch (Exception ignored) {}
        try { if (panelView  != null) wm.removeView(panelView);  } catch (Exception ignored) {}
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
