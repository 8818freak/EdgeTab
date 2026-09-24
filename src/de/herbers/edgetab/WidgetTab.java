package de.herbers.edgetab;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Bettet ein oder mehrere frei waehlbare App-Widgets in die Leiste ein - z.B.
 * das echte Posteingang-Widget des BlackBerry Hub, mit echten Inhalten,
 * unabhaengig davon, ob eine Benachrichtigung noch lebt. Es kann beliebig
 * viele Widget-Registerkarten geben (siehe Tabs.addWidgetTab); jede zeigt
 * ihre eigene Liste von Widgets uebereinander.
 *
 * "Nach Tippen schliessen" laesst sich fuer diese Karte einstellen. Da fremde
 * Widgets ihre Klicks selbst verarbeiten (eigene PendingIntents, nicht von
 * uns abfangbar), wird das ueber WidgetFrame angenaehert: jede kurze
 * Beruehrung ohne nennenswerte Bewegung (kein Scrollen/Wischen) schliesst die
 * Leiste kurz danach - genug Zeit, damit das Widget seinen PendingIntent
 * zuerst auslösen kann.
 *
 * EXPERIMENTELL: Ob ein fremdes (Sammel-)Widget im Overlay-Fenster
 * vollstaendig rendert, haengt vom Widget ab. Bei Problemen zeigt die Karte
 * die Fehlermeldung; das Log-Tag ist "EdgeTabWidget".
 */
public class WidgetTab extends BaseTab {

    WidgetTab(TabInstance inst) { super(inst, "Widget", R.drawable.ic_widget); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable tapClose = effectiveClose(closePanel);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(col);

        if (inst.widgets.isEmpty()) {
            TextView t = new TextView(ctx);
            t.setText("Hier lässt sich ein App-Widget einbetten – zum Beispiel der "
                    + "Posteingang des BlackBerry Hub. Es zeigt dann echte Inhalte, "
                    + "unabhängig von Benachrichtigungen. Mehrere Widgets lassen sich "
                    + "untereinander stapeln.\n\nExperimentell: nicht jedes Widget "
                    + "rendert in der Leiste.");
            t.setTextColor(Color.parseColor("#CCCCCC"));
            t.setTextSize(14);
            t.setPadding(0, 0, 0, 16 * d);
            col.addView(t);
        } else {
            WidgetHostHolder.startListening(ctx);
            AppWidgetHost host = WidgetHostHolder.host(ctx);
            AppWidgetManager mgr = WidgetHostHolder.mgr(ctx);
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            int wDp = Settings.panelWidth(ctx);
            int n = inst.widgets.size();
            int hDp = Math.max(160, ((int) (dm.heightPixels / dm.density) - 160) / Math.max(1, n));

            for (int i = 0; i < n; i++) {
                col.addView(widgetBlock(ctx, host, mgr, inst.widgets.get(i), i,
                        wDp, hDp, closePanel, tapClose, refreshContent, d));
            }
        }

        // Schwebendes Zahnrad statt Text-Knopf - Mathias' Wunsch nach
        // einheitlicherem Design; Zahnrad statt Stift, weil "Widget
        // hinzufügen" eher eine Einrichtungs- als eine Inhalts-Aktion ist.
        return withFab(ctx, scroll, fabGear(ctx, "#5A5A5E",
                new PickClick(ctx, closePanel, inst.id, -1)));
    }

