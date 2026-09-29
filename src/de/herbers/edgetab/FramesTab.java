package de.herbers.edgetab;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Widget 2": eine native Active-Frames-Karte in der Leiste. Zeigt - wie
 * BlackBerry OS10 - die zuletzt benutzten Apps als Kacheln (oben links die
 * zuletzt geoeffnete), mit rotem Stern bei Neuem und dem letzten App-Foto/
 * Benachrichtigungsbild.
 *
 * Die Daten (Reihenfolge, Sterne, echte App-Fotos) kommen ueber einen
 * Datenkanal aus der App "Active Frames" (FramesDataProvider). Dadurch braucht
 * EdgeTab KEINE eigenen heiklen Berechtigungen (Nutzungsdaten/Bedienungshilfe)
 * - Active Frames bleibt die einzige Datenquelle. Weil die Leiste die Kacheln
 * selbst zeichnet (kein Launcher-Host, keine RemoteViews-Sammlung), scrollt
 * diese Karte immer zuverlaessig - anders als das Home-Screen-Widget auf alten
 * Launchern.
 */
public class FramesTab extends BaseTab {

    /** Datenkanal der App Active Frames. */
    private static final String AUTH = "de.herbers.activeframes.data";

    /** Ob das Zahnrad-Menue dieser Karte gerade offen ist - je Karten-Id, weil
     *  bei jedem Aufbau ein neues FramesTab-Objekt entsteht (wie ShortcutsTab). */
    private static final Set<String> MENU_OPEN = new HashSet<>();

    FramesTab(TabInstance inst) { super(inst, R.string.tab_frames, R.drawable.ic_widget); }

