package de.herbers.edgetab;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Eine konfigurierte Registerkarte: welcher Typ (Kalender/Posteingang/Widget/
 * Aufgaben/Notizen/Kontakte), ob an/aus, eigener Name/Icon, ob Tippen darin
 * die Leiste schliesst - und, nur bei Typ "widget", die Liste der
 * eingebetteten App-Widgets. Wird als JSON in den Settings gespeichert
 * (siehe Tabs.java). Von "widget" kann es beliebig viele Instanzen geben.
 */
final class TabInstance {

    static final String TYPE_CALENDAR  = "calendar";
    static final String TYPE_INBOX     = "inbox";
    static final String TYPE_WIDGET    = "widget";
    static final String TYPE_TASKS     = "tasks";
    static final String TYPE_NOTES     = "notes";
    static final String TYPE_CONTACTS  = "contacts";
    static final String TYPE_SHORTCUTS = "shortcuts";
    static final String TYPE_MEDIA     = "media";

    final String id;
    final String type;
    boolean enabled = true;
    String name;         // null/leer = Standardname des Typs
    String iconPath;      // eigenes Foto (Datei unter getFilesDir()) - hat Vorrang
    String iconKey;        // mitgeliefertes Icon (siehe IconSet) - sonst Standard des Typs
    boolean closeOnTap = true;
    final List<WidgetRef> widgets = new ArrayList<>();

    // Nur bei Typ "shortcuts":
    final List<ShortcutRef> shortcuts = new ArrayList<>();
    // Je Gruppe eigene Ansicht (Raster/Liste, Spaltenzahl) - Schluessel ist
    // der Gruppenname, "" = die Verknuepfungen ohne Gruppe.
    final Map<String, GroupSettings> groupSettings = new HashMap<>();

    // Nur bei Typ "contacts":
    String contactsMode = "favorites"; // "all" | "selected" | "favorites"
    final List<String> selectedContacts = new ArrayList<>(); // Lookup-Keys

    TabInstance(String id, String type) {
        this.id = id;
        this.type = type;
    }

    static final class WidgetRef {
        final int id;
        final String provider;
        WidgetRef(int id, String provider) { this.id = id; this.provider = provider; }
    }

    /** Eine App-Verknuepfung: Paket, eigener Name (sonst App-Name), Gruppe
     *  (leer/null = ohne Gruppe). */
    static final class ShortcutRef {
        String pkg;
        String label;
        String group;
        ShortcutRef(String pkg, String label, String group) {
            this.pkg = pkg; this.label = label; this.group = group;
        }
    }

    /** Ansicht einer einzelnen Gruppe (oder der ungruppierten Verknuepfungen)
     *  in einer Verknuepfungs-Karte. */
    static final class GroupSettings {
        boolean grid = true; // true = Raster, false = Liste
        int columns = 4;     // Rastergroesse, nur bei grid
    }

    /** Ansicht einer Gruppe lesen (nie null - Standardwerte, falls noch nicht
     *  eigens eingestellt). */
    GroupSettings settingsFor(String group) {
        GroupSettings gs = groupSettings.get(group);
        return gs != null ? gs : new GroupSettings();
    }

    /** Wie settingsFor, legt die Gruppe aber bei Bedarf an - fuer Aenderungen. */
    GroupSettings mutableSettingsFor(String group) {
        GroupSettings gs = groupSettings.get(group);
        if (gs == null) { gs = new GroupSettings(); groupSettings.put(group, gs); }
        return gs;
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("type", type);
        o.put("enabled", enabled);
        if (name != null) o.put("name", name);
        if (iconPath != null) o.put("iconPath", iconPath);
        if (iconKey != null) o.put("iconKey", iconKey);
        o.put("closeOnTap", closeOnTap);
        JSONArray w = new JSONArray();
        for (WidgetRef r : widgets) {
            JSONObject wo = new JSONObject();
            wo.put("id", r.id);
            wo.put("provider", r.provider);
            w.put(wo);
        }
        o.put("widgets", w);

        JSONArray s = new JSONArray();
        for (ShortcutRef r : shortcuts) {
            JSONObject so = new JSONObject();
            so.put("pkg", r.pkg);
            if (r.label != null) so.put("label", r.label);
            if (r.group != null) so.put("group", r.group);
            s.put(so);
        }
        o.put("shortcuts", s);

        JSONObject gs = new JSONObject();
        for (Map.Entry<String, GroupSettings> e : groupSettings.entrySet()) {
            JSONObject go = new JSONObject();
            go.put("grid", e.getValue().grid);
            go.put("columns", e.getValue().columns);
            gs.put(e.getKey(), go);
        }
        o.put("groupSettings", gs);

        o.put("contactsMode", contactsMode);
        JSONArray sc = new JSONArray();
        for (String key : selectedContacts) sc.put(key);
        o.put("selectedContacts", sc);
        return o;
    }

    static TabInstance fromJson(JSONObject o) throws JSONException {
        TabInstance t = new TabInstance(o.getString("id"), o.getString("type"));
        t.enabled = o.optBoolean("enabled", true);
        t.name = o.optString("name", null);
        t.iconPath = o.optString("iconPath", null);
        t.iconKey = o.optString("iconKey", null);
        t.closeOnTap = o.optBoolean("closeOnTap", true);
        JSONArray w = o.optJSONArray("widgets");
        if (w != null) {
            for (int i = 0; i < w.length(); i++) {
                JSONObject wo = w.getJSONObject(i);
                t.widgets.add(new WidgetRef(wo.getInt("id"), wo.optString("provider", null)));
            }
        }
        JSONArray s = o.optJSONArray("shortcuts");
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                JSONObject so = s.getJSONObject(i);
                t.shortcuts.add(new ShortcutRef(so.getString("pkg"),
                        so.optString("label", null), so.optString("group", null)));
            }
        }
        JSONObject gs = o.optJSONObject("groupSettings");
        if (gs != null) {
            Iterator<String> keys = gs.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject go = gs.getJSONObject(key);
                GroupSettings gsE = new GroupSettings();
                gsE.grid = go.optBoolean("grid", true);
                gsE.columns = go.optInt("columns", 4);
                t.groupSettings.put(key, gsE);
            }
        }
        t.contactsMode = o.optString("contactsMode", "favorites");
        JSONArray sc = o.optJSONArray("selectedContacts");
        if (sc != null) {
            for (int i = 0; i < sc.length(); i++) t.selectedContacts.add(sc.getString(i));
        }
        return t;
    }
}
