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

    /** Stabile, unterscheidbare Farbe je Schluessel (z.B. Paketname) - fuer
     *  den farbigen Balken je Zeile, wenn es keine "echte" Farbe gibt (wie
     *  die Kalenderfarbe bei Terminen). Wie im Kalender-Tab, nur ohne
     *  vorgegebene Farbe je Quelle. */
    static int colorFor(String key) {
        int hue = Math.floorMod(key == null ? 0 : key.hashCode(), 360);
        return android.graphics.Color.HSVToColor(new float[]{hue, 0.5f, 0.85f});
    }

    /** Hebt eine ScrollView + schwebendes Symbol in einen gemeinsamen
     *  Container - das Symbol bleibt beim Scrollen an fester Stelle. */
    static View withFab(Context ctx, View scroll, View fab) {
        FrameLayout frame = new FrameLayout(ctx);
        frame.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        frame.addView(fab);
        return frame;
    }
}
