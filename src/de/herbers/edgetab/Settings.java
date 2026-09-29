package de.herbers.edgetab;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Alle Einstellungen der Leiste an einer Stelle. Deckt ab, was der
 * Original-Dialog bot - Seite, Position, Hoehe, Transparenz - plus die
 * anpassbare Breite (die das Original nicht hatte) und die Karten-Auswahl.
 */
public final class Settings {

    private static final String PREFS = "edgetab_settings";

    private static final String K_EDGE_RIGHT   = "edge_right";      // Seite
    private static final String K_HANDLE_HEIGHT= "handle_height";   // Griffhoehe in dp
    private static final String K_HANDLE_POS    = "handle_pos";      // -100..100 (vertikal)
    private static final String K_PANEL_WIDTH   = "panel_width";     // Panelbreite in dp
    private static final String K_TRANSPARENCY  = "transparency";    // 0..100
    private static final String K_ICON_SIZE     = "icon_size";       // Registerkarten-Icons, dp
    private static final String K_TAB_ORDER     = "tab_order";       // ids, kommagetrennt
    private static final String K_TAB_DISABLED  = "tab_disabled";    // ids, die AUS sind
    private static final String K_SOURCES       = "sources";         // Benachrichtigungsquellen
    private static final String K_FONT_SCALE    = "font_scale";       // Schriftgroesse in %
    private static final String K_SHOW_HEADER    = "show_header";      // Kopfzeile Uhr/Datum
    private static final String K_SHOW_BATTERY    = "show_battery";     // Akkuanzeige
    private static final String K_RETENTION_DAYS  = "retention_days";   // Aufbewahrung, 0 = unbegrenzt
    private static final String K_WIDGET_ID       = "widget_id";        // eingebettetes App-Widget
    private static final String K_WIDGET_PROVIDER = "widget_provider";  // dessen Provider (flach)
    private static final String K_JTX_ACCOUNT_NAME = "jtx_account_name"; // DAVx5-Konto fuer die Aufgaben-Karte
    private static final String K_JTX_ACCOUNT_TYPE = "jtx_account_type";
    private static final String K_MEDIA_CTRL_POS = "media_controls_pos"; // "top"/"middle"/"bottom"
    private static final String K_DISABLED_CHANNELS = "disabled_channels"; // "pkgchannelId"
    private static final String K_CONTACT_DISPLAY = "contact_display"; // "primary"/"alternative"
    private static final String K_CONTACT_SORT    = "contact_sort";    // "primary"/"alternative"
    // Karte "Aktive Kacheln" (Widget 2) - liest ihre Daten aus der App Active
    // Frames, zeichnet die Kacheln aber selbst (natives, zuverlaessiges Scrollen).
    private static final String K_FRAMES_COLUMNS  = "frames_columns";
    private static final String K_FRAMES_TILE_H   = "frames_tile_h";
    private static final String K_FRAMES_SCROLL   = "frames_scroll";
    private static final String K_FRAMES_MAX      = "frames_max";
    private static final String K_FRAMES_BIG_ROWS = "frames_big_rows";
    private static final String K_FRAMES_SHORT_PCT= "frames_short_pct";

    private Settings() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- Kopfzeile (Uhr, Datum, Wochentag) ----
    public static boolean showHeader(Context c) { return p(c).getBoolean(K_SHOW_HEADER, true); }
    public static void setShowHeader(Context c, boolean on) { p(c).edit().putBoolean(K_SHOW_HEADER, on).apply(); }

    // SMS/Anrufe zusaetzlich im Posteingang anzeigen (die eigenen Karten bleiben
    // davon unabhaengig). Standard AUS - der Posteingang bleibt sonst wie gewohnt.
    public static boolean smsInInbox(Context c) { return p(c).getBoolean("sms_in_inbox", false); }
    public static void setSmsInInbox(Context c, boolean on) { p(c).edit().putBoolean("sms_in_inbox", on).apply(); }
    public static boolean callsInInbox(Context c) { return p(c).getBoolean("calls_in_inbox", false); }
    public static void setCallsInInbox(Context c, boolean on) { p(c).edit().putBoolean("calls_in_inbox", on).apply(); }
    // Nachrichten zu Konversationen (Threads) gruppieren und aufklappbar machen.
    public static boolean groupConversations(Context c) { return p(c).getBoolean("group_conversations", true); }
    public static void setGroupConversations(Context c, boolean on) { p(c).edit().putBoolean("group_conversations", on).apply(); }

