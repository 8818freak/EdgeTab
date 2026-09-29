package de.herbers.edgetab;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Die eigene Ablage der abgefangenen Benachrichtigungen. Bewusst schlicht:
 * eine Tabelle, Einfuegen beim Eintreffen, als-gelesen-Markieren, Auslesen fuer
 * die Kachel. Genau das, was der Nutzer mit "die eh kommenden Nachrichten und
 * deren Status speichern, sonst nichts" gemeint hat.
 */
public class NotificationStore extends SQLiteOpenHelper {

    private static final String DB = "edgetab_notifications.db";
    private static final int VERSION = 4;
    private static NotificationStore instance;

    public static synchronized NotificationStore get(Context ctx) {
        if (instance == null) {
            instance = new NotificationStore(ctx.getApplicationContext());
        }
        return instance;
    }

    private NotificationStore(Context ctx) {
        super(ctx, DB, null, VERSION);
    }

    /** Datei der Ablage auf der Platte - fuer die Sicherung (Backup kopiert die
     *  Datei direkt, statt jede Benachrichtigung einzeln zu exportieren). */
    public static File dbFile(Context ctx) {
        return ctx.getApplicationContext().getDatabasePath(DB);
    }

    /** Offene Instanz schliessen (WAL vorher checkpointen), damit eine Datei-
     *  Kopie/-Ersetzung konsistent ist. Die naechste get()-Anfrage oeffnet neu.
     *  Fuer Sichern (Kopie muss vollstaendig sein) UND Wiederherstellen (Datei
     *  darf nicht offen sein, wenn sie ersetzt wird) - wie Suchers SearchStore. */
    public static synchronized void closeForBackup() {
        if (instance != null) {
            try { instance.getWritableDatabase().execSQL("PRAGMA wal_checkpoint(FULL)"); } catch (Exception ignored) {}
            instance.close();
            instance = null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE notes (" +
            "  _id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  nkey TEXT," +           // Android-Schluessel der Benachrichtigung
            "  pkg TEXT NOT NULL," +   // Herkunfts-App
            "  title TEXT," +
            "  text TEXT," +
            "  posted INTEGER," +      // Zeitstempel
            "  seen INTEGER DEFAULT 0," + // 0 = ungelesen, 1 = gelesen
            "  channel TEXT," +       // Notification-Channel-ID der Quell-App
            "  channel_name TEXT," +  // deren vom Nutzer sichtbarer Name, falls auslesbar
            "  info_json TEXT," +     // vollstaendiger, verlustfreier Abzug der Benachrichtigung (Notifications.toJson)
            "  sent_replies TEXT," +  // aus EdgeTab gesendete Antworten (Zeitstempel\ttext je Zeile) - baut einen Verlauf
            "  conversational INTEGER DEFAULT 0" + // 1 = Chat/Mail (Notifications.isConversational) - Inhalt vor Entwertung schuetzen
            ")");
        db.execSQL("CREATE INDEX idx_pkg ON notes(pkg)");
        db.execSQL("CREATE INDEX idx_posted ON notes(posted)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN channel TEXT");
            db.execSQL("ALTER TABLE notes ADD COLUMN channel_name TEXT");
        }
        if (oldV < 3) {
            // Vollstaendiger Benachrichtigungs-Abzug. Alt-Eintraege haben ihn
            // nicht (nur was ab jetzt eintrifft) - Spalte bleibt dort NULL.
            db.execSQL("ALTER TABLE notes ADD COLUMN info_json TEXT");
            db.execSQL("ALTER TABLE notes ADD COLUMN sent_replies TEXT");
        }
        if (oldV < 4) {
            db.execSQL("ALTER TABLE notes ADD COLUMN conversational INTEGER DEFAULT 0");
        }
    }

