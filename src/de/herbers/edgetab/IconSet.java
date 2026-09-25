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
        final int labelRes;
        Entry(String key, int res, int labelRes) { this.key = key; this.res = res; this.labelRes = labelRes; }
    }

    // Bewusst auf Begriffe beschraenkt, die zu einer Nachrichten-/
    // Produktivitaetsleiste passen (nach Mathias' Rueckmeldung: Koffer,
    // Fahne, Globus etc. waren beliebige Symbole ohne Bezug zum Programm).
    // Label als String-Resource statt fertigem Text - siehe BaseTab fuer
    // dasselbe Muster bei den Standard-Kartennamen.
    static final Entry[] ALL = {
        new Entry("mail",      R.drawable.ic_pick_mail,      R.string.icon_mail),
        new Entry("chat",      R.drawable.ic_pick_chat,      R.string.icon_chat),
        new Entry("bell",      R.drawable.ic_pick_bell,      R.string.icon_bell),
        new Entry("phone",     R.drawable.ic_pick_phone,     R.string.icon_phone),
        new Entry("inbox",     R.drawable.ic_inbox,          R.string.icon_inbox),
        new Entry("tasks",     R.drawable.ic_tasks,          R.string.icon_tasks),
        new Entry("notes",     R.drawable.ic_notes,          R.string.icon_notes),
        new Entry("person",    R.drawable.ic_contacts,       R.string.icon_person),
        new Entry("people",    R.drawable.ic_pick_people,    R.string.icon_people),
        new Entry("shield",    R.drawable.ic_pick_shield,    R.string.icon_shield),
        new Entry("clock",     R.drawable.ic_pick_clock,     R.string.icon_clock),
        new Entry("cloud",     R.drawable.ic_pick_cloud,     R.string.icon_cloud),
        new Entry("cart",      R.drawable.ic_pick_cart,      R.string.icon_cart),
        new Entry("star",      R.drawable.ic_pick_star,      R.string.icon_star),
        new Entry("heart",     R.drawable.ic_pick_heart,     R.string.icon_heart),
    };

    /** Liefert das Icon zu einem Schluessel, oder 0 wenn keins passt/gesetzt ist. */
    static int resFor(String key) {
        if (key == null) return 0;
        for (Entry e : ALL) if (e.key.equals(key)) return e.res;
        return 0;
    }
}
