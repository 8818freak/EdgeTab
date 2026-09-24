package de.herbers.edgetab;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Zeigt laufende Medienwiedergaben (Titel, Interpret, Cover) mit Steuerung
 * (Zurueck/Play-Pause/Vor) - ueber MediaSessionManager, mit dem eigenen
 * NotificationListenerService (NotificationCollector) als Ausweis. Keine
 * Sonderberechtigung noetig: ein aktiver Notification-Listener darf das,
 * ganz ohne "android.permission.MEDIA_CONTENT_CONTROL" (die ist
 * signaturgeschuetzt und fuer normale Apps nicht erreichbar).
 *
 * Baut den Inhalt bei jedem Panel-Aufbau frisch aus dem aktuellen Zustand -
 * kein Live-Ticken (wie der Rest der App). Nach einem Steuerbefehl wird kurz
 * gewartet und der Inhalt neu aufgebaut, damit Play/Pause/Titel sich sichtbar
 * aktualisieren.
 */
public class MediaTab extends BaseTab {

    MediaTab(TabInstance inst) { super(inst, "Medien", R.drawable.ic_media); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        List<MediaController> sessions = activeSessions(ctx);
        if (sessions == null) {
            TextView hint = new TextView(ctx);
            hint.setText("Kein Zugriff auf Medien-Sitzungen. Benachrichtigungszugriff "
                    + "in den Android-Einstellungen prüfen.");
            hint.setTextColor(Color.parseColor("#FFB0B0"));
            hint.setTextSize(14);
            root.addView(hint);
            return scroll;
        }
        if (sessions.isEmpty()) {
            TextView hint = new TextView(ctx);
            hint.setText("Gerade wird nichts abgespielt.");
            hint.setTextColor(Color.parseColor("#9E9E9E"));
            hint.setTextSize(14);
            root.addView(hint);
            return scroll;
        }

        List<MediaController> deduped = dedupe(sessions);
        MediaController primary = pickPrimary(deduped);
        String pos = Settings.mediaControlsPos(ctx);
        // Platz lassen, damit die feste Karte keinen andern Eintrag dauerhaft
        // verdeckt - Hoehe grosszuegig wie eine ganze Karte bemessen.
        int barSpace = 190 * d;
        if ("bottom".equals(pos)) root.setPadding(0, 0, 0, barSpace);
        else if ("top".equals(pos)) root.setPadding(0, barSpace, 0, 0);

        for (MediaController mc : deduped) {
            // Die "wichtigste" Sitzung wird NICHT hier in der scrollenden
            // Liste gezeigt, sondern einmal, fest positioniert, weiter unten -
            // sonst erscheint sie zweimal (Mathias' Fund: Karte oben blieb
            // stehen, nur eine kleine Extra-Leiste bewegte sich - wirkte wie
            // doppelt UND wie "die Einstellung tut nichts").
            if (mc == primary) continue;
            root.addView(sessionCard(ctx, mc, refreshContent, d, true));
        }

        FrameLayout frame = new FrameLayout(ctx);
        frame.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        if (primary != null) {
            View card = sessionCard(ctx, primary, refreshContent, d, true);
            FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            barLp.gravity = "top".equals(pos) ? Gravity.TOP
                    : "middle".equals(pos) ? Gravity.CENTER : Gravity.BOTTOM;
            barLp.leftMargin = 8 * d; barLp.rightMargin = 8 * d;
            barLp.topMargin = 8 * d; barLp.bottomMargin = 8 * d;
            frame.addView(card, barLp);
        }
        return frame;
    }

    /** Welche Sitzung die feste Schnellsteuerung zeigt - spielend geht vor
     *  pausiert-mit-Zustand geht vor allem anderen (wie score()). */
    private MediaController pickPrimary(List<MediaController> sessions) {
        MediaController best = null;
        int bestScore = -1;
        for (MediaController mc : sessions) {
            int s = score(mc);
            if (best == null || s > bestScore) { best = mc; bestScore = s; }
        }
        return best;
    }

    /** Manche Apps (z. B. Vorlese-/Player-Apps mit einer zweiten, leeren
     *  Zombie-Sitzung) melden mehrere Sitzungen fuer dieselbe App gleichzeitig
     *  an - Mathias hat das mit AIReaderX beobachtet (eine echte Sitzung mit
     *  Titel/Cover, eine zweite ohne Titel und ohne funktionierende Steuerung).
     *  Pro App wird nur die "beste" Sitzung behalten. */
    private List<MediaController> dedupe(List<MediaController> sessions) {
        LinkedHashMap<String, MediaController> best = new LinkedHashMap<>();
        for (MediaController mc : sessions) {
            String pkg = mc.getPackageName();
            MediaController cur = best.get(pkg);
            if (cur == null || score(mc) > score(cur)) best.put(pkg, mc);
        }
        return new ArrayList<>(best.values());
    }

