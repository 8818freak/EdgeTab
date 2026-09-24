package de.herbers.edgetab;

import android.content.Context;
import android.view.View;

/**
 * Eine Registerkarte der Leiste. Jede Funktion - Kalender, Posteingang,
 * Aufgaben, Notizen, Kontakte, Widget - ist ein Tab. Von manchen Typen (z.B.
 * Widget) kann es mehrere Instanzen geben; siehe TabInstance und Tabs.
 */
public interface Tab {

    /** Eindeutige, stabile Kennung dieser Instanz - wird gespeichert. */
    String id();

    /** Anzeigename: eigener Name, sonst der Standardname des Typs. */
    String title(Context ctx);

    /** Standard-Icon (Vektor-Drawable) dieses Kartentyps. */
    int iconRes();

    /** Pfad zu einem selbst gewaehlten Icon, oder null fuer das Standard-Icon. */
    String customIconPath();

    /** Ob Tippen auf einen Eintrag dieser Karte die Leiste schliessen soll. */
    boolean closeOnTap();

    /**
     * Baut den Inhalt der Karte. closePanel schliesst die Leiste sofort (z.B.
     * fuer eigene "Widget wechseln"-Knoepfe); refreshContent baut Kopf und
     * Inhalt der Leiste neu auf, ohne sie zu schliessen (z.B. nach Aendern
     * der Einstellungen dieser Karte).
     */
    View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent);
}
