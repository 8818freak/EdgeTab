package de.herbers.edgetab;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * "Geloescht - Wiederherstellen" oben im Kopfbereich des Panels.
 *
 * Das X loescht NICHT sofort: der Eintrag verschwindet nur aus der Liste, und
 * erst nach Ablauf der Frist (oder beim Schliessen des Panels bzw. beim
 * naechsten Loeschen) wird wirklich geloescht - in der Quell-App, in der
 * Statusleiste und in der Ablage. "Wiederherstellen" bricht das ab.
 */
final class UndoBar {

    private static final long TIMEOUT_MS = 5000;
    private static final Handler H = new Handler(Looper.getMainLooper());

    private static View bar;          // aktuelle Leiste im offenen Panel
    private static TextView label;
    private static Pending pending;   // hoechstens ein offener Loeschvorgang
    private static Runnable panelClose; // schliesst das Panel (fuer Hub-Dialog)

    private UndoBar() {}

    /** Leiste bauen und als Ueberlagerung des Kopfbereichs zurueckgeben. */
    static View build(Context ctx, float fs, Runnable panelCloser) {
        panelClose = panelCloser;
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(14 * d, 10 * d, 14 * d, 10 * d);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F0303034"));
        bg.setCornerRadius(12 * d);
        row.setBackground(bg);
        row.setClickable(true); // Beruehrung nicht an den Kopf durchreichen

        TextView t = new TextView(ctx);
        t.setTextColor(Color.parseColor("#E0E0E0"));
        t.setTextSize(13 * fs);
        t.setMaxLines(2);
        t.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(t);

        TextView undo = new TextView(ctx);
        undo.setText(ctx.getString(R.string.undo_action));
        undo.setTextColor(Color.parseColor("#2E9BE6"));
        undo.setTypeface(null, Typeface.BOLD);
        undo.setTextSize(13 * fs);
        undo.setPadding(12 * d, 6 * d, 0, 6 * d);
        undo.setOnClickListener(new UndoClick());
        row.addView(undo);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL);
        row.setLayoutParams(lp);
        row.setVisibility(View.GONE);
        bar = row; label = t;
        return row;
    }

    /** X gedrueckt: Zeile ausblenden, Loeschen vormerken, Leiste zeigen. */
    static void schedule(Context ctx, NotificationStore.Item it, View row) {
        commitNow(); // vorherigen offenen Vorgang abschliessen
        Pending p = new Pending(ctx.getApplicationContext(), it, row);
        pending = p;
        row.setVisibility(View.GONE);
        H.postDelayed(p, TIMEOUT_MS);
        if (bar != null) {
            String text = (it.title == null || it.title.isEmpty())
                    ? ctx.getString(R.string.undo_deleted_generic)
                    : ctx.getString(R.string.undo_deleted_named, it.title);
            label.setText(text);
            bar.animate().cancel();
            bar.setAlpha(0f);
            bar.setVisibility(View.VISIBLE);
            bar.animate().alpha(1f).setDuration(150).start();
        }
    }

    /** Offenen Vorgang sofort ausfuehren (Panel schliesst, naechstes X ...). */
    static void commitNow() {
        Pending p = pending;
        if (p == null) return;
        H.removeCallbacks(p);
        p.run();
    }

    /** Panel wird abgebaut: offenen Vorgang ausfuehren, Leiste vergessen. */
    static void detach() {
        commitNow();
        bar = null; label = null; panelClose = null;
    }

    private static void hideBar() {
        View b = bar;
        if (b == null) return;
        b.animate().cancel();
        b.animate().alpha(0f).setDuration(250).withEndAction(new Hide(b)).start();
    }

    static final class Hide implements Runnable {
        private final View v;
        Hide(View v) { this.v = v; }
        public void run() { v.setVisibility(View.GONE); }
    }

    static final class UndoClick implements View.OnClickListener {
        public void onClick(View v) {
            Pending p = pending;
            if (p == null) return;
            H.removeCallbacks(p);
            pending = null;
            if (p.row != null) p.row.setVisibility(View.VISIBLE);
            hideBar();
        }
    }

    /** Der eigentliche Loeschvorgang. */
    static final class Pending implements Runnable {
        final Context ctx; final NotificationStore.Item it; final View row;
        Pending(Context ctx, NotificationStore.Item it, View row) {
            this.ctx = ctx; this.it = it; this.row = row;
        }
        public void run() {
            if (pending != this) return;
            pending = null;
            // Loescht in der Quell-App; bei Aktionen mit Dialog (Hub) wird dazu
            // erst das Panel geschlossen und die App nach vorne geholt.
            boolean deleted = NotificationCollector.deleteInApp(ctx, it, panelClose);
            if (!deleted) NotificationCollector.dismiss(it);
            NotificationStore.get(ctx).delete(it.id);
            hideBar();
        }
    }
}
