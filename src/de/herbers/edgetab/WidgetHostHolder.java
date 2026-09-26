package de.herbers.edgetab;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;

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

    // AppWidgetHostView je Widget-ID wiederverwenden statt bei jedem
    // Panel-Neuaufbau (Tab-Wechsel, Drehung) neu zu erzeugen: fuer ein
    // "Sammel"-Widget (RemoteViewsFactory, z.B. BlackBerry Hubs
    // Posteingang-Liste) loest jede neue View einen frischen Verbindungs-
    // aufbau zu dessen RemoteViewsService aus, der die komplette Liste inkl.
    // aller Bilder neu ueber Binder ueberträgt - genau der Codepfad, in dem
    // BB Hub bei Mathias abstuerzte ("Could not write bitmap blob file
    // descriptor"). Eine echte Launcher-AppWidgetHostView ist ohnehin dafuer
    // gedacht, langlebig zu sein und nur per updateAppWidget() aktualisiert
    // zu werden, nicht bei jedem UI-Neuaufbau neu erstellt.
    private static final java.util.Map<Integer, AppWidgetHostView> viewCache = new java.util.HashMap<>();

    static synchronized AppWidgetHostView viewFor(Context ctx, AppWidgetHost host, int appWidgetId,
                                                   AppWidgetProviderInfo info) {
        AppWidgetHostView v = viewCache.get(appWidgetId);
        if (v == null) {
            v = host.createView(ctx.getApplicationContext(), appWidgetId, info);
            viewCache.put(appWidgetId, v);
        } else {
            ViewParent p = v.getParent();
            if (p instanceof ViewGroup) ((ViewGroup) p).removeView(v);
        }
        return v;
    }

    /** Beim expliziten Entfernen eines Widgets aus einer Karte aufrufen, damit
     *  der Cache nicht auf einer nicht mehr angezeigten View sitzen bleibt. */
    static synchronized void forget(int appWidgetId) {
        viewCache.remove(appWidgetId);
    }
}
