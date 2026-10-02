package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Kleine Helfer fuer die SMS-/Anrufe-Karten: ein Berechtigungs-Hinweis mit
 *  "Zugriff erlauben"-Knopf (fuehrt ueber MainActivity zur Laufzeit-Abfrage)
 *  und die Aufloesung einer Telefonnummer zu einem Kontaktnamen. */
final class TabPermHint {
    private TabPermHint() {}

    static View build(Context ctx, Runnable closePanel, int d, String message, String permission) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 12 * d, 0, 0);
        TextView t = new TextView(ctx);
        t.setText(message);
        t.setTextColor(Color.parseColor("#CCCCCC"));
        t.setTextSize(14);
        t.setPadding(0, 0, 0, 12 * d);
        box.addView(t);
        Button allow = new Button(ctx);
        allow.setText(R.string.grant_access_button);
        allow.setOnClickListener(v -> {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("request_permission", permission);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            if (closePanel != null) closePanel.run();
        });
        box.addView(allow);
        return box;
    }

    /** Eine SMS senden (mehrteilig). Braucht SEND_SMS. true bei Erfolg. */
    static boolean sendSms(Context ctx, String addr, String text) {
        try {
            android.telephony.SmsManager sm = ctx.getSystemService(android.telephony.SmsManager.class);
            if (sm == null) sm = android.telephony.SmsManager.getDefault();
            java.util.ArrayList<String> parts = sm.divideMessage(text);
            sm.sendMultipartTextMessage(addr, null, parts, null, null);
            return true;
        } catch (Throwable t) { return false; }
    }

    /** Kontaktname zu einer Telefonnummer (falls Kontakte-Zugriff besteht), sonst null. */
    // Nummer -> Name zwischenspeichern (leerer String = "kein Kontakt"): die
    // Kontakte-Abfrage lief vorher je SMS/Anruf-Zeile, bei vielen tausend
    // Eintraegen der Hauptgrund fuer das zaehe Laden von Posteingang/SMS/Anrufen.
    // Sitzungsweit; Kontakte aendern sich selten. Fehlendes Recht wird NICHT
    // gecacht (damit es nach dem Erteilen sofort greift).
    private static final java.util.Map<String, String> NAME_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    static String contactName(Context ctx, String number) {
        if (number == null || number.isEmpty()) return null;
        String cached = NAME_CACHE.get(number);
        if (cached != null) return cached.isEmpty() ? null : cached;
        if (ctx.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null;
        String name = null;
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            try (Cursor c = ctx.getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst()) name = c.getString(0);
            }
        } catch (Exception ignored) {}
        NAME_CACHE.put(number, name == null ? "" : name);
        return name;
    }
}
