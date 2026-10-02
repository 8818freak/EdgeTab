package de.herbers.edgetab;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import de.herbers.common.PermReminder;

import java.util.ArrayList;
import java.util.List;

/** Eine Quelle fuer EdgeTabs Berechtigungen - genutzt fuer die Erinnerung bei
 *  Verlust (PermReminder) UND fuer den aufklappbaren, erklaerten
 *  Berechtigungs-Abschnitt in den Einstellungen. */
final class Perms {
    private Perms() {}

    static boolean notifAccess(Context c) {
        String flat = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        if (TextUtils.isEmpty(flat)) return false;
        ComponentName me = new ComponentName(c, NotificationCollector.class);
        for (String e : flat.split(":")) {
            ComponentName cn = ComponentName.unflattenFromString(e);
            if (cn != null && cn.equals(me)) return true;
        }
        return false;
    }

    private static Intent appDetails(Context c) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + c.getPackageName()));
    }

    static List<PermReminder.Perm> list(Context ctx) {
        boolean cal = ctx.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
        boolean con = ctx.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
        List<PermReminder.Perm> l = new ArrayList<>();
        l.add(new PermReminder.Perm("overlay", "Über anderen Apps anzeigen",
                Settings.canDrawOverlays(ctx),
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.getPackageName())),
                "Damit sich die EdgeTab-Leiste über anderen Apps einblenden kann – die Grundlage der App."));
        l.add(new PermReminder.Perm("notif", "Benachrichtigungszugriff",
                notifAccess(ctx),
                new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"),
                "Für den Posteingang – liest die System-Benachrichtigungen (Antworten/Als gelesen/Löschen)."));
        l.add(new PermReminder.Perm("calendar", "Kalender",
                cal, appDetails(ctx),
                "Für die Termine-Karte – zeigt anstehende Termine."));
        l.add(new PermReminder.Perm("contacts", "Kontakte",
                con, appDetails(ctx),
                "Für die Kontakte-Karte – schneller Zugriff auf Personen."));
        boolean sms = ctx.checkSelfPermission(android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean sendSms = ctx.checkSelfPermission(android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean calls = ctx.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        boolean writeCalls = ctx.checkSelfPermission(android.Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        boolean callPhone = ctx.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
        l.add(new PermReminder.Perm("sms", "SMS lesen",
                sms, appDetails(ctx),
                "Für die SMS-Karte und den Posteingang – zeigt die letzten Kurznachrichten."));
        l.add(new PermReminder.Perm("send_sms", "SMS senden",
                sendSms, appDetails(ctx),
                "Für die Direktantwort auf eine SMS aus dem Posteingang bzw. der SMS-Karte."));
        l.add(new PermReminder.Perm("calls", "Anrufliste lesen",
                calls, appDetails(ctx),
                "Für die Anrufe-Karte und den Posteingang – zeigt die letzten Anrufe."));
        l.add(new PermReminder.Perm("write_calls", "Anrufliste ändern",
                writeCalls, appDetails(ctx),
                "Um verpasste Anrufe als gesehen zu markieren oder Einträge aus der Anrufliste zu löschen."));
        l.add(new PermReminder.Perm("call_phone", "Anrufen",
                callPhone, appDetails(ctx),
                "Um aus der Anrufliste heraus mit kurzem Tipp direkt anzurufen. Ohne das Recht öffnet sich stattdessen die Wähl-App mit der Nummer."));
        return l;
    }

    static void checkReminders(Context ctx) {
        PermReminder.check(ctx, "EdgeTab", list(ctx));
    }
}
