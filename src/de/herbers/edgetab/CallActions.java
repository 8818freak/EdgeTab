package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Gemeinsame Anruf-Aktionen fuer die Anrufliste (CallsTab) und die Anruf-Zeilen
 * im Posteingang (InboxTab). Buendelt:
 *  - die Rufnummern-Kennung (Mobil/Arbeit/Privat ...) aus dem Telefonbuch,
 *  - den Direktanruf (kurzer Tipp) mit Rueckfall auf die Waehl-App,
 *  - das Inline-Menue beim langen Tipp (EdgeTab zeigt Menues im Overlay
 *    grundsaetzlich inline, da ein Overlay-Dienst keinen Fenster-Token fuer
 *    PopupMenu/AlertDialog hat).
 */
final class CallActions {

    private CallActions() {}

    // Welche Anruf-Zeile gerade ihr Menue offen hat (Schluessel = CallLog _id).
    // Statisch, damit es einen Panel-Neuaufbau ueberlebt, wie REPLYING in InboxTab.
    static final java.util.Set<Long> MENU_OPEN = new java.util.HashSet<>();

    /** Lesbare Nummern-Kennung wie im Telefonbuch (Mobil, Arbeit, Privat, …)
     *  aus den im Anrufprotokoll zwischengespeicherten Feldern. Leer, wenn nichts
     *  Sinnvolles vorliegt. */
    static String typeLabel(Context ctx, int type, String customLabel) {
        try {
            if (type <= 0 && (customLabel == null || customLabel.isEmpty())) return "";
            CharSequence cs = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    ctx.getResources(), type, customLabel);
            return cs == null ? "" : cs.toString();
        } catch (Throwable t) { return ""; }
    }

    /** Kurzer Tipp: direkt anrufen. Mit CALL_PHONE sofort (ACTION_CALL), sonst
     *  Recht anfordern und auf die Waehl-App ausweichen, damit trotzdem etwas
     *  passiert. */
    static void call(Context ctx, String num, Runnable close) {
        if (num == null || num.trim().isEmpty()) return;
        String tel = "tel:" + Uri.encode(num);
        if (ctx.checkSelfPermission(android.Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                ctx.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(tel))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                if (close != null) close.run();
                return;
            } catch (Throwable ignored) {}
        } else {
            // Recht anfordern (wie beim SMS-Senden) und diesmal die Waehl-App
            // oeffnen, damit der Anruf trotzdem moeglich ist.
            try {
                ctx.startActivity(new Intent(ctx, MainActivity.class)
                        .putExtra("request_permission", android.Manifest.permission.CALL_PHONE)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Throwable ignored) {}
        }
        dialer(ctx, num, close);
    }

    /** Waehl-App mit vorausgefuellter Nummer oeffnen (ACTION_DIAL, kein Recht noetig). */
    static void dialer(Context ctx, String num, Runnable close) {
        if (num == null) num = "";
        try {
            ctx.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(num)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {}
        if (close != null) close.run();
    }

    static void sms(Context ctx, String num, Runnable close) {
        if (num == null) num = "";
        try {
            ctx.startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(num)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {}
        if (close != null) close.run();
    }

    /** Nummer im Telefonbuch zeigen; ist sie unbekannt, einen neuen/ergaenzten
     *  Kontakt anbieten. */
    static void contact(Context ctx, String num, Runnable close) {
        if (num == null) num = "";
        try {
            Uri look = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(num));
            Long id = null; String lookup = null;
            try (android.database.Cursor c = ctx.getContentResolver().query(look,
                    new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY},
                    null, null, null)) {
                if (c != null && c.moveToNext()) { id = c.getLong(0); lookup = c.getString(1); }
            }
            Intent i;
            if (id != null && lookup != null) {
                i = new Intent(Intent.ACTION_VIEW,
                        ContactsContract.Contacts.getLookupUri(id, lookup));
            } else {
                i = new Intent(Intent.ACTION_INSERT_OR_EDIT)
                        .setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                        .putExtra(ContactsContract.Intents.Insert.PHONE, num);
            }
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable ignored) {}
        if (close != null) close.run();
    }

    static void copy(Context ctx, String num) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && num != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Rufnummer", num));
                Toast.makeText(ctx, "Nummer kopiert", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignored) {}
    }

    static boolean deleteFromLog(Context ctx, long id) {
        if (ctx.checkSelfPermission(android.Manifest.permission.WRITE_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            try {
                ctx.startActivity(new Intent(ctx, MainActivity.class)
                        .putExtra("request_permission", android.Manifest.permission.WRITE_CALL_LOG)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Throwable ignored) {}
            return false;
        }
        try {
            int n = ctx.getContentResolver().delete(CallLog.Calls.CONTENT_URI,
                    CallLog.Calls._ID + "=?", new String[]{String.valueOf(id)});
            return n > 0;
        } catch (Throwable t) { return false; }
    }

    /** Das aufgeklappte Inline-Menue (langer Tipp) als fertige Ansicht. Baut die
     *  Optionszeilen; refresh baut das Panel nach einer Aktion neu auf. */
    static View menu(Context ctx, long rowId, String num, float fs, int d,
                     Runnable close, Runnable refresh) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#22262B"));
        bg.setCornerRadius(10 * d);
        box.setBackground(bg);
        box.setPadding(6 * d, 4 * d, 6 * d, 4 * d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 6 * d;
        box.setLayoutParams(lp);

        box.addView(item(ctx, "📞  Anrufen", fs, d, () -> call(ctx, num, close)));
        box.addView(item(ctx, "🔢  Wähl-App öffnen", fs, d, () -> dialer(ctx, num, close)));
        box.addView(item(ctx, "✉  SMS schreiben", fs, d, () -> sms(ctx, num, close)));
        box.addView(item(ctx, "👤  Im Telefonbuch öffnen", fs, d, () -> contact(ctx, num, close)));
        box.addView(item(ctx, "⧉  Nummer kopieren", fs, d, () -> { copy(ctx, num); }));
        box.addView(item(ctx, "🗑  Aus Anrufliste löschen", fs, d, () -> {
            boolean ok = deleteFromLog(ctx, rowId);
            MENU_OPEN.remove(rowId);
            if (ok && refresh != null) refresh.run();
        }));
        return box;
    }

    private static TextView item(Context ctx, String label, float fs, int d, Runnable onTap) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(Color.parseColor("#D0D4D8"));
        tv.setTextSize(14 * fs);
        tv.setPadding(10 * d, 12 * d, 10 * d, 12 * d);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setClickable(true);
        tv.setOnClickListener(v -> onTap.run());
        return tv;
    }
}
