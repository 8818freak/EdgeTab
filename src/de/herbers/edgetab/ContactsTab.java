package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kontakte-Karte ueber den Standard-Kontakteprovider (ContactsContract) -
 * wahlweise alle Kontakte, eine frei gewaehlte Auswahl, oder nur die als
 * Favorit markierten. Tippen oeffnet den Kontakt in der System-Kontakte-App.
 *
 * Berechtigung wird NICHT beim Ersteinrichten verlangt (anders als Kalender),
 * sondern erst hier, bei Bedarf - ueber MainActivity (`request_permission`),
 * da eine Laufzeit-Berechtigungsabfrage eine echte Activity braucht.
 */
public class ContactsTab extends BaseTab {

    // Zustand, der einen Panel-Neuaufbau ueberleben muss (wie bei ShortcutsTab).
    private static final Set<String> MENU_OPEN = new HashSet<>();
    private static final Set<String> PICKING = new HashSet<>();
    private static final Map<String, Integer> SAVED_SCROLL = new HashMap<>();

    ContactsTab(TabInstance inst) { super(inst, "Kontakte", R.drawable.ic_contacts); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);

        if (ctx.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            return permissionHint(ctx, closePanel, d);
        }

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        final Runnable refresh = () -> {
            SAVED_SCROLL.put(inst.id, scroll.getScrollY());
            if (refreshContent != null) refreshContent.run();
        };

        boolean picking = PICKING.contains(inst.id);
        boolean menuOpen = MENU_OPEN.contains(inst.id);

