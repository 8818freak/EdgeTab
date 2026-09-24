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

    public PlaceholderTab(TabInstance inst, String defaultTitle, int defaultIcon, String hint) {
        super(inst, defaultTitle, defaultIcon);
        this.hint = hint;
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