    // ---- Akkuanzeige in der Kopfzeile ----
    public static boolean showBattery(Context c) { return p(c).getBoolean(K_SHOW_BATTERY, true); }
    public static void setShowBattery(Context c, boolean on) { p(c).edit().putBoolean(K_SHOW_BATTERY, on).apply(); }

    // ---- Schriftgroesse ----  100 = normal, Bereich 80..150 (%)
    public static float fontScale(Context c) {
        return p(c).getInt(K_FONT_SCALE, 100) / 100f;
    }
    public static int fontScalePercent(Context c) { return p(c).getInt(K_FONT_SCALE, 100); }
    public static void setFontScale(Context c, int percent) {
        p(c).edit().putInt(K_FONT_SCALE, clamp(percent, 80, 150)).apply();
    }

    // ---- Seite ----
    public static boolean edgeRight(Context c) { return p(c).getBoolean(K_EDGE_RIGHT, true); }
    public static void setEdgeRight(Context c, boolean r) { p(c).edit().putBoolean(K_EDGE_RIGHT, r).apply(); }

    // ---- Griffhoehe (dp) ----  Standard 160, Bereich ~80..320
    public static int handleHeight(Context c) { return p(c).getInt(K_HANDLE_HEIGHT, 160); }
    public static void setHandleHeight(Context c, int dp) { p(c).edit().putInt(K_HANDLE_HEIGHT, clamp(dp,80,320)).apply(); }

    // ---- vertikale Position (-100 oben .. 0 Mitte .. 100 unten) ----
    public static int handlePos(Context c) { return p(c).getInt(K_HANDLE_POS, 0); }
    public static void setHandlePos(Context c, int v) { p(c).edit().putInt(K_HANDLE_POS, clamp(v,-100,100)).apply(); }

    // ---- Panelbreite (dp) ----  Standard 300, Bereich 220..420
    public static int panelWidth(Context c) { return p(c).getInt(K_PANEL_WIDTH, 300); }
    public static void setPanelWidth(Context c, int dp) { p(c).edit().putInt(K_PANEL_WIDTH, clamp(dp,220,420)).apply(); }

    // ---- Transparenz (0 = deckend .. 100 = sehr durchsichtig) ----
    public static int transparency(Context c) { return p(c).getInt(K_TRANSPARENCY, 7); }
    public static void setTransparency(Context c, int v) { p(c).edit().putInt(K_TRANSPARENCY, clamp(v,0,90)).apply(); }

    // ---- Icongroesse der Registerkarten-Spalte (dp) ----  Standard 28
    public static int iconSize(Context c) { return p(c).getInt(K_ICON_SIZE, 28); }
    public static void setIconSize(Context c, int dp) { p(c).edit().putInt(K_ICON_SIZE, clamp(dp,18,44)).apply(); }

