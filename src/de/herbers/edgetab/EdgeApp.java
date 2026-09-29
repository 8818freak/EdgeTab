package de.herbers.edgetab;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Process;

import de.herbers.common.DiagLog;
import de.herbers.common.Diagnostics;

/** App-weiter Absturz-Fangschirm. Zwei Dinge, die es vorher nicht gab:
 *  1. Die Fehlermeldung wird ins Diagnose-Protokoll gesichert
 *     ({@link de.herbers.common.DiagLog}), damit sie nach einem Absturz nicht
 *     verloren ist (vorher lag sie nur fluechtig im Systemprotokoll).
 *  2. Der Dienst wird automatisch neu gestartet. EdgeTab lief nach einem
 *     Absturz sonst gar nicht mehr, bis die App von Hand wieder geoeffnet
 *     wurde - fuer eine dauerhaft im Hintergrund laufende Randleiste
 *     unbrauchbar.
 *
 *  Der Neustart eines Vordergrunddienstes aus dem Hintergrund ist ab Android 12
 *  eigentlich eingeschraenkt, hier aber erlaubt: Apps mit der
 *  Overlay-Berechtigung (SYSTEM_ALERT_WINDOW - die EdgeTab fuer sein Panel
 *  ohnehin braucht) sind von dieser Sperre ausgenommen. */
public class EdgeApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Gemeinsame Diagnose: logcat-Tag + Absturz ins Diagnose-Protokoll
        // (dieselbe Bibliothek wie Sucher/ActiveFrames). Der Neustart des
        // Dienstes bleibt EdgeTab-spezifisch, daher eigener Handler statt des
        // generischen Diagnostics.installCrashLogger.
        DiagLog.setTag("EdgeTabDiag");
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            try {
                DiagLog.log(this, "ABSTURZ im Thread »" + (thread == null ? "?" : thread.getName())
                        + "«:\n" + Diagnostics.stackOf(ex));
            } catch (Throwable ignored) {}
            try { scheduleRestart(); } catch (Throwable ignored) {}
            // An den vorherigen Handler weitergeben (das System raeumt den
            // Prozess dann sauber ab und protokolliert den Absturz auch selbst);
            // fehlt einer, den Prozess selbst beenden.
            if (previous != null) {
                previous.uncaughtException(thread, ex);
            } else {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        });
    }

    private void scheduleRestart() {
        Intent i = new Intent(this, EdgeService.class);
        PendingIntent pi = PendingIntent.getForegroundService(
                this, 42, i, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) am.set(AlarmManager.RTC, System.currentTimeMillis() + 1500, pi);
    }
}
