package de.herbers.edgetab;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Faengt eintreffende System-Benachrichtigungen ab und legt sie im
 * NotificationStore ab (mit Gelesen-Status), damit die Posteingang-Karte sie
 * zeigen kann - auch nachdem Android sie aus der Statusleiste entfernt.
 *
 * Zusaetzlich merkt sich der Collector zu jeder noch aktiven Benachrichtigung
 * ihren contentIntent (das, was beim Antippen im System passiert - z.B. die
 * konkrete Mail oeffnen) und erlaubt der Karte, eine Benachrichtigung aus der
 * Statusleiste zu loeschen.
 */
public class NotificationCollector extends NotificationListenerService {

    private static final String TAG = "EdgeTabCollector";

    // Lebende Verbindung zum Dienst, damit die Karte loeschen kann.
    private static volatile NotificationCollector instance;
    // Schluessel -> contentIntent der noch aktiven Benachrichtigung.
    private static final ConcurrentHashMap<String, PendingIntent> intents = new ConcurrentHashMap<>();
    // Schluessel -> "Loeschen"-Aktion der Benachrichtigung (loescht in der App selbst).
    private static final ConcurrentHashMap<String, PendingIntent> deletes = new ConcurrentHashMap<>();
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    /** Benannte Klasse (anonyme lassen d8 stolpern). */
    static final class DelayedDismiss implements Runnable {
        private final NotificationStore.Item it;
        DelayedDismiss(NotificationStore.Item it) { this.it = it; }
        public void run() { dismiss(it); }
    }

    /**
     * Erst nach kurzer Frist wegwischen. Noetig, weil ein sofortiges
     * cancelNotification dem Hub in die Loeschung faellt: er postet die Mail
     * dann neu, statt sie zu loeschen.
     */
    public static void dismissDelayed(NotificationStore.Item it, long ms) {
        HANDLER.postDelayed(new DelayedDismiss(it), ms);
    }

    /** Aktive Benachrichtigung zum Schluessel frisch vom System holen (oder null). */
    private static StatusBarNotification active(String key) {
        NotificationCollector inst = instance;
        if (inst == null || key == null) return null;
        try {
            StatusBarNotification[] a = inst.getActiveNotifications(new String[]{key});
            if (a != null) for (StatusBarNotification sbn : a)
                if (sbn != null && key.equals(sbn.getKey())) return sbn;
        } catch (Exception e) { Log.w(TAG, "active", e); }
        return null;
    }

    /** Titel und Text so auslesen, wie sie auch in der Ablage landen. */
    static String[] extract(Notification n) {
        Bundle ex = n == null ? null : n.extras;
        CharSequence title = ex == null ? null : ex.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text  = ex == null ? null : ex.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence big   = ex == null ? null : ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (big != null && (text == null || big.length() > text.length())) text = big;
        return new String[]{ title == null ? "" : title.toString(), text == null ? "" : text.toString() };
    }

    /** Inhaltliche Kennung: gleiche App + Titel + Text = dieselbe Nachricht. */
    static String signature(String pkg, String title, String text) {
        return pkg + "\u0001" + (title == null ? "" : title) + "\u0001" + (text == null ? "" : text);
    }

    /**
     * Die aktive Benachrichtigung zu einem Eintrag: erst ueber den Schluessel,
     * sonst ueber den Inhalt. Noetig, weil z.B. der BlackBerry Hub dieselbe Mail
     * mehrfach neu postet und dabei den Schluessel wechseln kann.
     */
    static StatusBarNotification resolve(NotificationStore.Item it) {
        if (it == null) return null;
        StatusBarNotification byKey = active(it.nkey);
        if (byKey != null) return byKey;
        NotificationCollector inst = instance;
        if (inst == null) return null;
        String want = signature(it.pkg, it.title, it.text);
        StatusBarNotification best = null;
        try {
            StatusBarNotification[] a = inst.getActiveNotifications();
            if (a != null) for (StatusBarNotification sbn : a) {
                if (sbn == null || !it.pkg.equals(sbn.getPackageName())) continue;
                Notification n = sbn.getNotification();
                if (n == null || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) continue;
                String[] tt = extract(n);
                if (!want.equals(signature(sbn.getPackageName(), tt[0], tt[1]))) continue;
                if (best == null || sbn.getPostTime() > best.getPostTime()) best = sbn;
            }
        } catch (Exception e) { Log.w(TAG, "resolve", e); }
        Log.d(TAG, "resolve " + it.nkey + " -> " + (best == null ? "nichts aktiv" : best.getKey()));
        return best;
    }

    /** contentIntent zu einem Eintrag (frisch vom System, sonst gemerkt). */
    public static PendingIntent intentFor(NotificationStore.Item it) {
        StatusBarNotification sbn = resolve(it);
        if (sbn != null && sbn.getNotification() != null
                && sbn.getNotification().contentIntent != null) {
            return sbn.getNotification().contentIntent;
        }
        return it == null ? null : intentFor(it.nkey);
    }

