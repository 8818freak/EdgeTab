package de.herbers.edgetab;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.util.Log;

/**
 * Haelt den AppWidgetHost, mit dem EdgeTab fremde App-Widgets (z.B. das
 * Posteingang-Widget des BlackBerry Hub) in einer eigenen Registerkarte
 * einbetten kann - ohne den Umweg ueber Benachrichtigungen.
 *
 * Ein normaler App darf Widgets einbetten, muss das Binden aber vom Nutzer
 * bestaetigen lassen (bindAppWidgetIdIfAllowed bzw. ACTION_APPWIDGET_BIND).
 * Das erledigt der Widget-Auswahldialog in MainActivity.
 */
final class WidgetHostHolder {

    static final String TAG = "EdgeTabWidget";
    static final int HOST_ID = 0x45544142; // "ETAB"

    private static AppWidgetHost host;
    private static boolean listening;

    private WidgetHostHolder() {}

    static synchronized AppWidgetHost host(Context c) {
        if (host == null) host = new AppWidgetHost(c.getApplicationContext(), HOST_ID);
        return host;
    }

    static AppWidgetManager mgr(Context c) {
        return AppWidgetManager.getInstance(c.getApplicationContext());
    }

    static synchronized void startListening(Context c) {
        try { host(c).startListening(); listening = true; }
        catch (Throwable t) { Log.w(TAG, "startListening", t); }
    }

    static synchronized void stopListening(Context c) {
        if (!listening) return;
        try { host(c).stopListening(); } catch (Throwable t) { Log.w(TAG, "stopListening", t); }
        listening = false;
    }
}
