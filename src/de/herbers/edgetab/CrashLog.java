package de.herbers.edgetab;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Schreibt Abstuerze (und ausgewaehlte Diagnose-Meldungen) in eine kleine
 *  Datei im app-internen Speicher, damit die Fehlermeldung nach einem Absturz
 *  nicht verloren geht - sie liegt sonst nur fluechtig im Systemprotokoll
 *  (logcat/DropBox) und ist ohne Kabel/adb kaum einzusehen. Der globale
 *  Absturz-Handler in {@link EdgeApp} ruft {@link #save} auf; die
 *  "Ueber EdgeTab"-Seite zeigt den Inhalt an. Bewusst winzig gehalten (nur der
 *  jeweils juengste Teil, siehe MAX_BYTES). */
final class CrashLog {

    private CrashLog() {}

    private static final String FILE = "crash.log";
    private static final int MAX_BYTES = 64 * 1024;

    static synchronized void save(Thread t, Throwable e, Context ctx) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("==== " + stamp() + "  (Thread: " + (t != null ? t.getName() : "?") + ") ====");
        if (e != null) e.printStackTrace(pw);
        pw.flush();
        append(ctx, sw.toString() + "\n");
    }

    static synchronized void log(Context ctx, String msg) {
        append(ctx, stamp() + "  " + msg + "\n");
    }

    static String read(Context ctx) {
        try {
            File f = new File(ctx.getFilesDir(), FILE);
            if (!f.exists()) return "";
            byte[] b = new byte[(int) f.length()];
            try (InputStream in = new FileInputStream(f)) {
                int off = 0, n;
                while (off < b.length && (n = in.read(b, off, b.length - off)) != -1) off += n;
            }
            return new String(b, "UTF-8");
        } catch (Throwable ignored) {
            return "";
        }
    }

    static void clear(Context ctx) {
        try { new File(ctx.getFilesDir(), FILE).delete(); } catch (Throwable ignored) {}
    }

    private static void append(Context ctx, String text) {
        try {
            String combined = read(ctx) + text;
            if (combined.length() > MAX_BYTES) {
                combined = combined.substring(combined.length() - MAX_BYTES);
            }
            File f = new File(ctx.getFilesDir(), FILE);
            try (Writer w = new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8")) {
                w.write(combined);
            }
        } catch (Throwable ignored) {}
    }

    private static String stamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }
}
