package de.herbers.edgetab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Startet den Leisten-Dienst nach dem Booten wieder - und ebenso nach einem
 *  App-Update: MY_PACKAGE_REPLACED trifft genau EdgeTab selbst und wird
 *  garantiert zugestellt, auch wenn EdgeTab gerade nicht laeuft. Noetig, weil
 *  der alte Dienst-Prozess ein Update sonst manchmal ueberlebt (statt neu zu
 *  starten) und dann mit altem Code weiterlaeuft, bis man "Erzwungen
 *  stoppen" antippt - fuer Mathias' Testablauf per APK-Datei aus dem Chat
 *  (statt "adb install -r") ist genau das aufgefallen. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            if (android.provider.Settings.canDrawOverlays(ctx)) {
                ctx.startForegroundService(new Intent(ctx, EdgeService.class));
            }
        }
    }
}