    /** Hoeher = eher anzeigen: aktiv spielend > pausiert mit Zustand > hat
     *  wenigstens einen Titel > nackte, zustandslose Sitzung. */
    private int score(MediaController mc) {
        int s = 0;
        try {
            PlaybackState st = mc.getPlaybackState();
            if (st != null) {
                int state = st.getState();
                if (state == PlaybackState.STATE_PLAYING) s += 100;
                else if (state != PlaybackState.STATE_NONE && state != PlaybackState.STATE_STOPPED) s += 10;
            }
        } catch (Throwable ignored) {}
        try {
            MediaMetadata meta = mc.getMetadata();
            String title = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_TITLE) : null;
            if (title != null && !title.isEmpty()) s += 5;
        } catch (Throwable ignored) {}
        return s;
    }

    private List<MediaController> activeSessions(Context ctx) {
        try {
            MediaSessionManager mgr =
                    (MediaSessionManager) ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
            return mgr.getActiveSessions(new ComponentName(ctx, NotificationCollector.class));
        } catch (Throwable t) {
            return null;
        }
    }

    private View sessionCard(Context ctx, MediaController mc, Runnable refresh, int d, boolean showControls) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2C2C2E"));
        bg.setCornerRadius(12 * d);
        card.setBackground(bg);
        card.setPadding(12 * d, 12 * d, 12 * d, 10 * d);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = 10 * d;
        card.setLayoutParams(cardLp);

        MediaMetadata meta = null;
        try { meta = mc.getMetadata(); } catch (Throwable ignored) {}
        String title = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_TITLE) : null;
        String artist = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_ARTIST) : null;
        Bitmap art = null;
        if (meta != null) {
            art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (art == null) art = meta.getBitmap(MediaMetadata.METADATA_KEY_ART);
        }

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        ImageView cover = new ImageView(ctx);
        int s = 72 * d;
        LinearLayout.LayoutParams coverLp = new LinearLayout.LayoutParams(s, s);
        coverLp.rightMargin = 12 * d;
        cover.setLayoutParams(coverLp);
        if (art != null) {
            cover.setImageBitmap(art);
        } else {
            cover.setImageResource(R.drawable.ic_media);
            cover.setColorFilter(Color.parseColor("#B0B0B0"));
            cover.setPadding(10 * d, 10 * d, 10 * d, 10 * d);
        }
        head.addView(cover);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView appLabel = new TextView(ctx);
        appLabel.setText(appLabel(ctx, mc.getPackageName()));
        appLabel.setTextColor(Color.parseColor("#2E9BE6"));
        appLabel.setTextSize(12.5f);
        textCol.addView(appLabel);

        TextView t = new TextView(ctx);
        t.setText(title == null || title.isEmpty() ? "(ohne Titel)" : title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(19);
        t.setMaxLines(2);
        textCol.addView(t);

        if (artist != null && !artist.isEmpty()) {
            TextView a = new TextView(ctx);
            a.setText(artist);
            a.setTextColor(Color.parseColor("#9A9A9A"));
            a.setTextSize(15);
            textCol.addView(a);
        }
        head.addView(textCol);
        card.addView(head);

        if (showControls) {
            boolean playing = isPlaying(mc);
            LinearLayout controls = new LinearLayout(ctx);
            controls.setOrientation(LinearLayout.HORIZONTAL);
            controls.setGravity(Gravity.CENTER);
            controls.setPadding(0, 10 * d, 0, 0);

            controls.addView(controlButton(ctx,
                    "⏮", new TransportClick(mc, TransportClick.PREV, refresh)));
            controls.addView(controlButton(ctx,
                    playing ? "⏸" : "▶", new TransportClick(mc, TransportClick.PLAYPAUSE, refresh)));
            controls.addView(controlButton(ctx,
                    "⏭", new TransportClick(mc, TransportClick.NEXT, refresh)));
            card.addView(controls);
        }

        return card;
    }

    private boolean isPlaying(MediaController mc) {
        try {
            PlaybackState st = mc.getPlaybackState();
            return st != null && st.getState() == PlaybackState.STATE_PLAYING;
        } catch (Throwable t) { return false; }
    }

    private TextView controlButton(Context ctx, String symbol, View.OnClickListener onClick) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        TextView b = new TextView(ctx);
        b.setText(symbol);
        b.setTextColor(Color.WHITE);
        b.setTextSize(32);
        b.setPadding(22 * d, 12 * d, 22 * d, 12 * d);
        b.setOnClickListener(onClick);
        return b;
    }

    private String appLabel(Context ctx, String pkg) {
        try {
            android.content.pm.PackageManager pm = ctx.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) { return pkg; }
    }

    /** Sendet einen Steuerbefehl und baut den Inhalt kurz danach neu auf,
     *  damit Play/Pause/Titel sich sichtbar aktualisieren. Benannt statt
     *  anonym (d8). */
    static final class TransportClick implements View.OnClickListener {
        static final int PREV = 0, PLAYPAUSE = 1, NEXT = 2;
        private final MediaController mc; private final int action; private final Runnable refresh;
        TransportClick(MediaController mc, int action, Runnable refresh) {
            this.mc = mc; this.action = action; this.refresh = refresh;
        }
        public void onClick(View v) {
            try {
                MediaController.TransportControls tc = mc.getTransportControls();
                if (action == PREV) {
                    tc.skipToPrevious();
                } else if (action == NEXT) {
                    tc.skipToNext();
                } else {
                    PlaybackState st = mc.getPlaybackState();
                    boolean playing = st != null && st.getState() == PlaybackState.STATE_PLAYING;
                    if (playing) tc.pause(); else tc.play();
                }
            } catch (Throwable ignored) {}
            if (refresh != null) {
                new Handler(Looper.getMainLooper()).postDelayed(refresh, 350);
            }
        }
    }
}