    /** Neue Benachrichtigung ablegen. Gleicher Schluessel = Aktualisierung.
     *  conversational: ob die Benachrichtigung Chat/Mail ist (Notifications.
     *  isConversational) - nur dann greift der Schutz vor entwertenden Neuposts. */
    public void add(String key, String pkg, String title, String text, long posted,
                     String channel, String channelName, String infoJson, boolean conversational) {
        SQLiteDatabase db = getWritableDatabase();
        // Doppelte desselben Schluessels vermeiden: vorhandene ersetzen,
        // dabei den Gelesen-Status nicht ueberschreiben.
        if (key != null) {
            Cursor c = db.rawQuery("SELECT _id, text, conversational FROM notes WHERE nkey=? LIMIT 1", new String[]{key});
            boolean exists = c.moveToFirst();
            String oldText = exists ? c.getString(1) : null;
            boolean wasConversational = exists && c.getInt(2) != 0;
            c.close();
            if (exists) {
                ContentValues v = new ContentValues();
                // Rueckschritt-Sperre: Bei Chat/Mail darf ein Neu-Post desselben
                // Schluessels die gespeicherte echte Nachricht NICHT durch einen
                // blossen Bestaetigungs-/Leertext ersetzen. Manche Apps
                // (BlackBerry Hub/BBMe) posten nach dem Antworten "Geantwortet."
                // (ohne Titel) neu - das wuerde sonst die Originalnachricht
                // entwerten. ECHTE neue Nachrichten (anderer, substanzieller Text)
                // duerfen weiter aktualisieren - so gehen Konversationen, die sich
                // einen Schluessel teilen, nicht verloren.
                boolean downgrade = (wasConversational || conversational)
                        && oldText != null && !oldText.isEmpty()
                        && !de.herbers.common.Notifications.looksLikeReplyConfirmation(null, oldText)
                        && de.herbers.common.Notifications.looksLikeReplyConfirmation(title, text);
                if (!downgrade) {
                    v.put("title", title);
                    v.put("text", text);
                    v.put("channel", channel);
                    v.put("channel_name", channelName);
                    v.put("conversational", conversational ? 1 : 0);
                }
                v.put("posted", posted);
                if (infoJson != null && !infoJson.isEmpty()) v.put("info_json", infoJson);
                db.update("notes", v, "nkey=?", new String[]{key});
                return;
            }
            // Dieselbe Nachricht unter neuem Schluessel neu gepostet (BlackBerry
            // Hub tut das): vorhandene Zeile uebernehmen statt zu verdoppeln.
            Cursor d = db.rawQuery("SELECT _id FROM notes WHERE pkg=? AND title=? AND text=?"
                    + " ORDER BY posted DESC LIMIT 1",
                    new String[]{pkg, title == null ? "" : title, text == null ? "" : text});
            long dupId = d.moveToFirst() ? d.getLong(0) : -1;
            d.close();
            if (dupId >= 0) {
                ContentValues v = new ContentValues();
                v.put("nkey", key);
                v.put("posted", posted);
                if (infoJson != null && !infoJson.isEmpty()) v.put("info_json", infoJson);
                db.update("notes", v, "_id=?", new String[]{String.valueOf(dupId)});
                return;
            }
        }
        ContentValues v = new ContentValues();
        v.put("nkey", key);
        v.put("pkg", pkg);
        v.put("title", title);
        v.put("text", text);
        v.put("posted", posted);
        v.put("seen", 0);
        v.put("channel", channel);
        v.put("channel_name", channelName);
        v.put("info_json", infoJson);
        v.put("conversational", conversational ? 1 : 0);
        db.insert("notes", null, v);
    }

    public static class Item {
        public long id;
        public String nkey, pkg, title, text;
        public long posted;
        public boolean seen;
        public String channel, channelName;
        public String infoJson;   // vollstaendiger Abzug (kann null sein bei Alt-Eintraegen)
        public String sentReplies; // aus EdgeTab gesendete Antworten (Zeitstempel\ttext je Zeile), kann null sein
    }

    /** Die neuesten Eintraege fuer die Kachel, optional auf eine App gefiltert. */
    public List<Item> recent(int limit, String pkgFilter) {
        List<Item> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String where = pkgFilter == null ? null : "pkg=?";
        String[] args = pkgFilter == null ? null : new String[]{pkgFilter};
        Cursor c = db.query("notes", null, where, args, null, null,
                "posted DESC", String.valueOf(limit));
        while (c.moveToNext()) {
            Item it = new Item();
            it.id     = c.getLong(c.getColumnIndexOrThrow("_id"));
            it.nkey   = c.getString(c.getColumnIndexOrThrow("nkey"));
            it.pkg    = c.getString(c.getColumnIndexOrThrow("pkg"));
            it.title  = c.getString(c.getColumnIndexOrThrow("title"));
            it.text   = c.getString(c.getColumnIndexOrThrow("text"));
            it.posted = c.getLong(c.getColumnIndexOrThrow("posted"));
            it.seen   = c.getInt(c.getColumnIndexOrThrow("seen")) != 0;
            it.channel = c.getString(c.getColumnIndexOrThrow("channel"));
            it.channelName = c.getString(c.getColumnIndexOrThrow("channel_name"));
            it.infoJson = c.getString(c.getColumnIndexOrThrow("info_json"));
            it.sentReplies = c.getString(c.getColumnIndexOrThrow("sent_replies"));
            out.add(it);
        }
        c.close();
        return out;
    }

