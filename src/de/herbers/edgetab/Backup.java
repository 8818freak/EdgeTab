package de.herbers.edgetab;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Sichert/liest Einstellungen UND die Daten (die SQLite-Datenbank des
 * NotificationStore: erfasste Benachrichtigungen inkl. Gelesen-Status,
 * gesendeter Antworten und des vollstaendigen Abzugs info_json) als EINE
 * Zip-Datei - wie Suchers Backup. Ein reiner Text-Export der Einstellungen
 * wuerde die eigentlich wertvollen Daten (den Posteingang-Verlauf) nicht
 * mitnehmen; nach einer Neuinstallation waere er verloren.
 *
 * Kein externes Zip-Format/-Bibliothek noetig - java.util.zip ist Teil des JDK.
 * Der Import erkennt auch aeltere, reine Text-Sicherungen (nur Einstellungen)
 * und spielt diese weiter ein.
 */
final class Backup {

    private static final String ENTRY_SETTINGS = "settings.txt";
    private static final String ENTRY_DB = "notifications.db";

    private Backup() {}

    /** Einstellungen + Benachrichtigungs-DB in einen Zip-Strom schreiben. */
    static void exportZip(Context ctx, OutputStream out) throws IOException {
        // Vor dem Kopieren schliessen (Checkpoint), damit die Datei-Kopie einen
        // konsistenten, vollstaendigen Stand hat (siehe NotificationStore).
        NotificationStore.closeForBackup();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry(ENTRY_SETTINGS));
            zos.write(Settings.exportText(ctx).getBytes("UTF-8"));
            zos.closeEntry();

            File db = NotificationStore.dbFile(ctx);
            if (db.exists()) {
                // STORED (unkomprimiert): die Ablage ist meist klein, und STORED
                // spart die Kompression; verlangt aber Groesse+CRC32 vorab.
                ZipEntry entry = new ZipEntry(ENTRY_DB);
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(db.length());
                entry.setCompressedSize(db.length());
                entry.setCrc(crc32Of(db));
                zos.putNextEntry(entry);
                try (FileInputStream in = new FileInputStream(db)) { copy(in, zos); }
                zos.closeEntry();
            }
        }
    }

    /** Aus einem Strom wiederherstellen. Erkennt selbst, ob es eine neue
     *  Zip-Sicherung (beginnt mit "PK") oder eine alte reine Text-Sicherung
     *  (nur Einstellungen) ist. Liefert true bei Erfolg. */
    static boolean importAuto(Context ctx, InputStream in) throws IOException {
        byte[] all = readAll(in);
        boolean isZip = all.length >= 2 && all[0] == 'P' && all[1] == 'K';
        if (isZip) return importZip(ctx, new ByteArrayInputStream(all));
        // Alte Text-Sicherung: nur Einstellungen.
        return Settings.importText(ctx, new String(all, "UTF-8"));
    }

    /** Ersetzt Einstellungen UND Benachrichtigungs-DB durch den Inhalt einer
     *  Zip-Sicherung. false bei fehlender settings.txt (falsches/beschaedigtes
     *  Format), ohne etwas zu aendern. Eine fehlende DB ist kein Fehler (dann
     *  bleibt die bisherige Ablage unangetastet). */
    static boolean importZip(Context ctx, InputStream in) throws IOException {
        String settingsText = null;
        File dbTarget = NotificationStore.dbFile(ctx);
        File tmpDb = new File(dbTarget.getParentFile(), dbTarget.getName() + ".importing");
        boolean gotDb = false;
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (ENTRY_SETTINGS.equals(e.getName())) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    copy(zis, bos);
                    settingsText = bos.toString("UTF-8");
                } else if (ENTRY_DB.equals(e.getName())) {
                    dbTarget.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(tmpDb)) { copy(zis, fos); }
                    gotDb = true;
                }
            }
        }
        if (settingsText == null) {
            if (tmpDb.exists()) tmpDb.delete();
            return false;
        }
        boolean ok = Settings.importText(ctx, settingsText);
        if (!ok) {
            if (tmpDb.exists()) tmpDb.delete();
            return false;
        }
        if (gotDb) {
            // Offene Instanz UND alte -wal/-shm-Reste weg, bevor die neue Datei
            // an ihre Stelle tritt (siehe NotificationStore.closeForBackup).
            NotificationStore.closeForBackup();
            new File(dbTarget.getParentFile(), dbTarget.getName() + "-wal").delete();
            new File(dbTarget.getParentFile(), dbTarget.getName() + "-shm").delete();
            dbTarget.delete();
            tmpDb.renameTo(dbTarget);
        }
        return true;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copy(in, bos);
        return bos.toByteArray();
    }

    private static long crc32Of(File f) throws IOException {
        CRC32 crc = new CRC32();
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) crc.update(buf, 0, n);
        }
        return crc.getValue();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }
}
