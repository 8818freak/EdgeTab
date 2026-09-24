package de.herbers.edgetab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Startet den Leisten-Dienst nach dem Booten wieder. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (android.provider.Settings.canDrawOverlays(ctx)) {
                ctx.startForegroundService(new Intent(ctx, EdgeService.class));
            }
        }
    }
}