    /** Schluessel UND Inhalts-Kennungen aller aktiven Benachrichtigungen. */
    public static java.util.Set<String> liveIndex() {
        java.util.HashSet<String> out = new java.util.HashSet<>();
        NotificationCollector inst = instance;
        if (inst == null) return out;
        try {
            StatusBarNotification[] a = inst.getActiveNotifications();
            if (a != null) for (StatusBarNotification sbn : a) {
                if (sbn == null) continue;
                out.add(sbn.getKey());
                String[] tt = extract(sbn.getNotification());
                out.add(signature(sbn.getPackageName(), tt[0], tt[1]));
            }
        } catch (Exception e) { Log.w(TAG, "liveIndex", e); }
        return out;
    }

    /** Schluessel aller Benachrichtigungen, die gerade noch in der Statusleiste stehen. */
    public static java.util.Set<String> activeKeys() {
        java.util.HashSet<String> keys = new java.util.HashSet<>();
        NotificationCollector inst = instance;
        if (inst == null) return keys;
        try {
            StatusBarNotification[] a = inst.getActiveNotifications();
            if (a != null) for (StatusBarNotification sbn : a) if (sbn != null) keys.add(sbn.getKey());
        } catch (Exception e) { Log.w(TAG, "activeKeys", e); }
        return keys;
    }

    /** Aktueller contentIntent - bevorzugt frisch vom System (Apps erneuern ihn oft). */
    public static PendingIntent currentIntentFor(String key) {
        StatusBarNotification sbn = active(key);
        if (sbn != null && sbn.getNotification() != null
                && sbn.getNotification().contentIntent != null) {
            return sbn.getNotification().contentIntent;
        }
        return intentFor(key);
    }

    /** Sucht in einer Benachrichtigung eine "Antworten"-Aktion mit freier
     *  Texteingabe (RemoteInput) - genau das, was Messaging-Apps (Telegram,
     *  Signal/Molly, WhatsApp...) fuer die Direktantwort aus der System-
     *  Benachrichtigung heraus mitliefern. Kein Sonderrecht noetig - jeder
     *  aktive NotificationListenerService darf das auslesen und ausloesen,
     *  genau wie beim Loeschen. */
    static Notification.Action findReplyAction(Notification n) {
        if (n == null || n.actions == null) return null;
        for (Notification.Action a : n.actions) {
            if (a == null || a.actionIntent == null || a.getRemoteInputs() == null) continue;
            for (RemoteInput ri : a.getRemoteInputs()) {
                if (ri != null && ri.getAllowFreeFormInput()) return a;
            }
        }
        return null;
    }

    /** Frische Antworten-Aktion zu einem Eintrag holen (wie resolve() bei
     *  Loeschen/Oeffnen - PendingIntents auf alten Benachrichtigungen werden
     *  von manchen Apps schnell ungueltig). */
    static Notification.Action resolveReplyAction(NotificationStore.Item it) {
        StatusBarNotification sbn = resolve(it);
        return sbn == null ? null : findReplyAction(sbn.getNotification());
    }

    /** Wie findReplyAction(), aber auch fuer "Antworten"-Aktionen OHNE
     *  RemoteInput - z.B. BlackBerry Hubs "Allen antworten", das laut
     *  `dumpsys notification` eine reine startActivity-Aktion ist (oeffnet
     *  Hubs eigenen Antwortbildschirm, keine Direkteingabe aus der
     *  Benachrichtigung heraus). Erkennung wie bei findDeleteAction: erst
     *  semantisch, dann per Beschriftung. */
    static Notification.Action findAnyReplyAction(Notification n) {
        Notification.Action withInput = findReplyAction(n);
        if (withInput != null) return withInput;
        if (n == null || n.actions == null) return null;
        for (Notification.Action a : n.actions) {
            if (a == null || a.actionIntent == null
                    || a.getSemanticAction() != Notification.Action.SEMANTIC_ACTION_REPLY) continue;
            return a;
        }
        for (Notification.Action a : n.actions) {
            if (a == null || a.actionIntent == null || a.title == null) continue;
            String t = a.title.toString().toLowerCase(java.util.Locale.ROOT);
            if (t.contains("antwort") || t.contains("reply")) return a;
        }
        return null;
    }

    static Notification.Action resolveAnyReplyAction(NotificationStore.Item it) {
        StatusBarNotification sbn = resolve(it);
        return sbn == null ? null : findAnyReplyAction(sbn.getNotification());
    }

