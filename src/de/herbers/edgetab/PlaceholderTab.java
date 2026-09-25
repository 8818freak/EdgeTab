package de.herbers.edgetab;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Eine noch nicht ausgebaute Karte. Zeigt einen freundlichen Hinweis, damit
 * das Geruest der Registerkarten schon vollstaendig steht und man sieht, wo
 * welche Funktion sitzen wird. Wird nacheinander durch echte Karten ersetzt.
 */
public class PlaceholderTab extends BaseTab {

    private final String hint;
    private final String rawDefaultTitle;

    /** rawDefaultTitle ist hier ein interner, nicht uebersetzbarer Typ-
     *  Schluessel (z.B. ein unbekannter TabInstance.type-Wert) statt eines
     *  String-Resource - darum ueberschreibt diese Klasse title(Context)
     *  statt BaseTabs Resource-basierten Standardnamen zu nutzen. */
    public PlaceholderTab(TabInstance inst, String rawDefaultTitle, int defaultIcon, String hint) {
        super(inst, R.string.tab_unknown, defaultIcon);
        this.rawDefaultTitle = rawDefaultTitle;
        this.hint = hint;
    }

    @Override
    public String title(Context ctx) {
        return (inst.name != null && !inst.name.trim().isEmpty()) ? inst.name : rawDefaultTitle;
    }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int dp = (int) ctx.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, 40 * dp, 0, 40 * dp);

        TextView t = new TextView(ctx);
        t.setText(hint);
        t.setTextColor(Color.parseColor("#9E9E9E"));
        t.setTextSize(14);
        t.setGravity(Gravity.CENTER);
        box.addView(t);
        return box;
    }
}
