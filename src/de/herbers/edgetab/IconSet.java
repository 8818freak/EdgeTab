package de.herbers.edgetab;

/**
 * Mitgelieferter Icon-Satz zur Auswahl fuer Registerkarten - zusaetzlich zur
 * freien Bildauswahl aus der Galerie (siehe MainActivity.pickIconFromGallery).
 * Ein stabiler String-Schluessel wird gespeichert statt der Resource-Id,
 * damit gespeicherte Werte auch nach einem Neu-Build gueltig bleiben.
 */
final class IconSet {

    private IconSet() {}

    static final class Entry {
        final String key;
        final int res;
        final String label;
        Entry(String key, int res, String label) { this.key = key; this.res = res; this.label = label; }
    }

    // Bewusst auf Begriffe beschraenkt, die zu einer Nachrichten-/
    // Produktivitaetsleiste passen (nach Mathias' Rueckmeldung: Koffer,
    // Fahne, Globus etc. waren beliebige Symbole ohne Bezug zum Programm).
    static final Entry[] ALL = {
        new Entry("mail",      R.drawable.ic_pick_mail,      "Brief"),
        new Entry("chat",      R.drawable.ic_pick_chat,      "Sprechblase"),
        new Entry("bell",      R.drawable.ic_pick_bell,      "Glocke"),
        new Entry("phone",     R.drawable.ic_pick_phone,     "Telefon"),
        new Entry("inbox",     R.drawable.ic_inbox,          "Posteingang"),
        new Entry("tasks",     R.drawable.ic_tasks,          "Checkliste"),
        new Entry("notes",     R.drawable.ic_notes,          "Notizblock"),
        new Entry("person",    R.drawable.ic_contacts,       "Person"),
        new Entry("people",    R.drawable.ic_pick_people,    "Gruppe"),
        new Entry("shield",    R.drawable.ic_pick_shield,    "Schild"),
        new Entry("clock",     R.drawable.ic_pick_clock,     "Uhr"),
        new Entry("cloud",     R.drawable.ic_pick_cloud,     "Wolke"),
        new Entry("cart",      R.drawable.ic_pick_cart,      "Einkaufswagen"),
        new Entry("star",      R.drawable.ic_pick_star,      "Stern"),
        new Entry("heart",     R.drawable.ic_pick_heart,     "Herz"),
    };

    /** Liefert das Icon zu einem Schluessel, oder 0 wenn keins passt/gesetzt ist. */
    static int resFor(String key) {
        if (key == null) return 0;
        for (Entry e : ALL) if (e.key.equals(key)) return e.res;
        return 0;
    }
}