    /** Loest eine Antworten-Aktion OHNE RemoteInput direkt aus (oeffnet die
     *  Antwort-Activity der Quell-App, wie ein Tipp auf die Aktion in der
     *  System-Benachrichtigung selbst) - sofort senden, kein Vorab-
     *  Foregrounding (dieselbe Lehre wie beim Loeschen). */
    public static boolean openReplyAction(android.content.Context ctx, NotificationStore.Item it,
                                           Runnable panelClose) {
        Notification.Action action = resolveAnyReplyAction(it);
        if (action == null) return false;
        if (panelClose != null) panelClose.run();
        boolean ok = Launcher.send(ctx, action.actionIntent, Launcher.bgAllowed());
        Log.d(TAG, "Antworten-Activity " + (ok ? "gestartet" : "FEHLGESCHLAGEN") + ": " + it.pkg);
        return ok;
    }

    /** Schickt eine Direktantwort ueber die RemoteInput-Aktion der
     *  Benachrichtigung - wie das Antwortfeld in der System-Benachrichtigung
     *  selbst. Kein Vorab-Foregrounding (dieselbe Lehre wie beim Loeschen:
     *  sofort senden, sonst posten manche Apps neu und der PendingIntent
     *  verfaellt). */
    public static boolean sendReply(android.content.Context ctx, NotificationStore.Item it, String text) {
        Notification.Action action = resolveReplyAction(it);
        if (action == null) { Log.d(TAG, "keine Antworten-Aktion fuer " + it.nkey); return false; }
        Bundle results = new Bundle();
        for (RemoteInput ri : action.getRemoteInputs()) {
            if (ri != null && ri.getAllowFreeFormInput()) results.putCharSequence(ri.getResultKey(), text);
        }
        Intent fillIn = new Intent();
        RemoteInput.addResultsToIntent(action.getRemoteInputs(), fillIn, results);
        boolean ok = Launcher.send(ctx, action.actionIntent, fillIn, Launcher.bgAllowed());
        Log.d(TAG, "Antwort " + (ok ? "gesendet" : "FEHLGESCHLAGEN") + ": " + it.pkg);
        return ok;
    }

    /** Sucht in einer Benachrichtigung die Loeschen-Aktion (auch Wearable-Aktionen). */
    static PendingIntent findDeleteAction(Notification n) {
        if (n == null) return null;
        java.util.ArrayList<Notification.Action> all = new java.util.ArrayList<>();
        if (n.actions != null) java.util.Collections.addAll(all, n.actions);
        try { all.addAll(new Notification.WearableExtender(n).getActions()); }
        catch (Exception ignored) {}
        // 1. Semantisch als Loeschen markiert
        for (Notification.Action a : all) {
            if (a != null && a.actionIntent != null
                    && a.getSemanticAction() == Notification.Action.SEMANTIC_ACTION_DELETE) {
                return a.actionIntent;
            }
        }
        // 2. Nach Beschriftung
        for (Notification.Action a : all) {
            if (a == null || a.actionIntent == null || a.title == null) continue;
            String t = a.title.toString().toLowerCase(java.util.Locale.ROOT);
            if (t.contains("lösch") || t.contains("losch") || t.contains("delete")
                    || t.contains("papierkorb") || t.contains("trash")) {
                return a.actionIntent;
            }
        }
        return null;
    }

    /**
     * Loescht die Nachricht in der Quell-App, indem die "Loeschen"-Aktion der
     * Benachrichtigung ausgeloest wird - wie der Knopf in der Systembenachrichtigung.
     *
     * Frueher wurde bei einer Activity-Aktion (Bestaetigungsdialog, z.B. Hub)
     * erst die Quell-App per Launcher.fireAfterForeground in den Vordergrund
     * geholt und der PendingIntent 450ms spaeter ausgeloest. Log-Analyse am
     * 2026-09-24 (adb logcat waehrend eines EdgeTab- und eines direkten
     * System-Benachrichtigungs-Versuchs) zeigte: genau dieses Vorab-Hochholen
     * bringt den BlackBerry Hub dazu, dieselbe Benachrichtigung binnen
     * Sekundenbruchteilen mehrfach neu zu posten - der zuvor gemerkte
     * PendingIntent gehoert dann zu einer bereits ueberholten Benachrichtigung
     * und verpufft, ohne dass die Ziel-Activity (DeleteIntentActivity) je
     * startet (kein Log-Eintrag von ihr). Der direkte Tipp auf "Loeschen" in
     * der System-Benachrichtigung holt die App NICHT vorher hoch, sondern
     * sendet sofort - und funktioniert deshalb zuverlaessig. Das ahmen wir
     * jetzt nach: Panel schliessen, PendingIntent SOFORT mit
     * Hintergrund-Start-Erlaubnis senden, kein Vorab-Foregrounding, keine
     * Verzoegerung.
     * @return true, wenn eine Loeschen-Aktion gefunden und gesendet wurde.
     */
    public static boolean deleteInApp(android.content.Context ctx, NotificationStore.Item it,
                                      Runnable panelClose) {
        if (it == null) return false;
        PendingIntent pi = null;
        StatusBarNotification sbn = resolve(it);
        if (sbn != null) pi = findDeleteAction(sbn.getNotification());
        if (pi == null && it.nkey != null) pi = deletes.get(it.nkey);
        if (pi == null) { Log.d(TAG, "keine Loeschen-Aktion fuer " + it.nkey); return false; }

        if (android.os.Build.VERSION.SDK_INT >= 31) {
            Log.d(TAG, "Loeschen-PI: activity=" + pi.isActivity()
                    + " broadcast=" + pi.isBroadcast() + " service=" + pi.isService());
        }
        if (sbn != null) deletes.remove(sbn.getKey());
        if (it.nkey != null) deletes.remove(it.nkey);

        if (panelClose != null) panelClose.run();
        boolean ok = Launcher.send(ctx, pi, Launcher.bgAllowed());
        Log.d(TAG, "Loeschen-Aktion " + (ok ? "gesendet" : "FEHLGESCHLAGEN") + ": " + it.pkg);
        boolean activity = android.os.Build.VERSION.SDK_INT < 31 || pi.isActivity();
        if (ok && !activity) dismissDelayed(it, 1500);
        return ok;
    }