        LinearLayout tools = new LinearLayout(ctx);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);

        if (picking) {
            Button done = new Button(ctx);
            done.setText("Fertig");
            done.setOnClickListener(v -> { PICKING.remove(inst.id); refresh.run(); });
            tools.addView(done);
        } else {
            TextView gear = new TextView(ctx);
            gear.setText("⚙");
            gear.setTextColor(Color.parseColor("#B0B0B0"));
            gear.setTextSize(20);
            gear.setPadding(8 * d, 6 * d, 8 * d, 6 * d);
            gear.setOnClickListener(v -> {
                if (menuOpen) MENU_OPEN.remove(inst.id); else MENU_OPEN.add(inst.id);
                refresh.run();
            });
            tools.addView(gear);

            if (menuOpen) {
                tools.addView(modeButton(ctx, "Alle", "all", refresh));
                tools.addView(modeButton(ctx, "Ausgewählt", "selected", refresh));
                tools.addView(modeButton(ctx, "Favoriten", "favorites", refresh));
            }
        }
        root.addView(tools);

        // Absichtlich NICHT an menuOpen gekoppelt: das Waehlen von "Ausgewaehlt"
        // im Menue schliesst das Menue sofort (siehe modeButton) - waere dieser
        // Knopf an menuOpen gebunden, wuerde er im selben Moment verschwinden,
        // in dem er noetig wird. Er bleibt darum sichtbar, solange der Modus
        // "selected" ist, unabhaengig vom Menuestatus.
        if ("selected".equals(inst.contactsMode) && !picking) {
            Button pick = new Button(ctx);
            pick.setText("Kontakte auswählen…");
            pick.setOnClickListener(v -> {
                PICKING.add(inst.id);
                MENU_OPEN.remove(inst.id);
                refresh.run();
            });
            root.addView(pick);
        }

        if (picking) {
            root.addView(contactPicker(ctx, refresh, d));
        } else {
            List<ContactRow> rows = queryContacts(ctx, inst.contactsMode, inst.selectedContacts);
            if (rows.isEmpty()) {
                TextView hint = new TextView(ctx);
                hint.setText("selected".equals(inst.contactsMode)
                        ? "Noch keine Kontakte ausgewählt. Über das Zahnrad oben \"Kontakte auswählen…\"."
                        : "favorites".equals(inst.contactsMode)
                        ? "Keine Favoriten. In der Kontakte-App einen Kontakt als Favorit markieren."
                        : "Keine Kontakte gefunden.");
                hint.setTextColor(Color.parseColor("#9E9E9E"));
                hint.setTextSize(14);
                hint.setPadding(0, 12 * d, 0, 0);
                root.addView(hint);
            } else {
                for (ContactRow row : rows) root.addView(contactRow(ctx, row, close, d));
            }
        }

        Integer y = SAVED_SCROLL.remove(inst.id);
        if (y != null) { final int yy = y; scroll.post(() -> scroll.scrollTo(0, yy)); }
        // Schwebendes Personen-Symbol statt Textknopf im Menue - wie in BBs
        // eigener Kontakte-App (Mathias' Screenshot), immer erreichbar statt
        // hinter dem Zahnrad versteckt. Waehrend der Auswahl (picking)
        // ausgeblendet, um nicht zu verwirren.
        if (picking) return scroll;
        return withFab(ctx, scroll, fab(ctx, R.drawable.ic_fab_person_add, "#2E9BE6", v -> {
            try {
                Intent i = new Intent(Intent.ACTION_INSERT)
                        .setData(ContactsContract.Contacts.CONTENT_URI)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {}
            if (close != null) close.run();
        }));
    }

    private View permissionHint(Context ctx, Runnable closePanel, int d) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 12 * d, 0, 0);

        TextView t = new TextView(ctx);
        t.setText("Für die Kontakte-Karte fehlt die Berechtigung, auf deine "
                + "Kontakte zuzugreifen.");
        t.setTextColor(Color.parseColor("#CCCCCC"));
        t.setTextSize(14);
        t.setPadding(0, 0, 0, 12 * d);
        box.addView(t);

        Button allow = new Button(ctx);
        allow.setText("Zugriff erlauben");
        allow.setOnClickListener(v -> {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("request_permission", android.Manifest.permission.READ_CONTACTS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            if (closePanel != null) closePanel.run();
        });
        box.addView(allow);
        return box;
    }

    private Button modeButton(Context ctx, String label, String mode, Runnable refresh) {
        Button b = new Button(ctx);
        b.setText(inst.contactsMode.equals(mode) ? "✓ " + label : label);
        b.setOnClickListener(v -> {
            Tabs.update(ctx, inst.id, t -> t.contactsMode = mode);
            MENU_OPEN.remove(inst.id);
            refresh.run();
        });
        return b;
    }

    // ---------- Kontakte abfragen ----------

    private static final class ContactRow {
        String lookupKey; String name; String photoUri;
    }

    private List<ContactRow> queryContacts(Context ctx, String mode, List<String> selectedKeys) {
        List<ContactRow> out = new ArrayList<>();
        String selection = "favorites".equals(mode) ? ContactsContract.Contacts.STARRED + "=1" : null;
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI},
                    selection, null, ContactsContract.Contacts.SORT_KEY_PRIMARY + " ASC");
            if (c != null) {
                Set<String> want = "selected".equals(mode) ? new HashSet<>(selectedKeys) : null;
                while (c.moveToNext()) {
                    String key = c.getString(0);
                    if (want != null && !want.contains(key)) continue;
                    String name = c.getString(1);
                    if (name == null) continue;
                    ContactRow row = new ContactRow();
                    row.lookupKey = key;
                    row.name = name;
                    row.photoUri = c.getString(2);
                    out.add(row);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private View contactRow(Context ctx, ContactRow row, Runnable close, int d) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2C2C2E"));
        bg.setCornerRadius(10 * d);
        r.setBackground(bg);
        r.setPadding(10 * d, 8 * d, 10 * d, 8 * d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 6 * d;
        r.setLayoutParams(lp);

        ImageView photo = new ImageView(ctx);
        Bitmap bmp = row.photoUri != null ? decodePhoto(ctx, row.photoUri) : null;
        if (bmp != null) {
            photo.setImageBitmap(bmp);
        } else {
            photo.setImageResource(R.drawable.ic_contacts);
            photo.setColorFilter(Color.parseColor("#B0B0B0"));
        }
        int s = 32 * d;
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(s, s);
        plp.rightMargin = 12 * d;
        photo.setLayoutParams(plp);
        r.addView(photo);

        TextView name = new TextView(ctx);
        name.setText(row.name);
        name.setTextColor(Color.WHITE);
        name.setTextSize(15);
        r.addView(name);

        r.setClickable(true);
        r.setOnClickListener(new OpenClick(ctx, row.lookupKey, close));
        return r;
    }

    private Bitmap decodePhoto(Context ctx, String uriStr) {
        try {
            InputStream in = ctx.getContentResolver().openInputStream(Uri.parse(uriStr));
            Bitmap b = BitmapFactory.decodeStream(in);
            if (in != null) in.close();
            return b;
        } catch (Exception e) { return null; }
    }

    static final class OpenClick implements View.OnClickListener {
        private final Context ctx; private final String lookupKey; private final Runnable close;
        OpenClick(Context ctx, String lookupKey, Runnable close) {
            this.ctx = ctx; this.lookupKey = lookupKey; this.close = close;
        }
        public void onClick(View v) {
            try {
                Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {}
            if (close != null) close.run();
        }
    }

    // ---------- Auswahl-Liste (fuer Modus "selected") ----------

    private View contactPicker(Context ctx, Runnable refresh, int d) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 8 * d, 0, 0);

        List<ContactRow> all = queryContacts(ctx, "all", null);
        for (ContactRow row : all) {
            LinearLayout r = new LinearLayout(ctx);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(0, 6 * d, 0, 6 * d);

            // Ohne eigene Farbe war das Kaestchen im Overlay-Fenster kaum zu
            // erkennen (die Service-Ansicht hat kein eigenes Theme, das dem
            // Standard-Kaestchen genug Kontrast zur dunklen Karte gibt).
            CheckBox cb = new CheckBox(ctx);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E9BE6")));
            cb.setChecked(inst.selectedContacts.contains(row.lookupKey));
            final String key = row.lookupKey;
            cb.setOnCheckedChangeListener((v, on) -> Tabs.update(ctx, inst.id, t -> {
                if (on) { if (!t.selectedContacts.contains(key)) t.selectedContacts.add(key); }
                else t.selectedContacts.remove(key);
            }));
            r.addView(cb);

            ImageView photo = new ImageView(ctx);
            Bitmap bmp = row.photoUri != null ? decodePhoto(ctx, row.photoUri) : null;
            if (bmp != null) {
                photo.setImageBitmap(bmp);
            } else {
                photo.setImageResource(R.drawable.ic_contacts);
                photo.setColorFilter(Color.parseColor("#B0B0B0"));
            }
            int s = 26 * d;
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(s, s);
            plp.leftMargin = 4 * d; plp.rightMargin = 10 * d;
            photo.setLayoutParams(plp);
            r.addView(photo);

            TextView name = new TextView(ctx);
            name.setText(row.name);
            name.setTextColor(Color.WHITE);
            name.setTextSize(14);
            r.addView(name);

            box.addView(r);
        }
        if (all.isEmpty()) {
            TextView none = new TextView(ctx);
            none.setText("Keine Kontakte gefunden.");
            none.setTextColor(Color.parseColor("#9E9E9E"));
            box.addView(none);
        }
        return box;
    }
}
