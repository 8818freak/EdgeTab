package de.herbers.edgetab;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Gemeinsame Basis aller Registerkarten-Typen: loest Anzeigename/Icon der
 * TabInstance auf (eigener Name/eigenes Bild, sonst Standard) und kapselt
 * das "nach Tippen schliessen"-Verhalten. Spart, das in jeder Karte (Kalender,
 * Posteingang, Widget, Platzhalter) einzeln nachzubauen.
 */
abstract class BaseTab implements Tab {

    final TabInstance inst;
    private final int defaultTitleRes;
    private final int defaultIcon;

    /** defaultTitleRes: String-Resource statt fertigem Text, damit der
     *  Standardname je nach Geraetesprache uebersetzt erscheint - aufgeloest
     *  erst in title(Context), da der Konstruktor selbst keinen Context hat
     *  (siehe die super(inst, R.string.tab_xxx, ...)-Aufrufe der Unterklassen). */
    BaseTab(TabInstance inst, int defaultTitleRes, int defaultIcon) {
        this.inst = inst;
        this.defaultTitleRes = defaultTitleRes;
        this.defaultIcon = defaultIcon;
    }

    public String id() { return inst.id; }

    public String title(Context ctx) {
        return (inst.name != null && !inst.name.trim().isEmpty()) ? inst.name : ctx.getString(defaultTitleRes);
    }

    public int iconRes() {
        int fromSet = IconSet.resFor(inst.iconKey);
        return fromSet != 0 ? fromSet : defaultIcon;
    }

    public String customIconPath() { return inst.iconPath; }

    public boolean closeOnTap() { return inst.closeOnTap; }

    /** Liefert closePanel unveraendert, wenn diese Karte danach schliessen
     *  soll - sonst eine Runnable, die nichts tut. */
    final Runnable effectiveClose(Runnable closePanel) {
        return closeOnTap() ? closePanel : NO_OP;
    }

    private static final Runnable NO_OP = () -> {};

