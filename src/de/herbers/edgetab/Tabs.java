package de.herbers.edgetab;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Verzeichnis und Speicherung der Registerkarten. Registerkarten sind
 * Instanzen (TabInstance): von "widget" kann es beliebig viele geben, jede
 * mit eigenem Namen/Icon/Reihenfolge/Tippen-schliesst-Verhalten und - nur bei
 * Widget-Karten - einer eigenen Liste eingebetteter App-Widgets. Gespeichert
 * als ein JSON-Array in denselben Settings wie der Rest der App.
 */
final class Tabs {

    private Tabs() {}

    private static final String PREFS = "edgetab_settings"; // dieselbe Datei wie Settings
    private static final String K_TABS = "tabs_json";

    /** Ids der sechs urspruenglichen Karten - nicht loeschbar, nur aus/an. */
    private static final List<String> LEGACY_IDS = Arrays.asList(
            "calendar", "inbox", "widget", "tasks", "notes", "contacts");

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- Laden / Speichern ----

    static synchronized List<TabInstance> load(Context ctx) {
        String raw = p(ctx).getString(K_TABS, null);
        if (raw == null) {
            List<TabInstance> seeded = migrate(ctx);
            save(ctx, seeded);
            return seeded;
        }
        List<TabInstance> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                out.add(TabInstance.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            return migrate(ctx);
        }
        // Die Suche-Karte ist wieder raus (2026-09-24, zur eigenstaendigen
        // App "Sucher" herausgeloest) - bei bestehenden Installationen, die
        // 0.60 schon hatten, den alten Eintrag entfernen statt ihn als
        // "Unbekannter Kartentyp"-Platzhalter anzuzeigen.
        boolean changed = out.removeIf(t -> "search".equals(t.type));
        if (changed) save(ctx, out);
        return out;
    }

    static synchronized void save(Context ctx, List<TabInstance> list) {
        JSONArray arr = new JSONArray();
        try {
            for (TabInstance t : list) arr.put(t.toJson());
        } catch (JSONException ignored) {}
        p(ctx).edit().putString(K_TABS, arr.toString()).apply();
    }

    /** Erststart oder Umstieg von der alten (bis 0.16) Speicherung: sechs
     *  feste Karten anlegen, dabei alte Reihenfolge/Auswahl und das alte,
     *  einzelne eingebettete Widget uebernehmen, damit niemand etwas verliert. */
    private static List<TabInstance> migrate(Context ctx) {
        List<TabInstance> list = new ArrayList<>();
        list.add(new TabInstance("calendar", TabInstance.TYPE_CALENDAR));
        list.add(new TabInstance("inbox", TabInstance.TYPE_INBOX));
        TabInstance widget = new TabInstance("widget", TabInstance.TYPE_WIDGET);
        int oldId = Settings.widgetId(ctx);
        String oldProvider = Settings.widgetProvider(ctx);
        if (oldId > 0 && oldProvider != null) {
            widget.widgets.add(new TabInstance.WidgetRef(oldId, oldProvider));
        }
        list.add(widget);
        list.add(new TabInstance("tasks", TabInstance.TYPE_TASKS));
        list.add(new TabInstance("notes", TabInstance.TYPE_NOTES));
        list.add(new TabInstance("contacts", TabInstance.TYPE_CONTACTS));

        Set<String> ids = new LinkedHashSet<>(LEGACY_IDS);
        List<String> order = Settings.tabOrder(ctx, ids);
        List<TabInstance> ordered = new ArrayList<>();
        for (String id : order) {
            for (TabInstance t : list) {
                if (t.id.equals(id)) { ordered.add(t); break; }
            }
        }
        for (TabInstance t : list) if (!ordered.contains(t)) ordered.add(t);
        for (TabInstance t : ordered) t.enabled = Settings.isTabEnabled(ctx, t.id);
        return ordered;
    }

    // ---- Live-Tab-Objekte fuer die Leiste ----

    /** Nur die aktiven Karten, in Anzeige-Reihenfolge. */
    static List<Tab> buildActive(Context ctx) {
        List<Tab> out = new ArrayList<>();
        for (TabInstance t : load(ctx)) if (t.enabled) out.add(build(ctx, t));
        return out;
    }

    static Tab build(Context ctx, TabInstance t) {
        switch (t.type) {
            case TabInstance.TYPE_CALENDAR: return new CalendarTab(t);
            case TabInstance.TYPE_INBOX:    return new InboxTab(t);
            case TabInstance.TYPE_WIDGET:   return new WidgetTab(t);
            case TabInstance.TYPE_TASKS:    return new TasksTab(t);
            case TabInstance.TYPE_NOTES:    return new NotesTab(t);
            case TabInstance.TYPE_CONTACTS: return new ContactsTab(t);
            case TabInstance.TYPE_SHORTCUTS: return new ShortcutsTab(t);
            case TabInstance.TYPE_MEDIA:     return new MediaTab(t);
            default: return new PlaceholderTab(t, t.type, R.drawable.ic_widget, ctx.getString(R.string.unknown_card_hint));
        }
    }

    // ---- Bearbeiten (fuer die Einstellungen-Karte) ----

    interface Editor { void edit(TabInstance t); }

    static void update(Context ctx, String id, Editor editor) {
        List<TabInstance> list = load(ctx);
        for (TabInstance t : list) {
            if (t.id.equals(id)) { editor.edit(t); break; }
        }
        save(ctx, list);
    }

    static void move(Context ctx, String id, int delta) {
        List<TabInstance> list = load(ctx);
        int i = indexOf(list, id);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= list.size()) return;
        TabInstance tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
        save(ctx, list);
    }

    private static int indexOf(List<TabInstance> list, String id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(id)) return i;
        return -1;
    }

    static TabInstance addWidgetTab(Context ctx) {
        List<TabInstance> list = load(ctx);
        TabInstance t = new TabInstance("widget_" + System.currentTimeMillis(), TabInstance.TYPE_WIDGET);
        list.add(t);
        save(ctx, list);
        return t;
    }

    static TabInstance addShortcutsTab(Context ctx) {
        List<TabInstance> list = load(ctx);
        TabInstance t = new TabInstance("shortcuts_" + System.currentTimeMillis(), TabInstance.TYPE_SHORTCUTS);
        list.add(t);
        save(ctx, list);
        return t;
    }

    static TabInstance addMediaTab(Context ctx) {
        List<TabInstance> list = load(ctx);
        TabInstance t = new TabInstance("media_" + System.currentTimeMillis(), TabInstance.TYPE_MEDIA);
        list.add(t);
        save(ctx, list);
        return t;
    }

    /** Nur zusaetzlich angelegte Widget-Karten lassen sich wieder loeschen -
     *  die sechs urspruenglichen nur aus-/einschalten. */
    static boolean isDeletable(String id) { return !LEGACY_IDS.contains(id); }

    static void remove(Context ctx, String id) {
        if (!isDeletable(id)) return;
        List<TabInstance> list = load(ctx);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) { list.remove(i); break; }
        }
        save(ctx, list);
    }
}