    /** Benachrichtigung zu einem Eintrag aus der Statusleiste entfernen. */
    public static PendingIntent intentFor(String key) {
        return key == null ? null : intents.get(key);
    }

    /** Benachrichtigung aus der Statusleiste entfernen (wie Wegwischen). */
    public static boolean dismiss(String key) {
        NotificationCollector inst = instance;
        if (inst != null && key != null) {
            try { inst.cancelNotification(key); intents.remove(key); deletes.remove(key); return true; }
            catch (Exception e) { Log.w(TAG, "dismiss", e); }
        }
        return false;
    }

    /** Benachrichtigung zu einem Eintrag aus der Statusleiste entfernen. */
    public static void dismiss(NotificationStore.Item it) {
        StatusBarNotification sbn = resolve(it);
        if (sbn != null) dismiss(sbn.getKey());
        else if (it != null) dismiss(it.nkey);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            store(sbn);
        } catch (Exception e) {
            Log.w(TAG, "Fehler beim Ablegen", e);
        }
    }

    private void store(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();
        Notification n = sbn.getNotification();
        if (n == null) return;

        // Reine Gruppen-Zusammenfassung ("3 neue Mails") ueberspringen.
        boolean isSummary = (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0;
        if (isSummary) {
            Log.d(TAG, "uebersprungen (Gruppen-Summary): " + pkg);
            return;
        }

        String[] tt = extract(n);
        String title = tt[0], text = tt[1];

        if ((title == null || title.length() == 0)
                && (text == null || text.length() == 0)) {
            Log.d(TAG, "uebersprungen (leer): " + pkg);
            return;
        }

        // contentIntent merken - das oeffnet beim Antippen die konkrete Mail.
        if (n.contentIntent != null) intents.put(sbn.getKey(), n.contentIntent);
        PendingIntent del = findDeleteAction(n);
        if (del != null) deletes.put(sbn.getKey(), del);

        // Diagnose: Art des contentIntent und vorhandene Aktionen protokollieren.
        if (android.os.Build.VERSION.SDK_INT >= 31 && n.contentIntent != null) {
            PendingIntent ci = n.contentIntent;
            Log.d(TAG, "contentIntent " + pkg + ": activity=" + ci.isActivity()
                    + " broadcast=" + ci.isBroadcast() + " service=" + ci.isService()
                    + " fgService=" + ci.isForegroundService());
        }
        if (n.actions != null) for (Notification.Action a : n.actions) {
            if (a != null) Log.d(TAG, "Aktion " + pkg + ": \"" + a.title
                    + "\" semantic=" + a.getSemanticAction());
        }

        NotificationStore.get(this).add(
                sbn.getKey(), pkg,
                title, text,
                sbn.getPostTime());

        Log.d(TAG, "gespeichert: " + pkg + " – "
                + (title == null ? "" : title) + " / "
                + (text == null ? "" : text));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Eintrag in der Ablage behalten, aber den (nun ungueltigen) Intent lösen.
        if (sbn != null) { intents.remove(sbn.getKey()); deletes.remove(sbn.getKey()); }
    }

    @Override
    public void onListenerConnected() {
        instance = this;
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return;
            Log.d(TAG, "verbunden, " + active.length + " aktive Benachrichtigungen");
            for (StatusBarNotification sbn : active) store(sbn);
        } catch (Exception e) {
            Log.w(TAG, "onListenerConnected", e);
        }
    }

    @Override
    public void onListenerDisconnected() {
        if (instance == this) instance = null;
    }
}