    /** Zeilenform fuer die Kanal-Auswahl in den Einstellungen: Kanal-ID +
     *  lesbarer Name (falls beim Empfang auslesbar), je einmal pro App. */
    public static class Channel {
        public String id, name;
        public Channel(String id, String name) { this.id = id; this.name = name; }
    }

    /** Die distinkten Benachrichtigungs-Kanaele einer App - nur sinnvoll,
     *  wenn eine App mehrere Arten von Meldungen ueber getrennte Kanaele
     *  schickt (z.B. eBay: "Nachrichten" vs. "Neue Artikel"). */
    public List<Channel> distinctChannels(String pkg) {
        List<Channel> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT channel, MAX(channel_name), COUNT(*) FROM notes "
                + "WHERE pkg=? AND channel IS NOT NULL AND channel != '' "
                + "GROUP BY channel ORDER BY COUNT(*) DESC", new String[]{pkg});
        while (c.moveToNext()) {
            String name = c.getString(1);
            out.add(new Channel(c.getString(0), name == null || name.isEmpty() ? c.getString(0) : name));
        }
        c.close();
        return out;
    }

    /** Eintraege loeschen, die aelter als die eingestellte Frist sind. */
    public int pruneOlderThan(long cutoffMillis) {
        return getWritableDatabase().delete("notes", "posted < ?",
                new String[]{ String.valueOf(cutoffMillis) });
    }

    /** Einen Eintrag aus der Ablage entfernen (nach dem Loeschen). */
    public void delete(long id) {
        getWritableDatabase().delete("notes", "_id=?", new String[]{String.valueOf(id)});
    }

    /** Alle erfassten Benachrichtigungen aus der Ablage loeschen. */
    public void clearAll() {
        getWritableDatabase().delete("notes", null, null);
    }

    public int unseenCount(String pkgFilter) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM notes WHERE seen=0";
        String[] args = null;
        if (pkgFilter != null) { sql += " AND pkg=?"; args = new String[]{pkgFilter}; }
        Cursor c = db.rawQuery(sql, args);
        int n = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return n;
    }

    /** Alle Apps, von denen je eine Benachrichtigung kam - fuer die Quellenauswahl. */
    public java.util.List<String> distinctPackages() {
        java.util.List<String> out = new java.util.ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT pkg, COUNT(*) FROM notes GROUP BY pkg ORDER BY COUNT(*) DESC", null);
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        return out;
    }

    /** Alte Doppel (gleiche App/Titel/Text) auf den neuesten Eintrag eindampfen. */
    public void collapseDuplicates() {
        getWritableDatabase().execSQL("DELETE FROM notes WHERE _id NOT IN ("
                + "SELECT MAX(_id) FROM notes GROUP BY pkg, title, text)");
    }

    /** Eine aus EdgeTab gesendete Antwort zum Eintrag festhalten - wird unter
     *  der Nachricht angezeigt und baut so einen Verlauf auf. Format je Zeile:
     *  Zeitstempel(ms) \t Text. Setzt zugleich das "geantwortet"-Kennzeichen,
     *  das die Originalnachricht vor Ueberschreiben schuetzt (siehe add()). */
    public void addReply(long id, String text) {
        if (text == null || text.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        String old = null;
        Cursor c = db.rawQuery("SELECT sent_replies FROM notes WHERE _id=?",
                new String[]{String.valueOf(id)});
        if (c.moveToFirst()) old = c.getString(0);
        c.close();
        String line = System.currentTimeMillis() + "\t" + text.replace("\n", " ");
        String merged = (old == null || old.isEmpty()) ? line : old + "\n" + line;
        ContentValues v = new ContentValues();
        v.put("sent_replies", merged);
        db.update("notes", v, "_id=?", new String[]{String.valueOf(id)});
    }

    public void markSeen(long id) {
        ContentValues v = new ContentValues();
        v.put("seen", 1);
        getWritableDatabase().update("notes", v, "_id=?", new String[]{String.valueOf(id)});
    }

    public void markAllSeen(String pkgFilter) {
        ContentValues v = new ContentValues();
        v.put("seen", 1);
        String where = pkgFilter == null ? "seen=0" : "seen=0 AND pkg=?";
        String[] args = pkgFilter == null ? null : new String[]{pkgFilter};
        getWritableDatabase().update("notes", v, where, args);
    }
}