    /** Rundes, farbiges Symbol zum Hinzufuegen - wie die schwebenden
     *  Stift-/Personen-Knoepfe in BBs eigener Kalender-/Kontakte-/Hub-
     *  Oberflaeche (Mathias' Screenshots), statt eines gewoehnlichen
     *  Text-Knopfes. */
    static View fab(Context ctx, int iconRes, String bgColorHex, View.OnClickListener onClick) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        ImageView fab = new ImageView(ctx);
        fab.setImageResource(iconRes);
        int pad = 14 * d;
        fab.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor(bgColorHex));
        fab.setBackground(bg);
        fab.setElevation(6 * d);
        int s = 52 * d;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(s, s);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = 16 * d;
        fab.setLayoutParams(lp);
        fab.setClickable(true);
        fab.setOnClickListener(onClick);
        return fab;
    }

    /** Wie fab(), aber mit dem bekannten "⚙"-Textzeichen statt eines Icons -
     *  dasselbe Zahnrad, das auch die Haupteinstellungen und die Menues in
     *  ShortcutsTab/ContactsTab verwenden, nur in einer runden, farbigen
     *  Kachel (Mathias' Wunsch: "das bereits bekannte [Zahnrad] von den
     *  andern Menüs nehmen und da eine runde, farbige Kachel drum bauen"). */
    static View fabGear(Context ctx, String bgColorHex, View.OnClickListener onClick) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        android.widget.TextView gear = new android.widget.TextView(ctx);
        gear.setText("⚙");
        gear.setTextColor(Color.WHITE);
        gear.setTextSize(22);
        gear.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor(bgColorHex));
        gear.setBackground(bg);
        gear.setElevation(6 * d);
        int s = 52 * d;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(s, s);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = 16 * d;
        gear.setLayoutParams(lp);
        gear.setClickable(true);
        gear.setOnClickListener(onClick);
        return gear;
    }

    /** Langer Druck auf ein Textfeld fuegt den Text aus der Zwischenablage an der
     *  Einfuegemarke ein. Noetig, weil die schwebende System-Auswahlleiste
     *  (Einfuegen/Kopieren) ueber EdgeTabs Overlay-Fenster meist nicht erscheint;
     *  der Einfuegen-Befehl selbst funktioniert. Ist nichts in der Zwischenablage,
     *  wird der lange Druck durchgereicht (dann versucht es das System selbst). */
    static void enableClipboardPaste(final android.widget.EditText input) {
        input.setOnLongClickListener(v -> {
            try {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        input.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null
                        && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence paste = cm.getPrimaryClip().getItemAt(0).coerceToText(input.getContext());
                    if (paste != null && paste.length() > 0) {
                        int a = Math.max(0, input.getSelectionStart());
                        int b = Math.max(0, input.getSelectionEnd());
                        input.getText().replace(Math.min(a, b), Math.max(a, b), paste);
                        return true; // verbraucht - wir haben eingefuegt
                    }
                }
            } catch (Throwable ignored) {}
            return false; // nichts zum Einfuegen -> System seinen Weg gehen lassen
        });
    }

    /** Stabile, unterscheidbare Farbe je Schluessel (z.B. Paketname) - fuer
     *  den farbigen Balken je Zeile, wenn es keine "echte" Farbe gibt (wie
     *  die Kalenderfarbe bei Terminen). Wie im Kalender-Tab, nur ohne
     *  vorgegebene Farbe je Quelle. */
    static int colorFor(String key) {
        return de.herbers.common.ColorUtil.colorFor(key);
    }

    /** Wie fabGear(), aber mit frei waehlbarem Zeichen und Platzierung - fuer
     *  z.B. den "nach oben"-Knopf (↑) unten links, neben dem mittigen Haupt-
     *  Knopf. gravity bestimmt die Ecke; bei START/END wird automatisch ein
     *  Seitenabstand gesetzt, damit er nicht am Rand klebt. */
    static View fabGlyph(Context ctx, String glyph, String bgColorHex, int gravity, View.OnClickListener onClick) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        android.widget.TextView t = new android.widget.TextView(ctx);
        t.setText(glyph);
        t.setTextColor(Color.WHITE);
        t.setTextSize(22);
        t.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor(bgColorHex));
        t.setBackground(bg);
        t.setElevation(6 * d);
        int s = 52 * d;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(s, s);
        lp.gravity = gravity;
        lp.bottomMargin = 16 * d;
        if ((gravity & Gravity.START) == Gravity.START) lp.leftMargin = 16 * d;
        else if ((gravity & Gravity.END) == Gravity.END) lp.rightMargin = 16 * d;
        t.setLayoutParams(lp);
        t.setClickable(true);
        t.setOnClickListener(onClick);
        return t;
    }

    /** Hebt eine ScrollView + schwebendes Symbol in einen gemeinsamen
     *  Container - das Symbol bleibt beim Scrollen an fester Stelle. */
    static View withFab(Context ctx, View scroll, View fab) {
        return withFab(ctx, scroll, fab, null);
    }

    /** Wie withFab(), aber mit einem zweiten schwebenden Knopf (z.B. unten links
     *  der "nach oben"-Knopf, mittig der Haupt-Knopf). extra darf null sein. */
    static View withFab(Context ctx, View scroll, View fab, View extra) {
        FrameLayout frame = new FrameLayout(ctx);
        frame.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        if (extra != null) frame.addView(extra);
        if (fab != null) frame.addView(fab);
        return frame;
    }

    /** Haengt an eine Listen-ScrollView einen "nach oben"-Knopf, der NUR
     *  erscheint, wenn wirklich gescrollt wurde, und beim Erscheinen den
     *  Haupt-Knopf zur Gegenseite schiebt (so sind beide gleichmaessig verteilt).
     *  Beide sitzen auf der Seite, an der EdgeTab am Bildschirmrand klebt
     *  ({@link Settings#edgeRight}). primaryFab darf null sein (Listen ohne
     *  eigenen Aktionsknopf). Fuer alle Listen-Karten, deren Inhalt laenger als
     *  der Bildschirm werden kann. */
    static View withScrollTop(Context ctx, final android.widget.ScrollView scroll, final View primaryFab) {
        final int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        final boolean right = Settings.edgeRight(ctx);
        FrameLayout frame = new FrameLayout(ctx);
        frame.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        final View up = fabGlyph(ctx, "↑", "#5A5A5E",
                Gravity.BOTTOM | (right ? Gravity.END : Gravity.START),
                v -> scroll.smoothScrollTo(0, 0));
        up.setVisibility(View.GONE);
        frame.addView(up);
        if (primaryFab != null) frame.addView(primaryFab);

        final int threshold = 160 * d;
        // ViewTreeObserver statt setOnScrollChangeListener: letzteres gibt es je
        // ScrollView nur EINMAL, und z.B. der Posteingang nutzt es bereits fuer
        // das Merken der Scroll-Position. Der ViewTreeObserver erlaubt mehrere
        // Beobachter, die friedlich nebeneinander laufen.
        scroll.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int y = scroll.getScrollY();
            boolean show = y > threshold;
            if (show == (up.getVisibility() == View.VISIBLE)) return; // nichts aendert sich
            up.setVisibility(show ? View.VISIBLE : View.GONE);
            if (primaryFab != null) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) primaryFab.getLayoutParams();
                if (show) {
                    // Haupt-Knopf auf die Gegenseite des "nach oben"-Knopfes.
                    lp.gravity = Gravity.BOTTOM | (right ? Gravity.START : Gravity.END);
                    lp.leftMargin = right ? 16 * d : 0;
                    lp.rightMargin = right ? 0 : 16 * d;
                } else {
                    lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    lp.leftMargin = 0; lp.rightMargin = 0;
                }
                primaryFab.setLayoutParams(lp);
            }
        });
        return frame;
    }
}
