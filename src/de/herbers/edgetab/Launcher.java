package de.herbers.edgetab;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Oeffnet das Ziel einer Benachrichtigung aus dem Panel heraus.
 *
 * Zwei Faelle:
 *  - contentIntent ist eine Activity (Telegram, Signal, BBMe): direkt senden,
 *    Hintergrundstart ausdruecklich erlaubt.
 *  - contentIntent ist ein Broadcast/Service-"Trampolin" (BlackBerry Hub): der
 *    Empfaenger der App will danach selbst eine Activity starten - aus dem
 *    Hintergrund blockiert Android 14 das. Darum zuerst die App selbst in den
 *    Vordergrund holen (das darf EdgeTab), kurz warten, dann das Trampolin
 *    ausloesen. Die App ist dann sichtbar und darf die Mail oeffnen.
 */
final class Launcher {

    private static final String TAG = "EdgeTabLauncher";
    private static final long TRAMPOLINE_DELAY_MS = 450;

    private Launcher() {}

    static Bundle bgAllowed() {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                ActivityOptions ao = ActivityOptions.makeBasic();
                ao.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                return ao.toBundle();
            } catch (Throwable ignored) {}
        }
        return null;
    }

    static void open(Context ctx, NotificationStore.Item it) {
        Context app = ctx.getApplicationContext();
        Bundle opts = bgAllowed();
        String pkg = it.pkg;
        PendingIntent pi = NotificationCollector.intentFor(it);

        if (pi == null) {
            Log.d(TAG, "kein contentIntent, oeffne App " + pkg);
            launchApp(app, pkg, opts);
            return;
        }

        boolean isActivity = Build.VERSION.SDK_INT < 31 || pi.isActivity();
        if (isActivity) {
            if (!send(app, pi, opts)) launchApp(app, pkg, opts);
            return;
        }

        // Trampolin: App vorholen, dann ausloesen.
        Log.d(TAG, "Trampolin-Intent bei " + pkg + " - App zuerst in den Vordergrund");
        launchApp(app, pkg, opts);
        new Handler(Looper.getMainLooper()).postDelayed(
                new DelayedSend(app, pi, opts), TRAMPOLINE_DELAY_MS);
    }

    static boolean send(Context ctx, PendingIntent pi, Bundle opts) {
        return send(ctx, pi, null, opts);
    }

    /** Wie send(), aber mit einem "Fill-in"-Intent - noetig fuer RemoteInput-
     *  Antworten, deren Text ueber genau so ein Intent mitgegeben wird. */
    static boolean send(Context ctx, PendingIntent pi, Intent fillIn, Bundle opts) {
        try {
            if (fillIn != null || opts != null) pi.send(ctx, 0, fillIn, null, null, null, opts);
            else pi.send();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "send fehlgeschlagen", e);
            return false;
        }
    }

    static void launchApp(Context ctx, String pkg, Bundle opts) {
        try {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) return;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (opts != null) ctx.startActivity(i, opts); else ctx.startActivity(i);
        } catch (Exception e) {
            Log.w(TAG, "App-Start fehlgeschlagen: " + pkg, e);
        }
    }

    /** Benannte Klasse statt anonymer (d8-Fallstrick). */
    static final class DelayedSend implements Runnable {
        private final Context ctx; private final PendingIntent pi; private final Bundle opts;
        DelayedSend(Context ctx, PendingIntent pi, Bundle opts) {
            this.ctx = ctx; this.pi = pi; this.opts = opts;
        }
        public void run() { send(ctx, pi, opts); }
    }
}