    private View widgetBlock(Context ctx, AppWidgetHost host, AppWidgetManager mgr,
                              TabInstance.WidgetRef ref, int slot, int wDp, int hDp,
                              Runnable closePanel, Runnable tapClose, Runnable refreshContent, int d) {
        LinearLayout block = new LinearLayout(ctx);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, 0, 0, 10 * d);

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        TextView change = new TextView(ctx);
        change.setText("Anderes Widget wählen");
        change.setTextColor(Color.parseColor("#2E9BE6"));
        change.setTextSize(12);
        change.setOnClickListener(new PickClick(ctx, closePanel, inst.id, slot));
        head.addView(change, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView remove = new TextView(ctx);
        remove.setText("Entfernen ✕");
        remove.setTextColor(Color.parseColor("#8899AA"));
        remove.setTextSize(12);
        remove.setPadding(10 * d, 8 * d, 4 * d, 8 * d);
        remove.setOnClickListener(new RemoveClick(ctx, inst.id, slot, refreshContent));
        head.addView(remove);
        block.addView(head);

        AppWidgetProviderInfo info = mgr.getAppWidgetInfo(ref.id);
        WidgetFrame frame = new WidgetFrame(ctx, tapClose);
        frame.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, hDp * d));
        if (info == null) {
            TextView err = new TextView(ctx);
            err.setText("Widget nicht mehr verfügbar.");
            err.setTextColor(Color.parseColor("#FFB0B0"));
            frame.addView(err);
        } else {
            try {
                AppWidgetHostView view = host.createView(ctx.getApplicationContext(), ref.id, info);
                Bundle opts = new Bundle();
                opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, wDp);
                opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, wDp);
                opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, hDp);
                opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, hDp);
                try { view.updateAppWidgetSize(opts, wDp, hDp, wDp, hDp); } catch (Throwable ignored) {}
                try { mgr.updateAppWidgetOptions(ref.id, opts); } catch (Throwable ignored) {}
                view.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                frame.addView(view);
            } catch (Throwable t) {
                Log.w(WidgetHostHolder.TAG, "createView", t);
                TextView err = new TextView(ctx);
                err.setText("Widget konnte nicht dargestellt werden.\n" + t);
                err.setTextColor(Color.parseColor("#FFB0B0"));
                frame.addView(err);
            }
        }
        block.addView(frame);
        return block;
    }

    /**
     * Beobachtet Tipp-Gesten INNERHALB eines fremden Widgets, ohne dessen
     * eigene Klicks zu stoeren: dispatchTouchEvent wird fuer jede Beruehrung
     * aufgerufen, unabhaengig davon, welches Kind sie am Ende verarbeitet -
     * wir geben immer weiter, was super() ermittelt hat, und haengen nur eine
     * Beobachtung an. Bei einem kurzen Tippen (kaum Bewegung, kein
     * Scrollen/Wischen) und aktivem "Leiste schliessen"-Wunsch wird die
     * Leiste kurz danach geschlossen - genug Zeit fuer den PendingIntent des
     * Widgets, zuerst auszuloesen.
     */
    private static final class WidgetFrame extends FrameLayout {
        private final Runnable close;
        private final float slop;
        private float downX, downY;
        WidgetFrame(Context ctx, Runnable close) {
            super(ctx);
            this.close = close;
            this.slop = 16 * ctx.getResources().getDisplayMetrics().density;
        }
        @Override public boolean dispatchTouchEvent(MotionEvent ev) {
            boolean handled = super.dispatchTouchEvent(ev);
            int a = ev.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                downX = ev.getRawX(); downY = ev.getRawY();
            } else if (a == MotionEvent.ACTION_UP && close != null) {
                float dx = ev.getRawX() - downX, dy = ev.getRawY() - downY;
                if (Math.abs(dx) < slop && Math.abs(dy) < slop) postDelayed(close, 260);
            }
            return handled;
        }
    }

    /** Oeffnet den Widget-Auswahldialog (MainActivity) fuer eine bestimmte
     *  Karte/Stelle. slot = -1 haengt an, sonst ersetzt es dort. Benannt
     *  wegen d8. */
    static final class PickClick implements View.OnClickListener {
        private final Context ctx; private final Runnable closePanel;
        private final String tabId; private final int slot;
        PickClick(Context ctx, Runnable closePanel, String tabId, int slot) {
            this.ctx = ctx; this.closePanel = closePanel; this.tabId = tabId; this.slot = slot;
        }
        public void onClick(View v) {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("pick_widget", true);
            i.putExtra("tab_id", tabId);
            i.putExtra("slot", slot);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            if (closePanel != null) closePanel.run();
        }
    }

    /** Entfernt ein Widget aus der Karte (nur lokal, ohne Navigation - baut
     *  daher den Inhalt nur neu auf statt die Leiste zu schliessen). */
    static final class RemoveClick implements View.OnClickListener {
        private final Context ctx; private final String tabId; private final int slot;
        private final Runnable refresh;
        RemoveClick(Context ctx, String tabId, int slot, Runnable refresh) {
            this.ctx = ctx; this.tabId = tabId; this.slot = slot; this.refresh = refresh;
        }
        public void onClick(View v) {
            Tabs.update(ctx, tabId, t -> {
                if (slot >= 0 && slot < t.widgets.size()) t.widgets.remove(slot);
            });
            if (refresh != null) refresh.run();
        }
    }
}