    // ---- Karten-Reihenfolge ----
    public static List<String> tabOrder(Context c, Collection<String> knownIds) {
        String raw = p(c).getString(K_TAB_ORDER, null);
        List<String> order = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            order.addAll(Arrays.asList(raw.split(",")));
        }
        // Alle bekannten, noch nicht gelisteten Karten hinten anhaengen -
        // so tauchen neue Funktionen automatisch auf.
        for (String id : knownIds) if (!order.contains(id)) order.add(id);
        order.retainAll(new LinkedHashSet<>(knownIds));
        return order;
    }

    public static void setTabOrder(Context c, List<String> order) {
        p(c).edit().putString(K_TAB_ORDER, String.join(",", order)).apply();
    }

    // ---- Karte an/aus ----  (Standard: alle an)
    public static boolean isTabEnabled(Context c, String id) {
        return !disabledSet(c).contains(id);
    }
    public static void setTabEnabled(Context c, String id, boolean on) {
        Set<String> d = disabledSet(c);
        if (on) d.remove(id); else d.add(id);
        p(c).edit().putStringSet(K_TAB_DISABLED, d).apply();
    }
    private static Set<String> disabledSet(Context c) {
        return new HashSet<>(p(c).getStringSet(K_TAB_DISABLED, Collections.<String>emptySet()));
    }

    // ---- Benachrichtigungsquellen (fuer den Posteingang) ----
    public static Set<String> sources(Context c) {
        return new HashSet<>(p(c).getStringSet(K_SOURCES, Collections.<String>emptySet()));
    }
    public static boolean isSourceEnabled(Context c, String pkg) { return sources(c).contains(pkg); }
    public static void setSourceEnabled(Context c, String pkg, boolean on) {
        Set<String> s = sources(c);
        if (on) s.add(pkg); else s.remove(pkg);
        p(c).edit().putStringSet(K_SOURCES, s).apply();
    }

    // ---- Einzelne Benachrichtigungs-Kanaele je Quell-App ausblenden -----
    // (z.B. bei eBay "Neue Artikel" abwaehlen, "Nachrichten" behalten) - eine
    // App bleibt in Settings.sources() an, nur bestimmte ihrer Kanaele werden
    // im Posteingang unterdrueckt. Fehlt der Kanal (Item.channel leer, z.B.
    // Alt-Eintraege vor 0.47), gilt er immer als eingeschaltet. ----
    public static Set<String> disabledChannels(Context c) {
        return new HashSet<>(p(c).getStringSet(K_DISABLED_CHANNELS, Collections.<String>emptySet()));
    }
    public static boolean isChannelEnabled(Context c, String pkg, String channel) {
        if (channel == null || channel.isEmpty()) return true;
        return !disabledChannels(c).contains(pkg + "" + channel);
    }
    public static void setChannelEnabled(Context c, String pkg, String channel, boolean on) {
        if (channel == null || channel.isEmpty()) return;
        Set<String> s = disabledChannels(c);
        String key = pkg + "" + channel;
        if (on) s.remove(key); else s.add(key);
        p(c).edit().putStringSet(K_DISABLED_CHANNELS, s).apply();
    }

    // ---- Kategorie je Quell-App (wie BlackBerry Hub+ Services' "Kategorie
    // fuer App auswaehlen"-Dialog) - rein organisatorisch: faerbt den Balken
    // im Posteingang und erlaubt dort einen Schnellfilter.
    // Gespeichert wird ein stabiler, SPRACHUNABHAENGIGER Schluessel (nicht das
    // Anzeigewort) - sonst wuerde eine in Deutsch gewaehlte Kategorie nach
    // einem Sprachwechsel des Geraets nicht mehr wiedererkannt (der Vergleich
    // laeuft ueberall per String-Gleichheit). Anzeige-Text kommt separat aus
    // categoryLabel(). ----
    public static final String[] CATEGORIES = {
        "communication", "news", "shopping", "finance",
        "productivity", "entertainment", "other"
    };
    public static String category(Context c, String pkg) {
        return migrateCategoryKey(p(c).getString("cat_" + pkg, null));
    }
    public static void setCategory(Context c, String pkg, String cat) {
        if (cat == null) p(c).edit().remove("cat_" + pkg).apply();
        else p(c).edit().putString("cat_" + pkg, cat).apply();
    }
    /** Alte, vor der Mehrsprachigkeit als deutsches Anzeigewort gespeicherte
     *  Kategorien auf die neuen stabilen Schluessel abbilden, damit bereits
     *  zugewiesene Kategorien nicht verloren gehen. */
    private static String migrateCategoryKey(String raw) {
        if (raw == null) return null;
        switch (raw) {
            case "Kommunikation": return "communication";
            case "Nachrichten":   return "news";
            case "Einkaufen":     return "shopping";
            case "Finanzen":      return "finance";
            case "Produktivität": return "productivity";
            case "Unterhaltung":  return "entertainment";
            case "Sonstiges":     return "other";
            default: return raw; // schon ein neuer Schluessel
        }
    }
    public static String categoryLabel(Context c, String key) {
        if (key == null) return null;
        switch (key) {
            case "communication": return c.getString(R.string.category_communication);
            case "news":          return c.getString(R.string.category_news);
            case "shopping":      return c.getString(R.string.category_shopping);
            case "finance":       return c.getString(R.string.category_finance);
            case "productivity":  return c.getString(R.string.category_productivity);
            case "entertainment": return c.getString(R.string.category_entertainment);
            default:              return c.getString(R.string.category_other);
        }
    }
    public static int categoryColor(String cat) {
        if (cat == null) return 0;
        switch (cat) {
            case "communication": return Color.parseColor("#2E9BE6");
            case "news":          return Color.parseColor("#F5A623");
            case "shopping":      return Color.parseColor("#3DA764");
            case "finance":       return Color.parseColor("#D0A72E");
            case "productivity":  return Color.parseColor("#8E6FD6");
            case "entertainment": return Color.parseColor("#E0559B");
            default:              return Color.parseColor("#8A8A8E");
        }
    }

    // ---- Aufbewahrungsdauer des Posteingangs (Tage, 0 = unbegrenzt) ----
    public static final int[] RETENTION_CHOICES = { 3, 7, 14, 30, 90, 0 };
    public static int retentionDays(Context c) { return p(c).getInt(K_RETENTION_DAYS, 30); }
    public static void setRetentionDays(Context c, int days) {
        p(c).edit().putInt(K_RETENTION_DAYS, days).apply();
    }
    public static String retentionLabel(Context c, int days) {
        if (days <= 0) return c.getString(R.string.retention_unlimited);
        if (days == 1) return c.getString(R.string.retention_one_day);
        return c.getString(R.string.retention_days, days);
    }

    // ---- Eingebettetes App-Widget (Widget-Karte) ----
    public static int widgetId(Context c) { return p(c).getInt(K_WIDGET_ID, 0); }
    public static String widgetProvider(Context c) { return p(c).getString(K_WIDGET_PROVIDER, null); }
    public static void setWidget(Context c, int id, String provider) {
        p(c).edit().putInt(K_WIDGET_ID, id).putString(K_WIDGET_PROVIDER, provider).apply();
    }
    public static void clearWidget(Context c) {
        p(c).edit().remove(K_WIDGET_ID).remove(K_WIDGET_PROVIDER).apply();
    }

    // ---- DAVx5-Konto fuer die Aufgaben-Karte (JTX Boards Content-Provider
    // ist kontobezogen - jede Abfrage braucht account_name/account_type,
    // genau wie DAVx5 sie beim Synchronisieren mitgibt). ----
    public static String jtxAccountName(Context c) { return p(c).getString(K_JTX_ACCOUNT_NAME, null); }
    public static String jtxAccountType(Context c) { return p(c).getString(K_JTX_ACCOUNT_TYPE, null); }
    public static void setJtxAccount(Context c, String name, String type) {
        p(c).edit().putString(K_JTX_ACCOUNT_NAME, name).putString(K_JTX_ACCOUNT_TYPE, type).apply();
    }
    public static void clearJtxAccount(Context c) {
        p(c).edit().remove(K_JTX_ACCOUNT_NAME).remove(K_JTX_ACCOUNT_TYPE).apply();
    }

    // ---- Feste Position der Mediensteuerung-Knoepfe (Zurueck/Play/Vor)
    // innerhalb der Mediensteuerung-Karte - unabhaengig vom Scrollen, damit
    // sie einhaendig an einer festen Stelle erreichbar bleiben (Mathias'
    // Wunsch: "manche oben, manche mittig, manche unten"). ----
    public static String mediaControlsPos(Context c) { return p(c).getString(K_MEDIA_CTRL_POS, "bottom"); }
    public static void setMediaControlsPos(Context c, String pos) {
        p(c).edit().putString(K_MEDIA_CTRL_POS, pos).apply();
    }

    // ---- Namensdarstellung/-sortierung in der Kontakte-Karte - unabhaengig
    // voneinander waehlbar (wie Androids eigene Kontakte-App): "primary" =
    // Vorname zuerst / nach Vorname, "alternative" = Nachname zuerst
    // ("Nachname, Vorname") / nach Nachname. Nutzt ContactsContract-Spalten,
    // die genau das schon fertig mitbringen - kein eigenes Namens-Parsing. ----
    public static String contactDisplay(Context c) { return p(c).getString(K_CONTACT_DISPLAY, "primary"); }
    public static void setContactDisplay(Context c, String v) { p(c).edit().putString(K_CONTACT_DISPLAY, v).apply(); }
    public static String contactSort(Context c) { return p(c).getString(K_CONTACT_SORT, "primary"); }
    public static void setContactSort(Context c, String v) { p(c).edit().putString(K_CONTACT_SORT, v).apply(); }

    // ---- Karte "Aktive Kacheln" (Widget 2) ----
    // Spalten (BB-Passport hatte 4; Standard 2 wie OS10). Kachelhoehe in dp
    // (mit der vom Raster bestimmten Breite ergibt das das Format). Scrollen:
    // hier IMMER zuverlaessig, weil die Leiste die Kacheln selbst zeichnet
    // (kein Launcher-Host) - aus = nur wenige Kacheln zeigen. Grosse obere
    // Reihen + Prozenthoehe der folgenden wie bei Widget 1.
    public static int framesColumns(Context c) { return clamp(p(c).getInt(K_FRAMES_COLUMNS, 2), 1, 5); }
    public static void setFramesColumns(Context c, int n) { p(c).edit().putInt(K_FRAMES_COLUMNS, clamp(n, 1, 5)).apply(); }
    public static int framesTileHeight(Context c) { return clamp(p(c).getInt(K_FRAMES_TILE_H, 150), 60, 320); }
    public static void setFramesTileHeight(Context c, int dp) { p(c).edit().putInt(K_FRAMES_TILE_H, clamp(dp, 60, 320)).apply(); }
    public static boolean framesScroll(Context c) { return p(c).getBoolean(K_FRAMES_SCROLL, true); }
    public static void setFramesScroll(Context c, boolean on) { p(c).edit().putBoolean(K_FRAMES_SCROLL, on).apply(); }
    public static int framesMaxTiles(Context c) { return clamp(p(c).getInt(K_FRAMES_MAX, 12), 2, 60); }
    public static void setFramesMaxTiles(Context c, int n) { p(c).edit().putInt(K_FRAMES_MAX, clamp(n, 2, 60)).apply(); }
    public static int framesBigRows(Context c) { return clamp(p(c).getInt(K_FRAMES_BIG_ROWS, 1), 0, 20); }
    public static void setFramesBigRows(Context c, int n) { p(c).edit().putInt(K_FRAMES_BIG_ROWS, clamp(n, 0, 20)).apply(); }
    public static int framesShortPct(Context c) { return clamp(p(c).getInt(K_FRAMES_SHORT_PCT, 65), 30, 100); }
    public static void setFramesShortPct(Context c, int n) { p(c).edit().putInt(K_FRAMES_SHORT_PCT, clamp(n, 30, 100)).apply(); }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    // ---- Sichern/Wiederherstellen ----------------------------------------
    // Generischer Dump/Restore der GESAMTEN edgetab_settings-Datei (nicht
    // nur der Felder in dieser Klasse) - Tabs.java nutzt absichtlich
    // dieselbe SharedPreferences-Datei, deckt also automatisch auch
    // Registerkarten (Reihenfolge, Namen, Icon-Pfade, Widget-/Verknuepfungs-
    // Zuordnung) mit ab, ohne dass hier jedes Feld einzeln nachgezogen werden
    // muss. Icon-Bilddateien selbst (unter tab_icons/) wandern NICHT mit -
    // die gespeicherten Pfade gelten nur auf demselben Geraet/derselben
    // Installation.
    private static final String BACKUP_HEADER = "EdgeTab-Backup 1";

    // Delegiert an die gemeinsame Bibliothek (de.herbers.common.SettingsBackup,
    // Git-Submodul common/) - dieselbe Logik wie in Sucher/ActiveFrames.
    public static String exportText(Context c) {
        return de.herbers.common.SettingsBackup.export(p(c), BACKUP_HEADER);
    }

    public static boolean importText(Context c, String text) {
        return de.herbers.common.SettingsBackup.importInto(p(c), BACKUP_HEADER, text);
    }
}