    /** Eine Kachel: Paketname, "Neues"-Stern, Dateiname des Bilds (oder leer). */
    private static final class Tile {
        String pkg;
        boolean unread;
        String image;
    }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);
        final Runnable refresh = () -> { if (refreshContent != null) refreshContent.run(); };

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        root.addView(toolbar(ctx, d, refresh));

        List<Tile> tiles = queryTiles(ctx);
        if (tiles == null) {
            root.addView(hint(ctx, d, ctx.getString(R.string.frames_need_af)));
            return scroll;
        }
        if (tiles.isEmpty()) {
            root.addView(hint(ctx, d, ctx.getString(R.string.frames_empty)));
            return scroll;
        }

        int cols = Settings.framesColumns(ctx);
        // Scrollen aus = nur so wenige Kacheln, dass es kompakt bleibt.
        int max = Settings.framesScroll(ctx) ? Settings.framesMaxTiles(ctx) : cols * 2;
        if (tiles.size() > max) tiles = new ArrayList<>(tiles.subList(0, max));
        root.addView(grid(ctx, tiles, cols, close, refresh, d));
        return scroll;
    }

    // ---------- Datenkanal (Active Frames) ----------

    /** Kacheln in Reihenfolge abfragen. null = Active Frames nicht erreichbar
     *  (nicht installiert / kein Nutzungszugriff); leere Liste = noch keine. */
    private List<Tile> queryTiles(Context ctx) {
        Uri uri = new Uri.Builder().scheme("content").authority(AUTH).appendPath("tiles").build();
        Cursor c;
        try {
            c = ctx.getContentResolver().query(uri, null, null, null, null);
        } catch (Throwable t) {
            return null;
        }
        if (c == null) return null;
        List<Tile> out = new ArrayList<>();
        try {
            int iP = c.getColumnIndex("pkg");
            int iU = c.getColumnIndex("unread");
            int iI = c.getColumnIndex("image");
            while (c.moveToNext()) {
                Tile t = new Tile();
                t.pkg = iP >= 0 ? c.getString(iP) : null;
                t.unread = iU >= 0 && c.getInt(iU) != 0;
                t.image = iI >= 0 ? c.getString(iI) : "";
                if (t.pkg != null) out.add(t);
            }
        } finally {
            c.close();
        }
        return out;
    }

    private Bitmap loadImage(Context ctx, String fileName) {
        Uri uri = new Uri.Builder().scheme("content").authority(AUTH)
                .appendPath("image").appendPath(fileName).build();
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            return in != null ? BitmapFactory.decodeStream(in) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Zustandsaenderung an Active Frames zurueckmelden ("opened"/"dismiss"),
     *  damit Stern und Reihenfolge dort dieselbe Quelle bleiben. */
    private void notifyAF(Context ctx, String method, String pkg) {
        Uri uri = new Uri.Builder().scheme("content").authority(AUTH).appendPath("tiles").build();
        try { ctx.getContentResolver().call(uri, method, pkg, null); }
        catch (Throwable ignored) {}
    }

    // ---------- Kacheln zeichnen ----------

    /** Kacheln als verschachtelte LinearLayout-Reihen (NICHT GridLayout):
     *  GridLayout misst seine Hoehe in einer ScrollView unzuverlaessig und
     *  scrollt dann nicht. Gleichbreite Spalten per Gewicht, variable
     *  Reihenhoehen wie im Home-Screen-Widget. */
    private View grid(Context ctx, List<Tile> tiles, int cols, Runnable close, Runnable refresh, int d) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);

        int fullH = Settings.framesTileHeight(ctx);
        int shortH = Math.max(52, Math.round(fullH * Settings.framesShortPct(ctx) / 100f));
        int bigRows = Settings.framesBigRows(ctx);
        int rows = (tiles.size() + cols - 1) / cols;

        for (int r = 0; r < rows; r++) {
            int hDp = r < bigRows ? fullH : shortH;
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            col.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, hDp * d));
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                tlp.setMargins(3 * d, 3 * d, 3 * d, 3 * d);
                if (idx < tiles.size()) {
                    row.addView(buildTile(ctx, tiles.get(idx), close, refresh, d), tlp);
                } else {
                    row.addView(new View(ctx), tlp); // Fueller, damit die Spalten gleich breit bleiben
                }
            }
        }
        return col;
    }

    private View buildTile(Context ctx, Tile t, Runnable close, Runnable refresh, int d) {
        FrameLayout card = new FrameLayout(ctx);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#22282C"));
        bg.setCornerRadius(10 * d);
        card.setBackground(bg);
        card.setClipToOutline(true);

        android.content.pm.PackageManager pm = ctx.getPackageManager();

        // Hintergrund: echtes App-Foto / Benachrichtigungsbild, sonst App-Symbol.
        ImageView img = new ImageView(ctx);
        Bitmap photo = (t.image != null && !t.image.isEmpty()) ? loadImage(ctx, t.image) : null;
        if (photo != null) {
            img.setImageBitmap(photo);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            try { img.setImageDrawable(pm.getApplicationIcon(t.pkg)); } catch (Throwable ignored) {}
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int pad = 18 * d;
            img.setPadding(pad, pad, pad, pad);
        }
        card.addView(img, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Untere Titelleiste: kleines Symbol + Name + X (Schliessen), wie BB.
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.parseColor("#B0000000"));
        bar.setPadding(6 * d, 4 * d, 2 * d, 4 * d);

        ImageView smallIcon = new ImageView(ctx);
        try { smallIcon.setImageDrawable(pm.getApplicationIcon(t.pkg)); } catch (Throwable ignored) {}
        int is = 16 * d;
        LinearLayout.LayoutParams silp = new LinearLayout.LayoutParams(is, is);
        silp.rightMargin = 5 * d;
        bar.addView(smallIcon, silp);

        TextView name = new TextView(ctx);
        name.setText(appLabel(pm, t.pkg));
        name.setTextColor(Color.WHITE);
        name.setTextSize(11);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        bar.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView x = new TextView(ctx);
        x.setText("✕");
        x.setTextColor(Color.parseColor("#E0E0E0"));
        x.setTextSize(12);
        x.setPadding(8 * d, 2 * d, 6 * d, 2 * d);
        x.setOnClickListener(v -> { notifyAF(ctx, "dismiss", t.pkg); refresh.run(); });
        bar.addView(x);

        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barLp.gravity = Gravity.BOTTOM;
        card.addView(bar, barLp);

        // Roter Stern oben rechts bei etwas Neuem: derselbe Fuenf-Arm-Stern wie
        // im Home-Screen-Widget (Vektor-Drawable) - NICHT das Zeichen "✳", das
        // je nach Schriftart sechs-/achtstrahlig erschien.
        if (t.unread) {
            ImageView star = new ImageView(ctx);
            star.setImageResource(R.drawable.ic_star_badge);
            int ss = 20 * d;
            FrameLayout.LayoutParams stp = new FrameLayout.LayoutParams(ss, ss);
            stp.gravity = Gravity.TOP | Gravity.END;
            stp.topMargin = 4 * d;
            stp.rightMargin = 4 * d;
            star.setLayoutParams(stp);
            card.addView(star);
        }

        // Tippen auf die Kachel: als geoeffnet melden (Stern weg) + App starten.
        card.setOnClickListener(v -> {
            notifyAF(ctx, "opened", t.pkg);
            Launcher.launchApp(ctx.getApplicationContext(), t.pkg, Launcher.bgAllowed());
            if (close != null) close.run();
        });
        return card;
    }

    private String appLabel(android.content.pm.PackageManager pm, String pkg) {
        try { return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
        catch (Throwable t) { return pkg; }
    }

    // ---------- Werkzeugleiste + Einstellungen dieser Karte ----------

    private View toolbar(Context ctx, int d, Runnable refresh) {
        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.VERTICAL);

        boolean open = MENU_OPEN.contains(inst.id);
        TextView gear = new TextView(ctx);
        gear.setText("⚙");
        gear.setTextColor(Color.parseColor("#B0B0B0"));
        gear.setTextSize(20);
        gear.setPadding(8 * d, 6 * d, 8 * d, 6 * d);
        gear.setOnClickListener(v -> {
            if (open) MENU_OPEN.remove(inst.id); else MENU_OPEN.add(inst.id);
            refresh.run();
        });
        wrap.addView(gear);

        if (open) wrap.addView(settingsPanel(ctx, d, refresh));
        return wrap;
    }

    private View settingsPanel(Context ctx, int d, Runnable refresh) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FF1C1C1E"));
        bg.setCornerRadius(10 * d);
        box.setBackground(bg);
        box.setPadding(10 * d, 8 * d, 10 * d, 10 * d);

        CheckBox scroll = new CheckBox(ctx);
        scroll.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E9BE6")));
        scroll.setText(R.string.frames_scroll);
        scroll.setTextColor(Color.WHITE);
        scroll.setChecked(Settings.framesScroll(ctx));
        scroll.setOnCheckedChangeListener((v, on) -> { Settings.setFramesScroll(ctx, on); refresh.run(); });
        box.addView(scroll);

        TextView note = new TextView(ctx);
        note.setText(R.string.frames_scroll_note);
        note.setTextColor(Color.parseColor("#8AB49A"));
        note.setTextSize(11);
        box.addView(note);

        box.addView(stepper(ctx, d, ctx.getString(R.string.frames_columns),
                () -> Settings.framesColumns(ctx), n -> Settings.setFramesColumns(ctx, n), 1, refresh));
        box.addView(stepper(ctx, d, ctx.getString(R.string.frames_height),
                () -> Settings.framesTileHeight(ctx), n -> Settings.setFramesTileHeight(ctx, n), 10, refresh));
        box.addView(stepper(ctx, d, ctx.getString(R.string.frames_count),
                () -> Settings.framesMaxTiles(ctx), n -> Settings.setFramesMaxTiles(ctx, n), 2, refresh));
        box.addView(stepper(ctx, d, ctx.getString(R.string.frames_bigrows),
                () -> Settings.framesBigRows(ctx), n -> Settings.setFramesBigRows(ctx, n), 1, refresh));
        box.addView(stepper(ctx, d, ctx.getString(R.string.frames_shortpct),
                () -> Settings.framesShortPct(ctx), n -> Settings.setFramesShortPct(ctx, n), 5, refresh));

        TextView src = new TextView(ctx);
        src.setText(R.string.frames_source_note);
        src.setTextColor(Color.parseColor("#8899AA"));
        src.setTextSize(11);
        src.setPadding(0, 10 * d, 0, 0);
        box.addView(src);
        return box;
    }

    private interface IntGet { int get(); }
    private interface IntSet { void set(int v); }

    private View stepper(Context ctx, int d, String label, IntGet g, IntSet s, int step, Runnable refresh) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 8 * d, 0, 0);

        TextView lab = new TextView(ctx);
        lab.setText(label);
        lab.setTextColor(Color.parseColor("#CCCCCC"));
        lab.setTextSize(12);
        lab.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(lab);

        TextView minus = stepBtn(ctx, "−");
        minus.setOnClickListener(v -> { s.set(g.get() - step); refresh.run(); });
        row.addView(minus);

        TextView val = new TextView(ctx);
        val.setText(String.valueOf(g.get()));
        val.setTextColor(Color.WHITE);
        val.setTextSize(15);
        val.setPadding(10 * d, 0, 10 * d, 0);
        row.addView(val);

        TextView plus = stepBtn(ctx, "+");
        plus.setOnClickListener(v -> { s.set(g.get() + step); refresh.run(); });
        row.addView(plus);
        return row;
    }

    private TextView stepBtn(Context ctx, String s) {
        TextView t = new TextView(ctx);
        t.setText("  " + s + "  ");
        t.setTextColor(Color.parseColor("#2E9BE6"));
        t.setTextSize(18);
        return t;
    }

    private View hint(Context ctx, int d, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(Color.parseColor("#CCCCCC"));
        t.setTextSize(14);
        t.setPadding(0, 12 * d, 0, 12 * d);
        return t;
    }
}
