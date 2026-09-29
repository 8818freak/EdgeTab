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
        return l;
    }

    static void checkReminders(Context ctx) {
        PermReminder.check(ctx, "EdgeTab", list(ctx));
    }
}
