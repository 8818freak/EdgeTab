package de.herbers.edgetab;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Kein Dauer-Startbildschirm - nach dem BB-Vorbild.
 *
 * Beim ersten Start (oder solange Berechtigungen fehlen) zeigt diese Seite die
 * Instruktionen und fuehrt durch die drei noetigen Berechtigungen. Sobald alles
 * erteilt ist, startet ein Antippen des App-Icons nur noch die Leiste, zieht
 * sie auf und schliesst sich sofort wieder - es gibt dann keine Startseite mehr.
 */
public class MainActivity extends Activity {

    private static final int REQ_CAL = 1;
    private static final int REQ_BIND = 2;
    private static final int REQ_CONFIGURE = 3;
    private static final int REQ_ICON = 4;
    private static final int REQ_PERMISSION = 5;
    private static final int REQ_ACCOUNT = 6;
    /** Kontotyp von DAVx5 (siehe SyncApp-Enum in JTX Board, per jadx bestaetigt). */
    private static final String DAVX5_ACCOUNT_TYPE = "bitfire.at.davdroid";
    private int pendingWidgetId = 0;
    private String pendingProvider;
    private String pendingTabId;
    private int pendingSlot = -1;
    private String pendingIconTabId;
    private String[] pendingPermissions;
    private boolean pendingAccountPick;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        route();
    }

    @Override
    protected void onResume() {
        super.onResume();
        route();
    }

    /** Entscheidet: alles bereit -> Leiste aufziehen; sonst Instruktionen. */
    private void route() {
        Intent in = getIntent();
        if (in != null && in.getBooleanExtra("pick_widget", false)) {
            if (pendingWidgetId == 0) {
                pendingTabId = in.getStringExtra("tab_id");
                pendingSlot = in.getIntExtra("slot", -1);
                showWidgetPicker();
            }
            return;
        }
        if (in != null && in.hasExtra("pick_icon")) {
            if (pendingIconTabId == null) {
                pendingIconTabId = in.getStringExtra("pick_icon");
                pickIcon();
            }
            return;
        }
        if (in != null && (in.hasExtra("request_permission") || in.hasExtra("request_permissions"))) {
            if (pendingPermissions == null) {
                String[] multi = in.getStringArrayExtra("request_permissions");
                pendingPermissions = multi != null ? multi
                        : new String[]{in.getStringExtra("request_permission")};
                requestPermissions(pendingPermissions, REQ_PERMISSION);
            }
            return;
        }
        if (in != null && in.getBooleanExtra("pick_jtx_account", false)) {
            if (!pendingAccountPick) {
                pendingAccountPick = true;
                showJtxAccountSetup();
            }
            return;
        }
        if (allReady()) {
            // Dienst sicherstellen und Leiste von der eingestellten Seite aufziehen.
            startForegroundService(new Intent(this, EdgeService.class));
            startService(new Intent(this, EdgeService.class).putExtra("open", true));
            finish();
        } else {
            showInstructions();
        }
    }

    private boolean allReady() {
        boolean overlay = Settings.canDrawOverlays(this);
        boolean listener = isNotificationAccessGranted();
        boolean calendar = checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        return overlay && listener && calendar;
    }

    private void showInstructions() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("EdgeTab");
        title.setTextSize(26);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Die Produktivitätsleiste am Rand.\n\n"
                + "Einmalig eingerichtet, lebt danach alles in der Leiste selbst: "
                + "Am Rand erscheint ein Griff – antippen oder zur Mitte wischen "
                + "zieht sie auf, Zurückwischen schließt sie wieder. Die "
                + "Einstellungen sitzen oben als eigene Karte.\n\n"
                + "Für den Anfang sind drei Berechtigungen nötig:");
        sub.setTextSize(14);
        sub.setPadding(0, dp(4), 0, dp(20));
        root.addView(sub);

        boolean overlay = Settings.canDrawOverlays(this);
        boolean listener = isNotificationAccessGranted();
        boolean calendar = checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;

        root.addView(step("1. Über anderen Apps anzeigen", overlay, "Erlauben",
                v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())))));
        root.addView(step("2. Benachrichtigungszugriff (für den Posteingang)", listener,
                "Erlauben", v -> startActivity(
                        new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))));
        root.addView(step("3. Kalender", calendar, "Erlauben",
                v -> requestPermissions(
                        new String[]{android.Manifest.permission.READ_CALENDAR}, REQ_CAL)));

        TextView hint = new TextView(this);
        hint.setText("\nSobald alle drei erteilt sind, öffnet sich die Leiste "
                + "automatisch – und dieser Bildschirm erscheint nicht mehr.");
        hint.setTextColor(Color.GRAY);
        root.addView(hint);

        setContentView(root);
    }

    private View step(String label, boolean done, String action, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView status = new TextView(this);
        status.setText(done ? "✓ " : "○ ");
        status.setTextColor(done ? Color.parseColor("#2E9BE6") : Color.GRAY);
        status.setTextSize(18);

        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(15);
        t.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(status);
        row.addView(t);
        if (!done) {
            Button btn = new Button(this);
            btn.setText(action);
            btn.setOnClickListener(onClick);
            row.addView(btn);
        }
        return row;
    }

    private boolean isNotificationAccessGranted() {
        String flat = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ---------- Widget-Auswahl (fuer die Widget-Karte) ----------

    private void showWidgetPicker() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Widget auswählen");
        title.setTextSize(22);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Ein installiertes App-Widget für die Leiste. Für den Hub-Posteingang "
                + "das Widget des BlackBerry Hub wählen.\n");
        sub.setTextSize(13);
        sub.setPadding(0, dp(4), 0, dp(12));
        root.addView(sub);

        // HINWEIS zu fehlenden Widgets (z.B. BBMe): getInstalledProviders()
        // filtert vermutlich intern auf Kategorie HOME_SCREEN; der oeffentliche
        // API-Ueberladung mit Kategorie-Parameter existiert im aktuellen SDK
        // NICHT mehr (nur noch das parameterlose getInstalledProviders() ist
        // oeffentlich - javap bestaetigt das). Wir koennen das darum von hier
        // aus nicht erzwingen. Wenn ein Widget in dieser Liste fehlt, aber
        // auch im SYSTEM-Widget-Dialog (Startbildschirm lange druecken ->
        // Widgets) nicht auftaucht, liegt es an der Widget-App selbst
        // (kein exportierter Home-Screen-Widget-Provider) - das kann keine
        // dritte App umgehen.
        List<AppWidgetProviderInfo> providers =
                AppWidgetManager.getInstance(this).getInstalledProviders();
        final android.content.pm.PackageManager pm = getPackageManager();
        // Nach App-Name gruppiert sortieren - ueber eine TreeMap, um die
        // synthetische Comparator-Bruecke zu vermeiden (daran stuerzt d8 ab).
        java.util.TreeMap<String, AppWidgetProviderInfo> sorted = new java.util.TreeMap<>();
        int idx = 0;
        if (providers != null) for (AppWidgetProviderInfo info : providers) {
            if (info == null || info.provider == null) continue;
            String app = appLabel(pm, info.provider.getPackageName());
            CharSequence wl = null;
            try { wl = info.loadLabel(pm); } catch (Throwable ignored) {}
            String w = wl == null ? "" : wl.toString();
            sorted.put(app.toLowerCase() + "\u0001" + w.toLowerCase() + "\u0001" + (idx++), info);
        }

        String lastApp = null;
        for (java.util.Map.Entry<String, AppWidgetProviderInfo> e : sorted.entrySet()) {
            AppWidgetProviderInfo info = e.getValue();
            String app = appLabel(pm, info.provider.getPackageName());
            if (!app.equals(lastApp)) {
                lastApp = app;
                TextView head = new TextView(this);
                head.setText(app);
                head.setTextColor(Color.parseColor("#2E9BE6"));
                head.setTextSize(13);
                head.setPadding(0, dp(12), 0, dp(2));
                root.addView(head);
            }
            CharSequence wl = null;
            try { wl = info.loadLabel(pm); } catch (Throwable ignored) {}
            Button b = new Button(this);
            b.setText(wl == null || wl.length() == 0 ? info.provider.getShortClassName() : wl.toString());
            b.setAllCaps(false);
            b.setOnClickListener(new ProviderClick(this, info));
            root.addView(b);
        }
        if (sorted.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("Keine App-Widgets gefunden.");
            root.addView(none);
        }
        setContentView(scroll);
    }

    private void beginBind(AppWidgetProviderInfo info) {
        AppWidgetHost host = WidgetHostHolder.host(this);
        int id = host.allocateAppWidgetId();
        pendingWidgetId = id;
        pendingProvider = info.provider.flattenToString();
        boolean ok = AppWidgetManager.getInstance(this)
                .bindAppWidgetIdIfAllowed(id, info.provider);
        if (ok) {
            afterBound();
        } else {
            Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
            startActivityForResult(i, REQ_BIND);
        }
    }

    private void afterBound() {
        AppWidgetProviderInfo info =
                AppWidgetManager.getInstance(this).getAppWidgetInfo(pendingWidgetId);
        if (info != null && info.configure != null) {
            try {
                WidgetHostHolder.host(this).startAppWidgetConfigureActivityForResult(
                        this, pendingWidgetId, 0, REQ_CONFIGURE, null);
                return;
            } catch (Throwable t) {
                // Manche Konfig-Activities sind nicht exportiert - dann ohne Konfiguration.
            }
        }
        finishPick();
    }

    private void finishPick() {
        final int wId = pendingWidgetId;
        final String wProvider = pendingProvider;
        final int slot = pendingSlot;
        Tabs.update(this, pendingTabId, t -> {
            TabInstance.WidgetRef ref = new TabInstance.WidgetRef(wId, wProvider);
            if (slot >= 0 && slot < t.widgets.size()) t.widgets.set(slot, ref);
            else t.widgets.add(ref);
        });
        pendingWidgetId = 0; pendingProvider = null; pendingTabId = null; pendingSlot = -1;
        startService(new Intent(this, EdgeService.class).putExtra("open", true));
        finish();
    }

    private void cancelPending() {
        if (pendingWidgetId != 0) {
            try { WidgetHostHolder.host(this).deleteAppWidgetId(pendingWidgetId); }
            catch (Throwable ignored) {}
        }
        pendingWidgetId = 0;
    }

    // ---------- Konto-Einrichtung fuer die Aufgaben-/Notizen-Karte ----------
    // JTX Boards Content-Provider ist kontobezogen (siehe TasksTab-Kommentar);
    // wir brauchen also einmalig den Namen des DAVx5-Kontos. Der System-Konto-
    // waehler (newChooseAccountIntent) laeuft aber mit den Sichtbarkeits-
    // Rechten von EdgeTab selbst - seit Android 8 sieht eine App fremde
    // Konten nur, wenn deren Besitzer-App das explizit erlaubt. DAVx5 tut das
    // nicht automatisch, darum bleibt die Liste dort oft leer (bei Mathias
    // bestaetigt). Zuverlaessiger: der Kontoname laesst sich direkt in DAVx5
    // ablesen und hier eintippen - das umgeht die Sichtbarkeits-Huerde
    // komplett, weil EdgeTab dann gar keine Konten mehr auflisten muss.
    private void showJtxAccountSetup() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("JTX-Board-Konto einrichten");
        title.setTextSize(22);
        root.addView(title);

        String current = de.herbers.edgetab.Settings.jtxAccountName(this);
        TextView sub = new TextView(this);
        sub.setText((current != null ? "Aktuell eingetragen: " + current + "\n\n" : "")
                + "Am zuverlässigsten: Öffne DAVx5, dort steht der Kontoname oben auf "
                + "der Karte des Kontos - genau so hier eintragen.");
        sub.setTextSize(13);
        sub.setPadding(0, dp(6), 0, dp(16));
        root.addView(sub);

        TextView nameLabel = new TextView(this);
        nameLabel.setText("Kontoname (aus DAVx5)");
        nameLabel.setTextSize(12);
        root.addView(nameLabel);
        EditText nameField = new EditText(this);
        nameField.setSingleLine(true);
        if (current != null) nameField.setText(current);
        root.addView(nameField);

        TextView typeLabel = new TextView(this);
        typeLabel.setText("Kontotyp (nur bei anderer Sync-App als DAVx5 ändern)");
        typeLabel.setTextSize(12);
        typeLabel.setPadding(0, dp(12), 0, 0);
        root.addView(typeLabel);
        EditText typeField = new EditText(this);
        typeField.setSingleLine(true);
        String currentType = de.herbers.edgetab.Settings.jtxAccountType(this);
        typeField.setText(currentType != null ? currentType : DAVX5_ACCOUNT_TYPE);
        root.addView(typeField);

        Button save = new Button(this);
        save.setText("Übernehmen");
        save.setOnClickListener(v -> {
            String name = nameField.getText().toString().trim();
            String type = typeField.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Bitte Kontoname eingeben", Toast.LENGTH_SHORT).show();
                return;
            }
            de.herbers.edgetab.Settings.setJtxAccount(this, name,
                    type.isEmpty() ? DAVX5_ACCOUNT_TYPE : type);
            finishAccountPick();
        });
        save.setPadding(0, dp(16), 0, 0);
        root.addView(save);

        TextView orLabel = new TextView(this);
        orLabel.setText("\noder, falls dein Gerät es zulässt:");
        orLabel.setTextSize(12);
        orLabel.setPadding(0, dp(16), 0, dp(4));
        root.addView(orLabel);

        Button chooser = new Button(this);
        chooser.setText("Aus Konten-Liste wählen (funktioniert nicht auf jedem Gerät)");
        chooser.setAllCaps(false);
        chooser.setOnClickListener(v -> {
            try {
                Intent i = android.accounts.AccountManager.newChooseAccountIntent(
                        null, null, new String[]{DAVX5_ACCOUNT_TYPE}, null, null, null, null);
                startActivityForResult(i, REQ_ACCOUNT);
            } catch (Exception e) {
                Toast.makeText(this, "Keine Kontoauswahl verfügbar", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(chooser);

        setContentView(scroll);
    }

    private void finishAccountPick() {
        pendingAccountPick = false;
        startService(new Intent(this, EdgeService.class).putExtra("open", true));
        finish();
    }

    // ---------- Icon-Auswahl fuer eine Registerkarte ----------
    // Mitgelieferter Icon-Satz (schnell, kein Datei-Picker noetig) ODER freie
    // Bildauswahl aus der Galerie ODER Standard-Icon des Kartentyps.

    private void pickIcon() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Icon wählen");
        title.setTextSize(22);
        root.addView(title);

        Button gallery = new Button(this);
        gallery.setText("Eigenes Bild aus der Galerie…");
        gallery.setOnClickListener(v -> pickIconFromGallery());
        root.addView(gallery);

        Button reset = new Button(this);
        reset.setText("Standard-Icon der Karte verwenden");
        reset.setOnClickListener(v -> {
            Tabs.update(this, pendingIconTabId, t -> { t.iconKey = null; t.iconPath = null; });
            finishIconPick();
        });
        root.addView(reset);

        TextView sub = new TextView(this);
        sub.setText("\nOder ein mitgeliefertes Symbol:");
        sub.setTextSize(13);
        sub.setPadding(0, dp(8), 0, dp(4));
        root.addView(sub);

        android.widget.GridLayout grid = new android.widget.GridLayout(this);
        // Feste Zellbreite statt WRAP_CONTENT - sonst gibt GridLayout jeder
        // Spalte nur so viel Platz, wie das laengste Wort in Einzeilen-Breite
        // braucht, und laengere Beschriftungen ("Einkaufswagen") werden vom
        // ScrollView seitlich abgeschnitten statt umzubrechen.
        grid.setColumnCount(3);
        int cellW = dp(92);
        for (IconSet.Entry entry : IconSet.ALL) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            int cellPad = dp(8);
            cell.setPadding(cellPad, cellPad, cellPad, cellPad);
            android.widget.GridLayout.LayoutParams glp = new android.widget.GridLayout.LayoutParams();
            glp.width = cellW;
            cell.setLayoutParams(glp);

            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setImageResource(entry.res);
            iv.setColorFilter(Color.parseColor("#DDDDDD"));
            int s = dp(32);
            iv.setLayoutParams(new LinearLayout.LayoutParams(s, s));
            cell.addView(iv);

            TextView label = new TextView(this);
            label.setText(entry.label);
            label.setTextSize(10.5f);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            cell.addView(label);

            cell.setOnClickListener(new IconPickClick(this, entry.key));
            grid.addView(cell);
        }
        root.addView(grid);

        setContentView(scroll);
    }

    private void pickIconFromGallery() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        try {
            startActivityForResult(i, REQ_ICON);
        } catch (Exception e) {
            Toast.makeText(this, "Keine Bildauswahl verfügbar", Toast.LENGTH_SHORT).show();
            finishIconPick();
        }
    }

    private void finishIconPick() {
        pendingIconTabId = null;
        startService(new Intent(this, EdgeService.class).putExtra("open", true));
        finish();
    }

    /** Liest das gewaehlte Bild, schneidet es quadratisch, verkleinert es und
     *  speichert es dauerhaft im App-Speicher (kein Zugriff auf eine fremde
     *  content://-URI noetig, die spaeter ungueltig werden koennte). */
    private String saveIcon(Uri uri) {
        try {
            java.io.InputStream in = getContentResolver().openInputStream(uri);
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
            if (in != null) in.close();
            if (bmp == null) return null;
            int side = Math.min(bmp.getWidth(), bmp.getHeight());
            android.graphics.Bitmap square = android.graphics.Bitmap.createBitmap(bmp,
                    (bmp.getWidth() - side) / 2, (bmp.getHeight() - side) / 2, side, side);
            int target = dp(96);
            android.graphics.Bitmap scaled =
                    android.graphics.Bitmap.createScaledBitmap(square, target, target, true);
            java.io.File dir = new java.io.File(getFilesDir(), "tab_icons");
            dir.mkdirs();
            java.io.File file = new java.io.File(dir, pendingIconTabId + ".png");
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            android.util.Log.w("EdgeTab", "Icon speichern fehlgeschlagen", e);
            return null;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_BIND) {
            if (res == RESULT_OK) afterBound();
            else { cancelPending(); Toast.makeText(this, "Binden abgebrochen", Toast.LENGTH_SHORT).show(); showWidgetPicker(); }
        } else if (req == REQ_CONFIGURE) {
            if (res == RESULT_OK) finishPick();
            else { cancelPending(); showWidgetPicker(); }
        } else if (req == REQ_ICON) {
            if (res == RESULT_OK && data != null && data.getData() != null) {
                final String path = saveIcon(data.getData());
                if (path != null) {
                    Tabs.update(this, pendingIconTabId, t -> { t.iconPath = path; t.iconKey = null; });
                } else {
                    Toast.makeText(this, "Bild konnte nicht gelesen werden", Toast.LENGTH_SHORT).show();
                }
            }
            finishIconPick();
        } else if (req == REQ_ACCOUNT) {
            if (res == RESULT_OK && data != null
                    && data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME) != null) {
                String name = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME);
                String type = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_TYPE);
                de.herbers.edgetab.Settings.setJtxAccount(this, name, type != null ? type : DAVX5_ACCOUNT_TYPE);
                finishAccountPick();
            } else {
                // Abgebrochen oder keine passenden Konten sichtbar (siehe
                // Kommentar bei showJtxAccountSetup) - zurueck zum
                // Einrichtungsbildschirm, damit die manuelle Eingabe bleibt.
                showJtxAccountSetup();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(req, permissions, grantResults);
        if (req == REQ_PERMISSION) {
            // Egal ob erlaubt oder abgelehnt - die jeweilige Karte prueft die
            // Berechtigung beim Aufbau selbst neu und zeigt dann Inhalt oder
            // Hinweis. Leiste einfach wieder aufziehen.
            pendingPermissions = null;
            startService(new Intent(this, EdgeService.class).putExtra("open", true));
            finish();
        }
    }

    private String appLabel(android.content.pm.PackageManager pm, String pkg) {
        try { return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }

    /** Klick auf einen Widget-Anbieter (statisch/benannt wegen d8). */
    private static final class ProviderClick implements View.OnClickListener {
        private final MainActivity act;
        private final AppWidgetProviderInfo info;
        ProviderClick(MainActivity act, AppWidgetProviderInfo info) { this.act = act; this.info = info; }
        public void onClick(View v) { act.beginBind(info); }
    }

    /** Klick auf ein mitgeliefertes Icon im Auswahlraster (statisch/benannt wegen d8). */
    private static final class IconPickClick implements View.OnClickListener {
        private final MainActivity act;
        private final String key;
        IconPickClick(MainActivity act, String key) { this.act = act; this.key = key; }
        public void onClick(View v) {
            Tabs.update(act, act.pendingIconTabId, t -> { t.iconKey = key; t.iconPath = null; });
            act.finishIconPick();
        }
    }
}
