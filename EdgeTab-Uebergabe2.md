# EdgeTab – Übergabe für die nächste Sitzung

Diese Datei fasst alles zusammen, was nötig ist, um EdgeTab in einer frischen
Sitzung ohne den bisherigen Gesprächsverlauf weiterzubauen. Für tiefere
Vorgeschichte gibt es zusätzlich die Memory-Datei
`/areas/blackberry-productivityedge.md`.

---

## 1. Worum es geht

**Mathias Herbers** (GrapheneOS-Pixel, Mac seit 1995, nächtlicher Rhythmus,
mag ehrliche Fehlereingeständnisse und knappe Sprache) vermisst BlackBerrys
**Produktivitätsleiste** („Productivity Edge/Tab"): einen vom Rand aufziehbaren
Streifen mit Kalender, Posteingang, Aufgaben, Notizen, Kontakten.

Vorgeschichte in Kürze: Zuerst haben wir die originale BlackBerry-Suite
gepatcht/umsigniert (Werkzeug `bbresign`), das scheiterte aber an einer
`signature`-Berechtigung (`com.blackberry.pim.permission.INTERNAL`) plus
Abo-/BBMe-Risiken. Ergebnis: **Eigenbau** einer offenen App namens **EdgeTab**,
die nur offene Android-Schnittstellen nutzt und an nichts von BlackBerry hängt.

**Antwortsprache: Deutsch. Duzen.**

---

## 2. Aktueller Stand: EdgeTab 0.45

Läuft auf dem Pixel. Paketname **`de.herbers.edgetab`**, min SDK 29, target 34.

### Was funktioniert (auf dem Gerät bestätigt)
- **Rand-Griff** (rechts, einstellbar), Antippen ODER Wischen zur Mitte öffnet;
  **Zuwischen** schließt (SwipePanel, onInterceptTouchEvent).
- **Panel mit Registerkarten**: Icon-Spalte + Inhaltsfläche. Einstellungen als
  eigene Karte (Zahnrad) ganz oben.
- **Kopfzeile**: große Uhr, „Wochentag, Datum", rechts Akku; per Checkbox
  abschaltbar. Momentaufnahme beim Öffnen (tickt nicht live).
- **Kalender-Karte** (echt): Termine der nächsten 7 Tage, gruppiert, Farbbalken,
  Uhrzeit, Ort; Tippen öffnet den Termin.
- **Posteingang-Karte** (echt): liest abgefangene Benachrichtigungen der
  gewählten Quell-Apps (Mathias nutzt für Mail **nur den BlackBerry Hub**).
  Ungelesen fett, relative Zeit, App-Name, Vorschau bis 6 Zeilen.
  **Tippen öffnet die konkrete Nachricht** (Telegram/Signal-über-Molly/BBMe:
  direkt; Hub: über Vordergrund-Trampolin – landet aber nur im Posteingang, s.u.).
  **✕** blendet den Eintrag aus und zeigt oben die **Wiederherstellen-Leiste**
  (5 s), danach wird endgültig entfernt.
- **Einstellungen** (SettingsTab, im Panel): Seite, Position, Grifflänge,
  Breite, Transparenz, Schriftgröße (mit Live-Vorschau), Kopfzeile/Akku an/aus,
  Posteingang-Quellen, **Aufbewahrungsdauer** (3/7/14/30/90 Tage, unbegrenzt),
  Karten-Auswahl, „Leiste beenden". Versionsanzeige liest jetzt echte Version.
- **Keine Dauer-Startseite**: MainActivity zeigt bei fehlenden Berechtigungen
  die Instruktionsseite; sonst zieht Icon-Antippen nur die Leiste auf.

### Widget-Karte (NEU in 0.15, Bug in 0.16 behoben, seit 0.17 mehrfach & mehrere Widgets je Karte)
**Bestätigt funktionsfähig**: Mathias hat 0.16 getestet, das Hub-Posteingang-
Widget laesst sich einbetten und zeigt echte Inhalte. Details zur
Mehrfach-Karten-/Mehrfach-Widget-Erweiterung in 0.17: siehe Abschnitt
„Registerkarten-Umbau" weiter unten.

Karte **„Widget"** (`WidgetTab`), die ein **frei wählbares App-Widget** über
einen eigenen `AppWidgetHost` einbettet. Zweck: das **echte Hub-Posteingang-
Widget** anzeigen und so den Benachrichtigungs-Engpass (s.u.) umgehen.
- Auswahl über MainActivity im Modus `pick_widget` (Liste aller installierten
  Widget-Anbieter, nach App gruppiert). Binden per
  `bindAppWidgetIdIfAllowed`, sonst System-Bestätigung `ACTION_APPWIDGET_BIND`;
  optionale Konfig-Activity via `startAppWidgetConfigureActivityForResult`.
  Gewählte Widget-ID + Provider werden seit 0.17 in der jeweiligen
  `TabInstance` gespeichert (vorher flach in `Settings`).
- Host-Listening startet/stoppt in `EdgeService.onCreate/onDestroy`
  (`WidgetHostHolder`).
- **Offene Frage**: ob ein fremdes (Sammel-)Widget im Overlay-Fenster (TYPE_
  APPLICATION_OVERLAY) vollständig rendert und interaktiv ist. Bei Fehlern zeigt
  die Karte die Exception; Log-Tag **`EdgeTabWidget`**.
- **Bug gefunden und in 0.16 behoben**: Mathias hat Screenshots vom
  Widget-Auswahldialog geschickt — EdgeTab zeigte nur 4 App-Gruppen (App
  Manager, Google Play Store, Nutzer, System-UI), **der Hub fehlte komplett**,
  obwohl der native Launcher-Picker und der Picker der originalen BlackBerry
  Registerkarte Produktivität das Hub-Widget klar mit Vorschau zeigen. Ursache:
  `AndroidManifest.xml` hatte **keinen `<queries>`-Block**. Seit Android 11
  filtert `AppWidgetManager.getInstalledProviders()` alle Pakete raus, die die
  App nicht per `<queries>` sichtbar gemacht hat — übrig bleiben nur die paar
  immer-sichtbaren Systempakete (Play Store, System-UI etc.), genau das
  beobachtete Muster. Fix: in `AndroidManifest.xml` (als Geschwister von
  `<application>`) ergänzt:
  ```xml
  <queries>
      <intent>
          <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
      </intent>
  </queries>
  ```
  **→ Erste Aufgabe nächste Sitzung: Mathias installiert 0.16 und prüft, ob (a)
  das Hub-Widget jetzt in der Liste auftaucht, (b) es sich einbetten lässt,
  (c) es Inhalte zeigt und man Mails darin öffnen kann.** Wenn ja, ist das der
  Weg für einen echten, benachrichtigungsunabhängigen Posteingang.

### Registerkarten-Umbau (NEU in 0.17)
Auf Mathias' Wunsch (nach dem Vorbild der Original-Software, Screenshots dazu
in `Bilder BlackBerry Registerkarte Produktivität/`): Registerkarten sind
jetzt **Instanzen** statt einer festen Menge. Neue Klassen **TabInstance**
(Datenklasse: Typ, an/aus, eigener Name, eigenes Icon-Bild, „nach Tippen
schließen", bei Widget-Karten die Liste eingebetteter Widgets) und
**BaseTab** (gemeinsame Basis, loest Name/Icon/Tippen-Verhalten auf).
**Tabs** speichert die Instanzen jetzt als JSON (`tabs_json`-Pref) statt der
alten Reihenfolge-/An-Aus-Prefs; beim allerersten Laden nach dem Update
migriert `Tabs.migrate()` automatisch die alte Reihenfolge/Auswahl **und das
bereits eingebettete Hub-Widget aus 0.16** in die neue Struktur - nichts geht
verloren. Das `Tab`-Interface hat jetzt `title(Context)`/`iconRes()`/
`customIconPath()`/`closeOnTap()` sowie ein `refreshContent`-Runnable in
`buildContent` zusaetzlich zu `closePanel` (fuer Aenderungen, die die Leiste
neu aufbauen, aber nicht schliessen sollen).

Umgesetzt:
- **Mehrere Widget-Registerkarten**: „+ Neue Widget-Registerkarte" in den
  Einstellungen legt beliebig viele an (`Tabs.addWidgetTab`), jede mit
  eigenem Namen/Icon; loeschbar (nur zusaetzlich angelegte, die sechs
  urspruenglichen Karten bleiben nur aus-/einschaltbar - `Tabs.isDeletable`).
- **Mehrere Widgets je Widget-Karte**: `TabInstance.widgets` ist eine Liste;
  `WidgetTab` stapelt sie senkrecht (Hoehe geteilt durch Anzahl), „+ Weiteres
  Widget" haengt an, „Entfernen ✕" je Widget nimmt es wieder raus.
- **„Nach Tippen schließen" je Karte** (nicht je Widget - haelt es einfach):
  `TabInstance.closeOnTap`, Checkbox in den Einstellungen. Bei Kalender/
  Posteingang wirkt das direkt (eigene Klick-Handler). Bei eingebetteten
  Fremd-Widgets ist das nur eine Annaeherung: `WidgetTab.WidgetFrame`
  ueberwacht `dispatchTouchEvent` OHNE den Klick zu stehlen und schliesst bei
  einem kurzen Tippen (kaum Bewegung, kein Scrollen) die Leiste 260ms
  spaeter - genug Zeit fuer den PendingIntent des Widgets. Bekannte
  Einschraenkung: ein Tipp auf leere Stellen im Widget schliesst dann auch,
  Long-Press wird nicht erkannt.
- **Umbenennen**: EditText direkt in der Karte, speichert bei jeder Aenderung
  (`RenameWatcher`), kein Panel-Neuaufbau (sonst verliert das Feld den
  Fokus).
- **Eigenes Icon**: Tippen aufs Icon startet `MainActivity` mit
  `pick_icon=<tabId>`, die per `ACTION_OPEN_DOCUMENT` ein Bild waehlen laesst,
  quadratisch zuschneidet, auf 96dp verkleinert und dauerhaft unter
  `getFilesDir()/tab_icons/<id>.png` ablegt (kein Zugriff auf eine fremde
  content://-URI noetig). Aktives Icon wird NICHT eingefaerbt (sonst zerstoert
  das ein Foto), Standard-Vektoricons weiter mit Blau/Grau-Tint.
- **Reihenfolge**: ▲▼-Knoepfe je Karte (`Tabs.move`) - kein Drag&Drop (keine
  AndroidX/RecyclerView-Bibliothek im Projekt, siehe Bauweise oben).
- **Aktiver Tab hinterlegt**: `EdgeService.iconView` legt bei aktivem Tab
  einen blauen, abgerundeten Hintergrund hinter das (dann weisse) Icon -
  wie in den BB-Screenshots.

**Noch offen / ungetestet auf dem Geraet** (naechste Sitzung zuerst pruefen):
EditText-Fokus/Tastatur im Overlay-Fenster (erstes EditText ueberhaupt im
Projekt - bisher nur Buttons/Checkboxen/SeekBars dort verwendet), das
Tipp-Schliessen-Verhalten bei echten Fremd-Widgets (Hub), Mehrfach-Widgets
optisch bei wenig Platz.

### Rueckmeldungen zu 0.18, behoben in 0.19
- **BBMe-Widget-Verwirrung geklaert**: Es gibt kein eigenstaendiges
  BBMe-Widget - der Screenshot, den Mathias dazu zeigte, war das BBMe-Icon
  INNERHALB des Hub-Posteingang-Widgets (Konto-Icon einer Nachricht), nicht
  ein separater Widget-Anbieter. Kein Bug, kein Code-Thema. Erledigt/kein
  Handlungsbedarf.
- **Weisse Vollbildseiten** (Erststart-Instruktionen, Widget-/Icon-Auswahl in
  MainActivity) sahen neben der dunklen Leiste "furchtbar" aus - Standard-
  Theme war `Theme.Material.Light.DarkActionBar`. Neu: `res/values/styles.xml`
  mit **EdgeTabTheme** (dunkler Hintergrund `#1C1C1E`, Akzentfarbe
  `@color/accent` = `#2E9BE6`, helle Textfarben), in `AndroidManifest.xml`
  als `android:theme` eingetragen. Betrifft NUR die MainActivity-Seiten - die
  Leiste selbst (EdgeService) baut ihr Overlay-Fenster manuell und war davon
  nie betroffen.
- **Icon-Satz wenig und thematisch daneben** ("wozu brauche ich da einen
  Koffer oder eine Fahne? Oder gar einen Globus?"): Koffer/Fahne/Haus/Kamera/
  Note/Buch/Globus entfernt (Dateien geloescht, nicht nur aus der Liste
  genommen). Dafuer 15 auf eine Nachrichten-/Produktivitaetsleiste
  zugeschnittene Symbole in **IconSet.ALL**: Brief, Sprechblase, Glocke,
  Telefon, Posteingang, Checkliste, Notizblock, Person, Gruppe, Schild, Uhr,
  Wolke, Einkaufswagen, Stern, Herz. Vier davon sind **keine neuen Dateien**,
  sondern die schon vorhandenen Kartensymbole wiederverwendet
  (`ic_inbox`/`ic_tasks`/`ic_notes`/`ic_contacts` - passen inhaltlich genau
  und sparen Arbeit). Sechs neue Vektor-Icons: `ic_pick_mail`, `ic_pick_people`,
  `ic_pick_shield`, `ic_pick_cloud`, `ic_pick_cart`, `ic_pick_clock` (gleicher
  schlichter Zweifarb-Stil wie die bestehenden Kartensymbole - weiss +
  Akzentblau, selbst gezeichnet, keine fremden/urheberrechtlich heiklen
  Grafiken).

### Rueckmeldungen zu 0.17, behoben in 0.18
Mathias hat 0.17 ausprobiert - alles Neue hat gefallen, drei Punkte:
- **Sortieren sprang nach oben**: Jeder Klick auf ▲▼ (und jede andere
  Aenderung an einer Registerkarte - an/aus, loeschen, neue Widget-Karte)
  baute das Panel komplett neu auf (`refreshPanel` = schliessen+oeffnen) und
  die Einstellungen-ScrollView begann danach wieder oben. Fix: `SettingsTab`
  merkt sich die aktuelle Scroll-Position in einem **statischen** Feld
  (`savedScrollY`, noetig weil bei jedem Aufbau ein neues `SettingsTab`-Objekt
  entsteht) kurz bevor `refresh.run()` (also `refreshPanel`) aufgerufen wird,
  und stellt sie nach dem Neuaufbau per `scroll.post(() -> scroll.scrollTo(...))`
  wieder her.
- **Mitgelieferte Icon-Grafiken gewuenscht** (zusaetzlich zur freien
  Bildauswahl): 12 kleine, selbst gezeichnete Vektor-Icons in
  `res/drawable/ic_pick_*.xml` (Stern, Herz, Glocke, Sprechblase, Koffer,
  Fahne, Haus, Kamera, Note, Buch, Globus, Telefon) - schlicht gehalten,
  keine fremden/urheberrechtlich heiklen Grafiken. Neue Klasse **IconSet**
  bildet einen stabilen String-Schluessel (`"star"`, `"heart"`, ...) auf die
  Drawable-Ressource ab (Schluessel statt Resource-Id gespeichert, damit ein
  Neu-Build die Zuordnung nicht zerschiesst). `TabInstance.iconKey` neu,
  neben `iconPath` (eigenes Foto - hat Vorrang, beide schliessen sich
  gegenseitig aus). `BaseTab.iconRes()` loest jetzt zuerst `iconKey` auf,
  sonst das Standard-Icon des Kartentyps. Tippen aufs Icon in den
  Einstellungen oeffnet jetzt (`MainActivity.pickIcon`) einen Auswahlbildschirm:
  „Eigenes Bild aus der Galerie…" / „Standard-Icon verwenden" / Raster der
  12 mitgelieferten Symbole (`GridLayout` aus `android.widget` - **nicht**
  AndroidX, die Bibliothek ist im Projekt ja nicht eingebunden).
- **BBMe-Widget fehlt weiterhin in der Liste**: NICHT das gleiche Problem wie
  der `<queries>`-Bug aus 0.16. Vermutung war zunaechst, `getInstalledProviders()`
  filtere ohne Kategorie-Parameter nur auf `WIDGET_CATEGORY_HOME_SCREEN` und
  lasse Widgets mit anderer Kategorie (z.B. nur `KEYGUARD`) weg - **aber**:
  die dafuer noetige ueberladene Methode `getInstalledProviders(int
  categoryFilter)` existiert im aktuellen `android.jar` (API 34) gar nicht
  mehr oeffentlich (`javap` zeigt nur noch die parameterlose Variante) - lieg
  vermutlich seit einer neueren API-Ebene hinter `@SystemApi`/versteckt.
  **Kann EdgeTab (oder jede andere normale App) also nicht erzwingen.**
  Naechster Schritt: Mathias soll pruefen, ob BBMe im **System-eigenen**
  Widget-Dialog auftaucht (Startbildschirm lange druecken -> Widgets, ohne
  EdgeTab). Taucht es dort AUCH nicht auf, hat BBMe schlicht keinen
  exportierten Home-Screen-Widget-Provider - das koennte keine dritte App
  umgehen (auch nicht EdgeTab). Taucht es dort auf, aber nicht bei EdgeTab,
  ist das ein neuer, noch ungeklaerter Unterschied - dann bitte Bescheid
  geben, dann muss genauer hingeschaut werden.

### Bekannte, noch offene Punkte
- **Löschen im Hub — GELÖST in 0.22** (siehe unten für die volle Herleitung).
  Das ✕ löst die „Löschen"-Aktion der Hub-Benachrichtigung aus. Diese ist
  eine **Activity mit Bestätigungsdialog** (Mathias will den Dialog behalten,
  gegen Fehllöschen). 0.14 holte daher den Hub in den Vordergrund (Panel
  schließen → App vorn → Aktion), damit der Dialog erscheint.
  **Das war lange Zeit der Fehler selbst** – Ursache zunächst offen. Er hat
  gesagt, das Thema **vorerst ruhen** zu lassen. Log-Zeilen: `Loeschen-PI: …`,
  `Loeschen … gesendet/FEHLGESCHLAGEN`.

  **2026-09-24, wieder aufgenommen: Hub-APKs zerlegt (jadx), konkreter
  Verdacht gefunden.** Mathias hat die echten Hub-/PIM-Suite-APKs in
  `Hub/` gelegt (`com.blackberry.hub`, `.calendar`, `.contacts`, `.notes`,
  `.tasks`, `.infrastructure`, u.a.) sowie die `productivityedge`-APK
  (Original-Leiste). Beide mit `jadx` dekompiliert (lokal installiert,
  `/usr/local/bin/jadx`), Manifeste mit `aapt2 dump xmltree` gelesen.
  - Ausprobiert (auf Mathias' Frage): ob es eine offenere „Fernsteuerung"
    fuer Nachrichten-Aktionen gibt. Ergebnis: `com.blackberry.hub.service.
    HubIntentService` und `com.blackberry.hub.ui.HubBroadcastReceiver` sind
    die einzigen Komponenten im Hub-Manifest OHNE `pim.permission.INTERNAL`
    - aber sie behandeln nur Pin/Verknuepfung, Konto-Ansicht, Drucken und
      Sync-Fehler-Banner (`O0.a.m()`), **keine** Lese-/Loesch-Aktionen.
      Sackgasse, aber jetzt belegt statt vermutet.
  - **Der eigentliche Fund**: `com.blackberry.hub.ui.DeleteIntentActivity`
    (genau die Activity, die unser Loeschen-PendingIntent schon anspricht)
    prueft ganz am Anfang von `onCreate()`:
    ```java
    if (!k.f(this)) { finish(); return; }
    ```
    `k.f()` (Klasse `p027e2.k`, ruft `C1.k.f322c` auf) verlangt, dass
    **BlackBerry Hub selbst** diese fuenf Laufzeit-Berechtigungen hat:
    `GET_ACCOUNTS`, `READ_CALENDAR`, `READ_CONTACTS`, `READ_PHONE_STATE`,
    `READ_MEDIA_AUDIO` (Android 13+, sonst `READ_EXTERNAL_STORAGE`). Fehlt
    auch nur eine, beendet sich die Activity sofort - ohne zu loeschen, ohne
    Fehlermeldung. Passt exakt zum beobachteten Symptom.
  - **Zwischenstand, dann korrigiert**: Nach dem Berechtigungs-Test (alle
    fuenf `k.f()`-Berechtigungen erlaubt) blieb das leere Fenster bestehen.
    Erste Vermutung - ein Hub-internes "Concierge"-Lizenz-/Aktivierungs-
    problem, das auch bei direktem Loeschen in der System-Benachrichtigung
    auftrete - **war falsch**: Mathias hat klargestellt, dass Loeschen ueber
    die System-Benachrichtigung UND direkt im Hub anstandslos funktioniert,
    nur bei EdgeTab nicht. Der Fehler liegt also in UNSEREM Aufruf.
  - **Ursache gefunden per `adb logcat` (Telefon haengt am Mac, Debug aktiv)**:
    `adb logcat` gefiltert auf `EdgeTabLauncher/EdgeTabCollector/
    DeleteIntentActivity/PermissionUtils/HubIntentService/ActivityManager:W`
    waehrend zwei Loeschversuchen (einmal ueber EdgeTab, einmal direkt ueber
    die System-Benachrichtigung) aufgezeichnet und verglichen:
    - Direkt ueber die System-Benachrichtigung: `DeleteIntentActivity` laeuft
      sauber durch bis `executeSingleIntent` (Ziel:
      `com.blackberry.infrastructure/com.blackberry.email.service.
      EmailIntentService`).
    - Ueber EdgeTab: **kein einziger `DeleteIntentActivity`-Log-Eintrag** -
      die Activity startet nie. Stattdessen postet der Hub **dieselbe
      Benachrichtigung binnen ~300ms achtmal neu** (00:41:25.355 bis .649),
      offenbar ausgeloest dadurch, dass `Launcher.fireAfterForeground` den
      Hub zuerst per `launchApp()` in den Vordergrund holte (das loeste
      wohl einen Sync-/Refresh-Zyklus im Hub aus). Der VOR diesem Hochholen
      gemerkte Loeschen-PendingIntent gehoerte danach zu einer bereits
      ueberholten Benachrichtigung und verpuffte beim Senden 450ms spaeter
      spurlos - kein Absturz, kein Fehler, einfach nichts.
    - Der direkte Tipp in der Systembenachrichtigung holt die App NICHT
      vorher hoch, sondern sendet sofort - und trifft daher immer die
      aktuell gueltige Benachrichtigung.
  - **Fix in 0.22**: `NotificationCollector.deleteInApp()` ruft nicht mehr
    `Launcher.fireAfterForeground` (App zuerst hochholen, 450ms warten),
    sondern sendet den Loeschen-PendingIntent **sofort** mit
    Hintergrund-Start-Erlaubnis (`Launcher.send(ctx, pi, Launcher.
    bgAllowed())`) - genau wie es der System-eigene Weg tut. Panel wird
    weiterhin vorher geschlossen, aber kein Vorab-Foregrounding mehr. Die
    dadurch ungenutzte Methode `Launcher.fireAfterForeground` wurde entfernt
    (war nur noch dort verwendet).
  - Direkt per `adb install -r` auf Mathias' verbundenes Handy installiert
    (statt Datei-Versand). **Von Mathias bestaetigt: Loeschen funktioniert
    jetzt.** Thema damit endgueltig erledigt.
  - Entpackte/dekompilierte Kopien liegen nur im Scratchpad dieser Sitzung
    (nicht im Projektordner) - bei Bedarf naechste Sitzung neu entpacken,
    ist in Sekunden erledigt (`jadx -d <ziel> <apk>`).
- **Der „Benachrichtigungs-Engpass"** (mehrfach erklärt): Tippen/Löschen/„in die
  Mail" geht nur, solange die **Benachrichtigung noch in der Statusleiste lebt**.
  Der Hub entfernt sie, sobald man den Posteingang oder die Mail einmal ansieht.
  Danach steht die Mail in EdgeTab auf „**· nur App**" (Tippen öffnet nur die
  App). **Beim Hub landet Tippen ohnehin nur im Posteingang, nicht in der
  konkreten Mail** – sein contentIntent führt dorthin.
- **„Draht speichern" nicht möglich**: der contentIntent ist ein PendingIntent
  (Einmal-Token des Hubs), nicht persistierbar, wird beim Zurückziehen der
  Benachrichtigung ungültig. Ein dauerhafter Posteingang ginge nur mit direktem
  **IMAP-Client** in EdgeTab (eigener größerer Baustein). Das Widget (0.15) ist
  der Versuch, das ohne IMAP zu lösen.

---

## 3. Wie gebaut wird (WICHTIG – ohne Gradle)

**Seit 0.16: lokal auf Mathias' Mac**, nicht mehr im Cloud-Container. Das
Android-SDK liegt **persistent** unter `/Users/mathias/Library/Android/sdk`
(einmalig installiert, bleibt zwischen Sitzungen erhalten — kein Neu-Download
mehr nötig). Java ist bereits vorhanden (`/usr/bin/javac`, OpenJDK 17;
`--release 11` beim Kompilieren zieht das runter, siehe Fallstricke).

Aktuelle, entpackte Quelle liegt direkt hier (kein Hochladen/Entpacken nötig):
`/Users/mathias/Downloads/Lesen/_sortieren/_Sortieren/_auf Handlich/EdgeTab/EdgeTab-quellcode_aktuell/`
(`AndroidManifest.xml`, `src/…`, `res/…`). **Wichtig, Ordnername hat sich
2026-09-24 geändert**: hieß vorher `EdgeTab-quellcode_0_15` (nach der
zuletzt hochgeladenen Version benannt); Mathias hat den EdgeTab-Ordner
aufgeräumt und alte Zips/APKs in eigene Unterordner `EdgeTab/` (APKs) und
`EdgeTab-quellcode/` (Zips) sortiert - der lebende Arbeitsordner heißt seither
**`EdgeTab-quellcode_aktuell`**, nicht mehr versions-nummeriert (bleibt so,
damit er nicht wieder Namenskollisionen mit dem Archiv verursacht). **Vorsicht
beim Entpacken eines Zips in diesen Ordner**: macOS' Dateisystem ist
Groß-/Kleinschreibung-unempfindlich - der Zip-interne Ordner heißt
`edgetab/` (klein), das kollidiert mit einem vorhandenen `EdgeTab/`-Ordner
und verschmilzt beide! Lieber gezielt in einen Temp-Ordner entpacken und
nur `AndroidManifest.xml`/`src`/`res` gezielt kopieren (siehe Baubefehle
unten). Keystore weiterhin lokal: `.../EdgeTab/keystore.p12`. Nach jedem Bau
wird der Ordner zusätzlich als `EdgeTab-quellcode_0_XX.zip` archiviert
(Konvention aus der alten Container-Zeit, für Mathias' Ablage beibehalten).

**Seit 0.22/0.23 zusätzlich möglich**: Wenn Mathias' Handy per USB-Debugging
am Mac hängt (`adb devices` zeigt es), kann direkt gebaut UND installiert
werden (`adb install -r build/EdgeTab-$V.apk`) - schneller als Datei-Versand,
und `adb logcat` lässt sich fuer Fehlersuche mitschneiden (siehe Abschnitt
zum Hub-Löschen-Fix weiter unten als Beispiel). Android-SDK-`platform-tools`
(enthält `adb`) sind installiert.

Der alte Container-Weg (falls doch mal nötig, z. B. ohne Mac-Zugriff) bleibt
unten dokumentiert, ist aber **nicht mehr der Standardweg**.

1. **Android-SDK holen** (unter `/home/claude/sdk`, nur falls im Container statt lokal gebaut wird):
   ```bash
   mkdir -p /home/claude/sdk && cd /home/claude/sdk
   curl -sL -o cmdline.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
   unzip -q cmdline.zip && mkdir -p cmdline-tools/latest && mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null
   yes | ./cmdline-tools/latest/bin/sdkmanager --sdk_root=/home/claude/sdk "platforms;android-34" "build-tools;34.0.0" >/dev/null
   ```
2. **JDK sicherstellen** (oft nur JRE da): `apt-get update -qq && apt-get install -y openjdk-21-jdk-headless`
3. **Bauen** (im Ordner `edgetab/`; `set -e`, damit der Bau bei Fehlern ABBRICHT –
   sonst läuft er trotz javac-Fehler weiter und signiert ein altes APK!):
   ```bash
   SDK=/home/claude/sdk; BT="$SDK/build-tools/34.0.0"; AJAR="$SDK/platforms/android-34/android.jar"; V=0.24
   set -e
   rm -rf build && mkdir -p build/gen build/obj
   "$BT/aapt2" compile --dir res -o build/res.zip
   "$BT/aapt2" link -o build/base.apk -I "$AJAR" --manifest AndroidManifest.xml \
     --java build/gen -R build/res.zip --auto-add-overlay --min-sdk-version 29 --target-sdk-version 34
   javac --release 11 -d build/obj -classpath "$AJAR" $(find src build/gen -name '*.java')
   "$BT/d8" --min-api 29 --lib "$AJAR" --output build/ $(find build/obj -name '*.class')
   cp build/base.apk build/unsigned.apk && ( cd build && zip -qj unsigned.apk classes.dex )
   "$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
   "$BT/apksigner" sign --ks /mnt/user-data/uploads/keystore.p12 --ks-type PKCS12 \
     --ks-pass pass:bbresign --ks-key-alias bbresign --key-pass pass:bbresign \
     --min-sdk-version 21 --max-sdk-version 34 \
     --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
     --out build/EdgeTab-$V.apk build/aligned.apk
   "$BT/apksigner" verify --min-sdk-version 21 -v build/EdgeTab-$V.apk | head -4
   ```

### Fallstricke (jedes Mal beachten)
- **`javac --release 11`** verwenden (`--release 17` → d8-NPE).
- **Klargestellt in 0.17** (nach genauerem Hinsehen, mit 0.17 als Beleg - baut
  sauber): **Lambdas fuer einfache Listener (`OnClickListener`,
  `OnCheckedChangeListener`, `Runnable`) sind unproblematisch** und werden im
  Projekt laengst ueberall so verwendet (EdgeService, SettingsTab, InboxTab,
  CalendarTab). Ein **echtes `new Foo(){...}`** (anonyme innere Klasse mit
  Klassenkoerper) wird weiter vermieden, ebenso wie **`Comparator`-
  Implementierungen jeder Art** (Lambda oder anonym) - dort laesst die
  synthetische Bruecken methode `compare(Object,Object)` d8 mit interner NPE
  abstuerzen (`Error in …$X.class: String.length() … null`). Statt zu
  sortieren eine **TreeMap mit String-Schlüssel** nutzen (siehe
  MainActivity.showWidgetPicker). **Benannte, moeglichst static nested
  Klassen** bleiben die richtige Wahl, wenn ein Listener mehrere Methoden hat
  (z.B. `TextWatcher`, `OnSeekBarChangeListener`) oder eigenen Zustand haelt
  (z.B. `WidgetTab.WidgetFrame`, das Touch-Events beobachtet).
- **`set -e` im Bau-Skript** – sonst wird nach javac-Fehler ein veraltetes APK
  signiert (ist zweimal passiert). Nach dem Bau immer versionName im
  `aapt2 dump badging` und die neuen Klassen mit `strings build/classes.dex`
  gegenprüfen.
- **aapt2 link** braucht `--auto-add-overlay`.
- **JDK** fehlt anfangs oft (nur JRE → kein `javac`).
- **In MainActivity heißt `Settings` das Android-`Settings`** (Import). Unsere
  Klasse dort **voll qualifizieren**: `de.herbers.edgetab.Settings.…`.
- Bytecheck der Signatur: `apksigner verify --min-sdk-version 21 -v` zeigt v1+v2+v3
  (ohne `--min-sdk-version` meldet es fälschlich v1/v2=false).

### Signaturschlüssel (entscheidend für Updates ohne Deinstallation)
Mathias' Keystore **`/mnt/user-data/uploads/keystore.p12`** (er lädt ihn hoch),
Store-/Key-Passwort **`bbresign`**, Alias **`bbresign`**, CN=BlackBerry Ltd
(kosmetisch). **Immer damit signieren**, sonst muss er deinstallieren.
versionCode je Release +1.

---

## 4. Codestruktur (Paket `de.herbers.edgetab`)

- **MainActivity** – Erststart-Assistent (Berechtigungen) / Leiste öffnen /
  **Widget-Auswahldialog** (`pick_widget`, mit `tab_id`+`slot` seit 0.17,
  Bind-/Konfig-Ablauf, onActivityResult) / **Icon-Auswahl** (`pick_icon`,
  seit 0.19: erst Auswahlbildschirm - mitgelieferter Satz `IconSet.ALL` als
  `GridLayout` ODER `pickIconFromGallery` -> `ACTION_OPEN_DOCUMENT`,
  `saveIcon` schneidet/skaliert/speichert unter `getFilesDir()/tab_icons/`).
  Theme seit 0.19: `@style/EdgeTabTheme` (`res/values/styles.xml`, dunkel +
  Akzentblau) statt des weissen Standard-Themes - betrifft nur diese
  Vollbildseiten, nicht die Leiste selbst.
- **EdgeService** – Vordergrunddienst: Rand-Griff, Panel, Icon-Spalte, Kopfzeile,
  Tab-Umschaltung. Enthält `SwipePanel` (Schließgeste) und `SwipeOpen` (Öffnen).
  `iconView()` seit 0.17: hinterlegt den aktiven Tab (blaue, abgerundete
  Flaeche), faerbt eigene (Foto-)Icons nicht ein. `dp()` package-private.
  Extras: `"refresh"`, `"open"`. Startet/stoppt `WidgetHostHolder`-Listening.
- **Tab** (Interface: `id/title(ctx)/iconRes/customIconPath/closeOnTap/
  buildContent(ctx,closePanel,refreshContent)`), **BaseTab** (gemeinsame
  Basis - loest Name/Icon/Tippen-Verhalten aus der TabInstance auf),
  **TabInstance** (Datenklasse + JSON-Serialisierung), **Tabs** (Speicherung
  als JSON, `load/save/update/move/addWidgetTab/remove/isDeletable/
  buildActive/build`, Migration der alten 0.16-Daten), **PlaceholderTab**.
- **CalendarTab** – `CalendarContract`.
- **InboxTab** – Posteingang aus `NotificationStore`; Tippen→`Launcher.open`,
  ✕→`UndoBar.schedule`; markiert Einträge ohne lebende Benachrichtigung als
  „· nur App"; prunt beim Aufbau nach Aufbewahrungsdauer; führt Doppel zusammen.
- **Launcher** – öffnet Ziel-Intents; direkte Activities sofort, „Trampolin"
  (Broadcast/Service, z. B. Hub) erst App in den Vordergrund, dann PI.
  `fireAfterForeground` (für Aktionen mit eigenem Dialog, z. B. Hub-Löschen).
- **NotificationCollector** (NotificationListenerService) – speichert
  Benachrichtigungen; `intents`/`deletes`-Maps; `resolve(Item)` findet die aktive
  Benachrichtigung per Schlüssel ODER Inhalt (App+Titel+Text), weil der Hub neu
  postet; `deleteInApp(ctx,it,panelClose)` (Activity-Löschen→Vordergrund),
  `dismiss`, `liveIndex`, `signature`.
- **NotificationStore** (SQLite) – recent/markSeen/delete/distinctPackages,
  `collapseDuplicates`, `pruneOlderThan`.
- **UndoBar** – „gelöscht · WIEDERHERSTELLEN" oben; verzögertes Endgültig-Löschen.
- **Settings / SettingsTab** – allgemeine Einstellungen (Position, Breite,
  Transparenz, Schrift, Kopfzeile, Posteingang-Quellen/Aufbewahrung) plus
  seit 0.17 der Registerkarten-Editor (`tabRow`, benannte `RenameWatcher`/
  `IconClick`/`MoveClick`). Widget-ID/Provider liegen seit 0.17 in den
  jeweiligen `TabInstance`n, nicht mehr flach in `Settings` (die alten
  Settings-Methoden bleiben nur fuer die Migration bestehen).
- **WidgetHostHolder** – hält den `AppWidgetHost`, start/stopListening.
- **WidgetTab** – bettet die Widgets einer Widget-Karte ein (mehrere gestapelt
  moeglich, `WidgetFrame` beobachtet Tipp-Gesten fuer „nach Tippen schließen").
- **ShortcutsTab** (NEU 0.21) – App-Icon-Verknuepfungen, Raster/Liste,
  Gruppen, Umbenennen; statischer Zustand (`ADDING`/`EDITING`/`SAVED_SCROLL`)
  wie bei SettingsTab.
- **IconSet** (NEU 0.19) – mitgelieferter Icon-Satz fuer Registerkarten,
  String-Schluessel -> Drawable-Resource.
- **HeaderView**, **BootReceiver**.

Icons in `res/drawable/` (…, **ic_widget**, **ic_shortcuts**,
**ic_pick_\*** neu). **Keine BlackBerry-Grafiken.** Eigene, vom Nutzer
gewaehlte Tab-Icons liegen NICHT in `res/`, sondern als PNG unter
`getFilesDir()/tab_icons/<tabId>.png` (App-eigener, privater Speicher).

---

### Rueckmeldungen zu 0.19, behoben in 0.20
- **Icon-Namen im Auswahlraster abgeschnitten**: `GridLayout` gab jeder Spalte
  nur so viel Breite, wie das laengste Wort einzeilig braucht (Zellen hatten
  keine feste Breite) - laengere Namen wie "Einkaufswagen" liefen seitlich aus
  dem ScrollView raus und wurden abgeschnitten. Fix in
  `MainActivity.pickIcon()`: Zellen haben jetzt eine feste Breite (`92dp`
  ueber `GridLayout.LayoutParams.width`), 3 statt 4 Spalten, Beschriftung
  zweizeilig (`setMaxLines(2)`) und zentriert.

### Neue Kartenart in 0.21: Verknuepfungen (ShortcutsTab)
Mathias wollte eine frei gestaltbare Karte mit App-Verknuepfungen. Eingeschaetzt
und mit ihm geklaert: **"echte" tiefe Verknuepfungen** (z.B. "WhatsApp: Chat
mit X", nicht nur "App oeffnen") sind fuer normale Drittanbieter-Apps auf
Android kaum zu bekommen - das Anheften solcher Verknuepfungen
(`ShortcutManager`/`LauncherApps`) landet auf modernen Android-Versionen
fast immer exklusiv beim Standard-Launcher, den EdgeTab nicht ersetzen soll.
Umgesetzt wurde daher bewusst die **reine App-Icon-Variante**:

- Neuer Kartentyp `TYPE_SHORTCUTS` in **TabInstance** (`shortcuts`-Liste aus
  `ShortcutRef{pkg,label,group}`, dazu `groupSettings: Map<String,
  GroupSettings{grid,columns}>` - **je Gruppe eigene Ansicht**, nicht eine
  Einstellung fuer die ganze Karte, auf Mathias' Praezisierung hin so gebaut:
  "einen Teil als Raster und einen andern als Liste haben, wenn man möchte").
  Neue Karte **ShortcutsTab** (`Tabs.build`-Fall, `Tabs.addShortcutsTab`,
  „+ Neue Verknüpfungen-Registerkarte" in SettingsTab).
- **App-Auswahl**: „+ App hinzufügen" listet alle startbaren Apps
  (`PackageManager.queryIntentActivities` auf MAIN/LAUNCHER) direkt IM Panel
  (kein Activity-Umweg noetig, anders als beim Widget-Picker - PackageManager-
  Abfragen brauchen keine Bind-Bestaetigung). **Wichtig**: dafuer musste der
  `<queries>`-Block im Manifest um genau diesen Intent-Filter erweitert
  werden - sonst waere (gleicher Fallstrick wie beim Hub-Widget in 0.16) die
  App-Liste unvollstaendig gewesen.
- **Anzeige**: nach `group`-Feld gruppiert (LinkedHashMap, Reihenfolge =
  erstes Auftauchen einer Gruppe), **jede Gruppe unabhaengig** Raster
  (Spaltenzahl 2-6) ODER Liste (`TabInstance.settingsFor(group)`); Tippen
  startet die App direkt ueber `Launcher.launchApp` (wiederverwendet),
  respektiert `closeOnTap`.
- **Bearbeiten-Modus**: pro Gruppe eine Kopfzeile mit eigenem Ansicht-
  Umschalter + Spalten-Stepper (`groupHeaderEdit`), darunter pro Verknuepfung
  eigener Name (EditText, Platzhalter = echter App-Name) und Gruppenname
  (EditText, leer = ohne Gruppe). Der Name speichert bei jeder Aenderung
  ohne Neuaufbau (`FieldWatcher`, wie `SettingsTab.RenameWatcher` - sonst
  Fokusverlust); die Gruppe ebenso, baut die Gruppen-Ansicht aber zusaetzlich
  per `setOnFocusChangeListener` neu auf, SOBALD das Feld den Fokus verliert
  (nicht bei jedem Tastendruck) - damit ein umbenannter Eintrag sichtbar in
  seine neue Gruppe wandert, ohne beim Tippen den Fokus zu verlieren.
  „✕" entfernt einen Eintrag.
- **Automatische Gruppierung "alles was zu einem Programm gehoert"** wurde
  NICHT eigens gebaut - bei reiner App-Icon-Variante hat jede App ohnehin nur
  einen Eintrag, das waere trivial/wirkungslos gewesen. Falls spaeter doch
  mehrere Eintraege pro App noetig werden (z.B. ueber die einzeln zu
  recherchierenden App-Deep-Links, s.u.), waere das der Punkt, wo das
  nachgeruestet werden muesste.
- Zustand ("App hinzufügen"-Modus, "Bearbeiten"-Modus, Scroll-Position) haengt
  wie bei SettingsTab an **statischen**, je Karten-Id gefuehrten Feldern
  (`ADDING`/`EDITING`/`SAVED_SCROLL`), weil bei jedem Panel-Neuaufbau ein
  neues `ShortcutsTab`-Objekt entsteht.
- Neues Standard-Icon **`ic_shortcuts.xml`** (3x3-Punkteraster, unterscheidet
  sich bewusst vom Widget-Symbol).
- **Ungetestet auf dem Geraet** (naechste Sitzung zuerst pruefen): App-Liste
  vollstaendig? Gruppierung/Umbenennen/Raster-Spalten wie erwartet? Fuer
  einzelne Apps mit **dokumentierten** Deep-Links (z.B. WhatsApp `wa.me/…`)
  koennte man spaeter noch Einzelfall-Verknuepfungen ergaenzen, falls
  gewuenscht - bewusst zurueckgestellt (Bastelei pro App, bricht leicht bei
  Updates).

### Rueckmeldungen zu 0.21/0.22, behoben in 0.23
- **Icongroesse bei Verknuepfungen aenderte sich nicht**: `ShortcutsTab.grid()`
  nutzte eine fest verdrahtete Zellbreite (76dp) und Icongroesse (36dp),
  unabhaengig von der eingestellten Spaltenzahl - bei mehr Spalten passten
  die fest breiten Zellen rechnerisch oft gar nicht nebeneinander. Fix:
  Zellbreite wird jetzt aus der verfuegbaren Panelbreite und der Spaltenzahl
  berechnet (`Settings.panelWidth(ctx)` durch `columns` geteilt, min. 48dp),
  Icongroesse ist ca. 55% der Zellbreite (20-56dp begrenzt) - mehr Spalten
  = kleinere Icons, weniger Spalten = groessere, sichtbar "am Raster
  ausgerichtet".
- **Einstellungen wirkten unelegant/nahmen viel Platz weg** (eine lange,
  durchgehende Liste aller Abschnitte). Umgebaut auf ein **zweistufiges
  Kategorien-Menue**: Zahnrad zeigt jetzt nur 5 kurze Kategorie-Zeilen
  (Position & Aussehen / Kopfzeile / Posteingang / Registerkarten / Dienst),
  Tippen oeffnet nur die jeweilige Unterseite mit „‹ Zurück"-Kopfzeile. Neues
  statisches Feld `SettingsTab.openCategory` (wie `savedScrollY` - ueberlebt
  den Panel-Neuaufbau, weil jedes Mal ein neues SettingsTab-Objekt entsteht).
  Alle bisherigen Einstellungen inhaltlich unveraendert, nur die
  Bildschirmaufteilung ist neu.
- **Aufziehen per Wischen klappte auf beiden Seiten nicht zuverlaessig**
  (links schlechter als rechts, aber laut Mathias auch rechts nicht gut).
  Vermutung: Androids System-Wischgesten am Bildschirmrand (Zurueck/Vorwaerts)
  fangen die Beruehrung ab, bevor sie den nur 14dp breiten Rand-Griff
  erreicht - der Griff liegt komplett in der ueblichen Gesten-Zone. Fix:
  `EdgeService.addHandle()` meldet den Griff-Bereich per
  `View.setSystemGestureExclusionRects()` (API 29+, passt zu minSdk 29) als
  "hier bitte keine System-Gesten" an. **Ungetestet** - naechste Sitzung
  zuerst pruefen, ob das Aufziehen jetzt zuverlaessiger klappt, auf beiden
  Seiten.

### Rueckmeldungen zu 0.23, behoben in 0.24
- **Wisch-Aufziehen bestaetigt funktionierend** - Mathias hatte in Wischrichtung
  vertikal statt horizontal probiert, nach Klaerung ("kaum macht man es
  richtig, schon geht es") lief es. Die Gesten-Ausschlusszone aus 0.23
  bleibt trotzdem sinnvoll (Androids Rand-Wischgesten haetten sonst weiter
  Beruehrungen abfangen koennen) und bleibt drin.
- **Griff-Design wie im BB-Original**: Mathias fand die BB-Umsetzung
  eleganter und bat, das nachzubauen. Original-APK mit `apktool`
  dekompiliert (liefert im Gegensatz zu `jadx --no-res` auch die
  XML-Ressourcen/Grafiken lesbar): `res/layout/handle.xml` zeigt eine aus
  drei Teilen gebaute, spitz zulaufende Griff-Form (obere/untere Spitze +
  gerader Mittelteil), weiss mit schmalem dunklem Schattensaum zur
  Bildschirmmitte hin, drei kleine graue Punkte mittig als Zieh-Andeutung;
  fuer die Gegenseite per `setScaleX(-1)` gespiegelt statt zweimal gezeichnet.
  Genauso nachgebaut: neue Grafiken `handle_tip.xml` (Spitze, fuer unten per
  `scaleY(-1)` wiederverwendet), `handle_middle.xml` (dehnbarer Mittelteil,
  `layer-list` mit Schattenschicht + weiss inset 3dp), `ic_handle_dots.xml`
  (drei Punkte). `EdgeService.addHandle()` baut jetzt ueber
  `buildHandleShape()` eine dreiteilige `LinearLayout`-Komposition statt
  eines einzelnen `View` mit einfachem Rechteck-Drawable; Seiten-Spiegelung
  per `setScaleX(-1)` wenn links. Beruehrungslogik (`SwipeOpen`) unveraendert
  - `getRawX()` ist von der visuellen Spiegelung nicht betroffen.
- **✕-Trefferflaechen vergroessert**: In InboxTab und ShortcutsTab hatten die
  Loeschen-„✕" nur die Glyphengroesse als Trefferflaeche (kaum Polster).
  Jetzt grosszuegiges Padding ringsum (14/10/6/10dp bzw. aehnlich) und
  etwas groessere Schrift. WidgetTab „Entfernen ✕" (hat schon einen
  Textrahmen, war weniger kritisch) ebenfalls etwas gepolstert.
- **Icongroesse der Registerkarten-Spalte einstellbar**: Neuer Regler
  „Größe der Registerkarten-Icons" (18-44dp, Standard 28) unter
  „Position & Aussehen". `Settings.iconSize`/`setIconSize` neu.
  `EdgeService.iconView()` nutzt das statt der festen 28dp; die Icon-
  Spaltenbreite (`iconColumnWidthDp()`, war fest 52dp) waechst automatisch
  mit (`iconSize + 24dp`), sonst waeren groessere Icons abgeschnitten.
- **Verknüpfungen-Karte: nur ein Zahnrad statt zweier Knoepfe** - Mathias'
  urspruengliche Bitte ("ein Zahnrad, danach erscheinen die Menues") hatte
  ich zunaechst nur auf die Haupteinstellungen bezogen, nicht auch auf die
  Werkzeugleiste der einzelnen Verknuepfungen-Karte. Jetzt: im Ruhezustand
  nur ein „⚙", Tippen klappt ein kleines Menue mit „+ App hinzufügen" /
  „Bearbeiten" auf; waehrend eine Aktion laeuft, zeigt sich nur deren
  eigener Abbrechen/Fertig-Knopf (kein Zahnrad-Chaos mittendrin). Neues
  statisches Feld `ShortcutsTab.MENU_OPEN` (wie `ADDING`/`EDITING`).

### Mediensteuerung-Karte fertiggestellt (NEU in 0.28)
Neuer Kartentyp **MediaTab** (`TYPE_MEDIA`, addierbar wie Widget/Verknuepfungen
ueber „+ Neue Mediensteuerung-Registerkarte", nicht Teil der urspruenglichen
sechs Karten). Zeigt laufende Wiedergaben (Titel, Interpret, Cover) mit
Zurueck/Play-Pause/Vor-Steuerung.
- **Keine neue Berechtigung noetig**: `MediaSessionManager.getActiveSessions(
  new ComponentName(ctx, NotificationCollector.class))` - ein aktiver
  `NotificationListenerService` (den EdgeTab fuer den Posteingang ohnehin
  schon ist) darf das ohne die sonst noetige, signaturgeschuetzte
  `MEDIA_CONTENT_CONTROL`-Berechtigung. Keine Manifest-Aenderung.
- Zeigt **alle** aktiven Sitzungen gestapelt (mehrere Apps koennen gleichzeitig
  eine Sitzung haben), nicht nur eine.
- **Kein Live-Ticken** (wie der Rest der App) - nach einem Steuerbefehl wird
  ueber `Handler.postDelayed(refresh, 350)` kurz gewartet und der Inhalt neu
  aufgebaut, damit sich Play/Pause/Titel sichtbar aktualisieren, ohne eine
  dauerhafte `MediaController.Callback`-Registrierung zu brauchen (waere
  komplexer wegen des Tab-Objekt-Lebenszyklus - jedes Mal ein neues Objekt).
- Neues, eigenes Icon `ic_media.xml` (einfaches Play-Dreieck).
- **Praxistest 0.34 durch Mathias**: Titel/Interpret/Cover erscheinen korrekt
  (getestet mit AIReaderX). **Bug gefunden und behoben**: AIReaderX meldete
  gleichzeitig zwei aktive Sitzungen fuer dieselbe App - eine echte (Titel,
  Cover, Steuerung funktioniert) und eine leere Zombie-Sitzung ("(ohne
  Titel)", keine Steuerung), beide wurden angezeigt (doppelte Karte). Fix in
  0.34: `dedupe()` gruppiert nach Paketname und behaelt pro App nur die
  Sitzung mit dem hoechsten `score()` (spielend > pausiert-mit-Zustand >
  hat-Titel > nackt). Noch nicht erneut von Mathias bestaetigt.

### Aufgaben-Karte fertiggestellt (NEU in 0.29)
War bisher `PlaceholderTab`. Jetzt echte Karte **TasksTab** (`TYPE_TASKS`,
eine der sechs urspruenglichen Karten, weiterhin nur aus-/einschaltbar, nicht
loeschbar) – Antwort auf Mathias' Frage „Kommen wir dann zur Implementierung
von Aufgaben? […] Welche SW ich dafuer nutze, weißt Du ja." (JTX Board, siehe
Exkurs Punkt 8 unten).

- **Kein CalDAV-Client in EdgeTab.** JTX Board haelt seine per DAVx⁵
  synchronisierten Aufgaben lokal in einer eigenen SQLite-Tabelle vor und
  legt darueber einen **offenen, dritt-App-lesbaren `ContentProvider`**
  offen – Authority `at.techbee.jtx.provider`, Haupttabelle `icalobject`
  mit u. a. den Spalten `module` (`"TODO"`/`"NOTE"`/`"JOURNAL"`), `summary`,
  `due`, `percent`, `status`. Architektonisch dasselbe Prinzip wie Androids
  eigenes `CalendarContract`/`ContactsContract`: offen dokumentiert im
  quelloffenen `TechbeeAT/jtxBoard`-Repo (`AndroidManifest.xml` +
  `contract/JtxContract.kt`), recherchiert statt geraten.
- **Berechtigung**: `at.techbee.jtx.permission.READ` – eine ganz normale
  ("dangerous"), **nicht signaturgeschuetzte** Laufzeit-Berechtigung, die
  JTX Board selbst deklariert. Kein Vergleich zu BlackBerrys eigenem
  `com.blackberry.pim.permission.INTERNAL` (signaturgeschuetzt, siehe
  Exkurs) – hier ist der Zugriff fuer jede App offen, solange sie danach
  fragt.
- **Berechtigungs-Abfrage generalisiert**: der bisher Kontakte-spezifische
  Mechanismus in `MainActivity` (`request_contacts`/`pendingContactsRequest`/
  `REQ_CONTACTS`) wurde zu einem generischen `request_permission`-String-Extra
  (`pendingPermission`/`REQ_PERMISSION`) umgebaut, der eine beliebige
  Berechtigung transportieren kann. `ContactsTab` nutzt denselben Weg jetzt
  mit `Manifest.permission.READ_CONTACTS`, `TasksTab` mit
  `at.techbee.jtx.permission.READ`. Ein Baustein, zwei Verwender.
- **Manifest-Ergaenzungen**: `<uses-permission
  android:name="at.techbee.jtx.permission.READ" />` sowie in `<queries>`
  ein `<package android:name="at.techbee.jtx" />`, damit EdgeTab ab
  Android 11 ueberhaupt erkennen kann, ob JTX Board installiert ist
  (`PackageManager.getPackageInfo`) – sonst waere das Paket unsichtbar,
  genau wie beim Widget-Picker-Bug in 0.16.
- **Darstellung**: offene (nicht `COMPLETED`) Aufgaben, sortiert nach
  Faelligkeit (ohne Faelligkeitsdatum ans Ende), je Zeile Titel, relatives
  Faelligkeitsdatum (rot bei ueberfaellig), Fortschritt in %. Tippen oeffnet
  die Aufgabe direkt in JTX Board ueber dessen dokumentierten
  View-Intent (`content://at.techbee.jtx/icalobject/<id>`).
- Drei Zustaende sauber abgefangen: JTX Board nicht installiert (Hinweistext),
  Berechtigung fehlt (Hinweis + Button „Zugriff erlauben"), keine offenen
  Aufgaben (Hinweistext).
- **Bug in 0.29, behoben in 0.30 (Kontobindung entdeckt)**: Mathias testete
  0.29 und bekam durchgehend „Aufgaben konnten nicht gelesen werden. Ist JTX
  Board aktuell genug?" Per `jadx`-Dekompilierung der auf dem Geraet
  installierten JTX-Board-APK (2.17.00.ose, direkt per `adb pull` gezogen)
  fand sich die eigentliche Ursache in `SyncContentProvider.query()`:
  **JTX Boards Content-Provider ist strikt kontobezogen** - `getAccountFromUri()`
  liest `account_name`/`account_type` als Pflicht-Query-Parameter aus der URI
  und liefert sonst `null`, was danach zu einer `NullPointerException` fuehrt
  (`accountFromUri.name` auf `null`) - genau der Fehler, den `TasksTab`s
  Catch-Block als Hinweistext anzeigte. Das ist dieselbe Route, ueber die auch
  DAVx5 selbst synchronisiert (Kontotyp `bitfire.at.davdroid`, per
  `SyncApp`-Enum in JTX Board bestaetigt) - keine separate, unkontierte
  Lese-URI existiert (alle `UriMatcher`-Pfade in `SyncContentProvider`
  verlangen dieselbe Kontobindung, auch fuer `collection`).
  **Fix**: Einmalige Konto-Auswahl ueber
  `AccountManager.newChooseAccountIntent(null, null, new String[]{"bitfire.at.davdroid"}, ...)`
  - der System-Kontowaehler zeigt DAVx5-Konten an, OHNE dass EdgeTab dafuer
  die (seit Android 8 ohnehin kaum noch wirksame) `GET_ACCOUNTS`-Berechtigung
  oder eine von DAVx5 gewaehrte Sichtbarkeit braucht (direktes
  `AccountManager.getAccountsByType()` waere durch die Account-Sichtbarkeits-
  Regeln seit Android 8 blockiert gewesen). Ablauf wie bei der Icon-/Widget-
  Auswahl: `TasksTab` zeigt einen Hinweis mit Knopf „Konto auswählen", der
  ueber `MainActivity` (Extra `pick_jtx_account`) den Kontowaehler oeffnet;
  `onActivityResult` speichert Name+Typ in `Settings` (`jtxAccountName`/
  `jtxAccountType`), danach zieht sich die Leiste wieder auf. Die Abfrage-URI
  wird seither immer mit `.buildUpon().appendQueryParameter("account_name", …)
  .appendQueryParameter("account_type", …)` aufgebaut. Der Tippen-zum-Oeffnen-
  Intent (`content://at.techbee.jtx/icalobject/<id>`) brauchte KEINE Aenderung
  - das ist ein reiner Activity-`VIEW`-Intent-Filter (Host `at.techbee.jtx`,
  kein ContentProvider), JTX Board loest die ID intern gegen seine eigene
  Datenbank auf.
  **Bemerkenswerter Nebenfund**: `SyncContentProvider.query()` ruft
  `isSyncAdapter(uri)` auf, wertet den Rueckgabewert aber nirgends aus (totes
  Ergebnis) - `caller_is_syncadapter=true` wird fuer **Lesen** also gar nicht
  wirklich durchgesetzt, obwohl die Methode das suggeriert. Nur fuer Schreiben
  (nicht von EdgeTab genutzt) koennte das relevant sein.
  **Praxistest 0.30 durch Mathias: Kontowaehler zeigte KEIN Konto an.**
  Grund (in 0.31 behoben): `newChooseAccountIntent` fragt intern
  `AccountManager.getAccountsByType()` mit den Sichtbarkeitsrechten der
  AUFRUFENDEN App ab - seit Android 8 sieht eine App fremde Konten nur, wenn
  deren Besitzer-App das per `setAccountVisibility()` explizit erlaubt.
  DAVx5 tut das fuer EdgeTab nicht automatisch, darum blieb die Liste leer,
  obwohl das Konto existiert. **Fix in 0.31**: Statt (nur) auf den System-
  Kontowaehler zu setzen, gibt es jetzt einen eigenen Einrichtungsbildschirm
  (`MainActivity.showJtxAccountSetup()`) mit einem Eingabefeld fuer den
  Kontonamen (den Mathias direkt in DAVx5 abliest - dort steht er oben auf
  der Kontokarte) plus einem vorausgefuellten, aenderbaren Kontotyp-Feld
  (Standard `bitfire.at.davdroid`). Der System-Kontowaehler bleibt als
  zweite Option daneben bestehen (fuer Geraete/Situationen, wo er doch
  funktioniert), fuehrt bei Abbruch/leerer Liste aber zurueck zu diesem
  Bildschirm statt die Einrichtung kommentarlos zu beenden.
  **Jederzeit wiederholbar**: neuer Abschnitt „Aufgaben & Notizen" unten in
  Einstellungen → Registerkarten, zeigt das aktuell eingetragene Konto und
  einen Knopf „JTX-Board-Konto ändern" - vorher gab es dafuer keinen Weg
  ausser dem einmaligen Ersteinrichtungs-Hinweis in der Karte selbst.

  **Zweiter Bug, in 0.32 behoben**: Mit korrekt eingetragenem Konto
  („KontakteUndKalender"/`bitfire.at.davdroid`) kam per Live-`adb logcat`
  ein NEUER Fehler zutage: `IllegalArgumentException: Currently only
  Syncadapters are supported.` aus `SyncContentProvider.isSyncAdapter()`.
  Anders als beim ersten Fund vermutet, WIRFT diese Methode tatsaechlich
  (mein erster Blick auf den obfuszierten Code las die R8-ausgelagerte
  Wurfstelle faelschlich als reines Logging). Sie verlangt zusaetzlich zu
  `account_name`/`account_type` den Parameter `caller_is_syncadapter=true`
  in der Abfrage-URI - **exakt der von `JtxContract.asSyncAdapter()`
  vorgesehene, offiziell dokumentierte Weg**, den ich beim ersten Anlauf
  uebersehen hatte. Bemerkenswert: die Pruefung selbst ist reine
  Parameter-Anwesenheit, keine echte Berechtigungspruefung - fuer JTX Board
  unbekannte Aufrufer (wie EdgeTab) kommt sie immer durch, sobald das Flag
  gesetzt ist. Fix: `.appendQueryParameter("caller_is_syncadapter", "true")`
  vor den Konto-Parametern in beiden Karten (`TasksTab`, `NotesTab`).
  **Praxistest 0.32 durch Mathias: FUNKTIONIERT** - Aufgaben erscheinen
  jetzt korrekt.

### Notizen-Karte fertiggestellt (NEU in 0.30)
Mathias fragte direkt im Anschluss: „Kann die SW auch meine Notizen machen?
Dann bräuchte ich dafür nicht noch iMap Notes." Antwort: technisch ja, ueber
denselben Weg wie die Aufgaben-Karte - **NotesTab** ist quasi eine Kopie von
`TasksTab` mit demselben kontobezogenen JTX-Board-Zugriff (gleiche
Berechtigung, gleiche einmalige Konto-Auswahl - beide Karten teilen sich
`Settings.jtxAccountName/-Type`), nur mit Modul-Filter `module = 'NOTE' OR
module = 'JOURNAL'` statt `'TODO'`, sortiert nach letzter Aenderung, zeigt
Titel + Beschreibungsanfang (3 Zeilen) + relative Aenderungszeit; Tippen
oeffnet die Notiz zum Bearbeiten in JTX Board.
- **Wichtige Einschraenkung, Mathias explizit mitgeteilt**: Das setzt voraus,
  dass JTX Board ueberhaupt Notizen/Journal-Eintraege bekommt - das haengt
  davon ab, ob die per DAVx5 verbundene Sammlung auf seinem Server VJOURNAL
  ueberhaupt synchronisiert. Das ist eine **DAVx5/Server-Einstellung, keine
  EdgeTab-Frage** - falls die Karte leer bleibt, obwohl das Konto ausgewaehlt
  ist, liegt es dort, nicht an EdgeTab.
- Ersetzt den bisherigen `PlaceholderTab` fuer `TYPE_NOTES` (eine der
  sechs urspruenglichen Karten, weiterhin nur aus-/einschaltbar).
- **Praxistest 0.32 durch Mathias**: zeigt korrekt „Keine Notizen gefunden" -
  kein Bug, JTX Board hat schlicht (noch) keine Notiz-/Journal-Eintraege
  (Mathias' Notizen liegen bisher ausschliesslich in ImapNotes3 über IMAP,
  nichts synchronisiert bisher VJOURNAL in JTX Board hinein).
  **Root Cause recherchiert (2026-09-24)**: Laut DAVx5s eigener Dokumentation
  (manual.davx5.com/tasks_notes.html) brauchen Journale/Notizen eine **eigene,
  dediziert dafuer eingerichtete CalDAV-Sammlung** - anders als Aufgaben
  koennen sie NICHT einfach in einer bestehenden Termine+Aufgaben-Sammlung
  (wie „Gemeinsam Kalender") mit aktiviert werden. Mathias' Server ist
  **DAViCal** (Adresse deutet auf `server1.novabytes.de/davical/` hin, per
  DAVx5-Kontodetailseite abgelesen) - DAViCal unterstuetzt VJOURNAL als
  Komponententyp grundsaetzlich (Wikipedia/DAViCal-Doku), es fehlt nur eine
  entsprechend konfigurierte Sammlung auf dem Server. Das waere ein kleiner,
  gezielter Auftrag an Mathias' Mann (Server-Admin) - eine neue leere
  Sammlung mit VJOURNAL-Unterstuetzung anlegen (DAViCal-Admin-UI oder
  `davical-cli` „CREATE COLLECTION"), NICHT das grosse Exchange/grommunio-
  Thema von weiter oben. Migration der Alt-Notizen aus ImapNotes3 will
  Mathias selbststaendig ueber die macOS-„Notizen"-App klaeren (eigenes
  Vorgehen, ausserhalb von EdgeTab) - nicht weiter verfolgen, ausser er
  fragt gezielt danach.

### Exkurs (2026-09-24, kein EdgeTab-Feature): Mailserver-Architektur
Auf Mathias' Wunsch per `adb` (Konten-/Sync-Dump, Kalender-/Kontakt-Abfragen,
`jadx`-Dekompilierung von `com.blackberry.contacts`) untersucht, welche Apps
wie mit seinem Mailserver (DAViCal, siehe oben) sprechen:
- **Kalender/Kontakte werden zweifach synchronisiert**: DAVx5
  (`KontakteUndKalender`) UND BlackBerry Infrastructures eigener,
  unabhaengiger CalDAV/CardDAV-Client (`com.blackberry.dav.caldav`/
  `com.blackberry.dav.carddav`, Konto `telefonmann@telefonanleitungen.de`) -
  beide schreiben in dieselben Android-Standard-Provider
  (`com.android.calendar`/`com.android.contacts`).
- **Kalender-Seite ist bei BB inaktiv** (kein einziger BB-Kalender in der
  Kalendertabelle, kein periodischer Sync) - reine Karteileiche, ungefaehrlich.
- **Kontakt-Seite laeuft bei BB aktiv** (stuendlicher `PERIODIC`-Sync,
  per `adb shell content query` bestaetigt: exakt 504 Rohkontakte doppelt -
  einmal unter `bitfire.at.davdroid.address_book`, einmal unter
  `com.blackberry.dav.carddav`, identische Quelle).
- **Erster Vorschlag (BB-CardDAV-Konto entfernen) war FALSCH** - von Mathias
  live widerlegt: BlackBerry Contacts nutzt entgegen meiner ersten
  Code-Analyse NICHT die Android-Standard-Kontaktdatenbank fuer die
  Kontenauswahl-Anzeige, sondern eine eigene Autoritaet
  `com.blackberry.unified.contacts.provider` (gefunden in
  `CustomContactListFilterActivity.java`), die nur BB-eigene Kontotypen
  (+ „Lokal") kennt - DAVx5 taucht dort nicht als Option auf. Deaktivieren
  des BB-Kontos macht dort wirklich ALLES leer (von Mathias getestet).
  **Nicht antasten, solange BB Contacts aktiv genutzt wird.**
- **Korrigierte Empfehlung**: Stattdessen in DAVx5 nur das eine ueberlappende
  Adressbuch „telefonmann@telefonanleitungen.de addressbook" (der
  DAVx5-Kopie, Konto-Nr. #51) deaktivieren - BBs eigener CardDAV-Sync speist
  weiterhin ganz normal die Standard-Provider (aus denen auch EdgeTabs
  Kontakte-Karte liest), nur eben nicht mehr doppelt von DAVx5 UND BB
  gleichzeitig.
- Diese Recherche fuehrte direkt zum Notizen-Befund oben (DAViCal-Identifikation
  via URL, VJOURNAL-Anforderung).

### Aufgaben abhakbar (NEU in 0.33)
Mathias-Wunsch: „Kann man Notizen [eigentlich: Aufgaben, Tippfehler in der
Frage] abhakbar machen, wenn sie erledigt sind?" Jede Aufgaben-Zeile hat
jetzt links eine Checkbox.
- **Schreibzugriff auf JTX Board**: neue Berechtigung
  `at.techbee.jtx.permission.WRITE` (separat von `READ`, beide vom Nutzer
  unabhaengig voneinander erteilbar). Wird beim ersten Antippen der Checkbox
  auf Nachfrage angefordert (generischer Mehrfach-Berechtigungs-Weg in
  MainActivity, siehe unten) - nicht schon beim ersten Oeffnen der Karte, um
  nicht mehr als noetig abzufragen.
- **JTX Boards `update()` ist ein reines SQL-UPDATE ohne automatische
  Sync-Markierung** (per erneuter Dekompilierung von `SyncContentProvider`
  bestaetigt) - anders als man erwarten wuerde, setzt es weder `dirty` noch
  `lastmodified` von selbst. EdgeTab setzt daher explizit
  `status=COMPLETED`, `percent=100`, `completed=<jetzt>` UND **`dirty=1`**
  selbst - ohne das letzte wuerde die Erledigung nie zu DAVx5 und damit nie
  zum Server hochgeladen werden, sondern nur lokal in JTX Board sichtbar
  bleiben.
- **Bekannte Einschraenkung**: wiederkehrende Aufgaben (mit `RRULE`) ruecken
  beim Abhaken ueber EdgeTab NICHT automatisch zum naechsten Termin vor -
  das behandelt nur JTX Boards eigene App gesondert. Nur relevant fuer
  Aufgaben mit Wiederholung.
- `MainActivity`s Berechtigungs-Relais generalisiert von einer einzelnen
  Berechtigung (`request_permission`-String) zu wahlweise mehreren
  (`request_permissions`-String-Array) - fuer den Fall, dass eine Karte
  READ+WRITE zusammen braucht.

### Mediensteuerung: groesser + feste Schnellleiste (0.35/0.36)
Praxistest von 0.34 bestaetigte den Dedupe-Fix (AIReaderX zeigt nur noch
eine Karte). Zwei direkte Folgewuensche:
- **0.35**: Cover (48→72dp), Titel/Interpret-Schrift und die drei
  Steuerknoepfe deutlich vergroessert ("Geil so. Kann man das noch
  vergrößern?").
- **0.36**: Mathias erklärte, warum das Original so gut war - Bedienelemente
  sollten einhaendig mit dem Daumen der haltenden Hand erreichbar sein,
  wahlweise oben/mittig/unten je nach Nutzer. Umgesetzt als **feste,
  scroll-unabhaengige Schnellleiste**: `MediaTab.buildContent()` liefert
  jetzt ein `FrameLayout` (ScrollView mit allen Sitzungskarten + eine
  darueber gelegte, fest positionierte Leiste nur fuer die "wichtigste"
  Sitzung, per `pickPrimary()`/`score()` ermittelt - spielend vor pausiert
  vor nacktem Zustand). Position per neuer Einstellung
  `Settings.mediaControlsPos()` ("top"/"middle"/"bottom", Standard "bottom")
  unter Einstellungen → Position & Aussehen → "Mediensteuerung". Die
  scrollbare Liste bekommt oben/unten passendes Padding, damit die Leiste
  keinen Eintrag dauerhaft verdeckt.
- **Nebenbei erklaert (kein Code-Thema)**: physische Play/Pause-Taste am
  Geraeterand (wie bei BB10) ist mit Bordmitteln nicht nachbaubar - Android
  liefert screen-off Lautstaerke-/Seitentasten nicht an Drittanbieter-Apps
  aus. Gleichzeitig leise Musik + laute Vorlesefunktion (Audio-Ducking)
  scheitert an Androids Audiofokus-System - haengt vom Verhalten der
  jeweiligen Apps ab (`AUDIOFOCUS_GAIN` vs. `..._MAY_DUCK`), nicht von
  EdgeTab beeinflussbar.

### Zahnrad-Konsistenz + Registerkarten-Anordnung (0.37)
- **Zahnrad vereinheitlicht**: `ShortcutsTab` und `ContactsTab` nutzten fuer
  ihr Mini-Einstellungsmenue ein reines Unicode-Zeichen ("⚙" als TextView) -
  ersetzt durch dasselbe `R.drawable.ic_settings`-Icon (mit Tint), das auch
  die Haupteinstellungen-Karte in der Icon-Spalte verwendet. Optisch
  konsistent.
- **Registerkarten-Spalte umsortiert**: neuer, gewichteter Spacer-`View`
  zwischen dem Einstellungen-Zahnrad (samt Trennlinie) und den restlichen
  Karten-Icons in `EdgeService.buildIconColumn()` - druckt alle regulaeren
  Karten an den UNTEREN Rand der Spalte, nur das Zahnrad bleibt oben
  abgesetzt (statt direkt darunter aufgereiht). Mathias' Wunsch nach
  einhaendiger Erreichbarkeit, konsistent mit dem Mediensteuerungs-Wunsch
  oben.

### Kalendereintrag hinzufuegen (NEU in 0.37)
Mathias-Wunsch: "Kann man eine Kalendereintrag Hinzufügen Funktion bauen,
wie in der BB SW?" Per `apktool` an `com.blackberry.calendar` (installierte
APK, `adb pull`) das Feldset von `edit_event_3.xml` abgelesen - vollstaendiger
AOSP-Calendar-Fork mit Titel/Ort/Ganztaegig/Beginn/Ende/Wiederholung/
Erinnerungen/Teilnehmer/Kategorien/Sichtbarkeit/Zeitzone/Anhaengen. EdgeTab
uebernimmt bewusst nur die fuer eine schnelle Rand-Erfassung sinnvolle Teilmenge:
- Neuer Bildschirm `MainActivity.showAddEvent()` (Extra `add_event`, eigener
  Berechtigungs-Weg `REQ_WRITE_CAL` fuer die neue `WRITE_CALENDAR`-
  Berechtigung - separat von `READ_CALENDAR`, das schon beim Ersteinrichten
  verlangt wird).
- Felder: Titel, Kalenderauswahl (nur Kalender mit
  `CALENDAR_ACCESS_LEVEL >= CAL_ACCESS_CONTRIBUTOR`, sonst wuerde das
  Einfuegen ohnehin scheitern), Ganztaegig-Schalter, Beginn/Ende (Knopf oeffnet
  `DatePickerDialog` dann bei Bedarf `TimePickerDialog`), Ort, Beschreibung,
  Erinnerung (Keine/10min/30min/1h/1 Tag vorher).
  **Bewusst weggelassen**: Teilnehmer, Kategorien, Sichtbarkeit/Privatsphaere,
  Zeitzonen-Auswahl, Anhaenge, Wiederholung (RRULE) - passt nicht zum
  schlanken Rand-Charakter der App, kann bei Bedarf spaeter ergaenzt werden.
- Ganztaegige Termine: `DTSTART`/`DTEND` auf Mitternacht UTC normalisiert
  (CalendarContract-Konvention - sonst landet der Termin auf dem falschen
  Tag).
- Einstiegspunkt: neuer "+ Termin hinzufügen"-Knopf ganz oben in der
  Kalender-Karte (`CalendarTab.addEventButton()`).
- **Ungetestet auf dem Geraet** - naechste Sitzung/Mathias zuerst pruefen:
  Berechtigungsdialog, Datum-/Zeitauswahl, Speichern landet im richtigen
  Kalender, Erinnerung loest aus.

### Zahnrad-Richtung korrigiert, Termin/Kontakt ueber Systemapps, echtes Antworten (0.38-0.42)
Nach Praxistest von 0.37 mehrere Korrekturen/Erweiterungen:

- **Zahnrad-Richtung war falsch herum (0.38)**: Mathias meinte mit "Zahnrad
  vereinheitlichen" das GENAUE GEGENTEIL von dem, was 0.37 umsetzte - nicht
  ShortcutsTab/ContactsTab sollten das Vektor-Icon der Haupteinstellungen
  uebernehmen, sondern umgekehrt: das Haupt-Zahnrad oben in der Icon-Spalte
  sollte zum simplen "⚙"-Unicode-Zeichen werden (das die beiden anderen
  Tabs schon hatten). `EdgeService.gearGlyphView()` neu - Text statt Bild,
  gleiche Box-Masse wie `iconView()`.
- **"+ Termin hinzufügen" und neues "+ Kontakt hinzufügen" nutzen jetzt die
  System-App (0.38)**: Mathias' Feedback zum eigenen Termin-Formular aus
  0.37: "Der Erstellen-Dialog ist nett, aber besser ist der des Kalenders
  selber. Da geht mehr. Und mehrere, verschiedene zu bedienende sind doof."
  → kompletter Rueckbau des eigenen Formulars (`MainActivity.showAddEvent()`
  samt Helfern, `WRITE_CALENDAR`-Berechtigung, `REQ_WRITE_CAL`) zugunsten
  von `Intent.ACTION_INSERT` auf `CalendarContract.Events.CONTENT_URI` bzw.
  `ContactsContract.Contacts.CONTENT_URI` - startet die auf dem Geraet
  installierte Kalender-/Kontakte-App mit ihrem eigenen, volleren Editor.
  Kein Formular-Duplikat mehr in EdgeTab, keine Zusatzberechtigung noetig
  (die gestartete App schreibt mit ihren eigenen Rechten).
- **Mediensteuerung: "immer noch zwei" + "Position ändert sich nicht" (0.40/0.41)**:
  Zwei verschiedene Ursachen hintereinander gefunden:
  1. (0.40) Die "wichtigste" Sitzung wurde als GETRENNTE kompakte Leiste
     UND als Info-Karte (ohne Knoepfe) in der Liste gezeigt - wirkte wie
     doppelt, weil dieselbe Wiedergabe an zwei Stellen auf dem Bildschirm
     auftauchte. Fix: `pinnedBar()` entfernt, stattdessen wird fuer die
     wichtigste Sitzung `sessionCard()` (volle Karte mit Cover+Titel+Knoepfen)
     einmal fest positioniert gezeigt, NICHT zusaetzlich in der Liste.
  2. (0.41, der eigentliche Grund fuer "Position ändert sich nicht"):
     `EdgeService.renderContent()` haengte den Karteninhalt ohne feste Hoehe
     in ein LinearLayout ("wrap") - ein WRAP_CONTENT-Container gibt einem
     `FrameLayout` mit `Gravity`-Positionierung (wie die Mediensteuerung sie
     fuer oben/mitte/unten nutzt) gar keine Restflaeche, in der sich etwas
     bewegen koennte (ein FrameLayout waechst unter WRAP_CONTENT nur auf die
     Groesse seines Inhalts, anders als eine ScrollView, die AT_MOST-Platz
     automatisch ausfuellt - deshalb fiel es bei den anderen, ScrollView-
     basierten Karten nie auf). Fix: sowohl `content.addView(wrap, ...)` als
     auch `wrap.addView(tabInhalt, ...)` bekommen jetzt explizite
     MATCH_PARENT- bzw. Gewicht-1-LayoutParams, damit am Ende eine echte,
     definierte Resthoehe existiert. Betrifft technisch alle Karten (reine
     Absicherung, fuer ScrollView-Karten kein sichtbarer Unterschied).
  Wichtiger Nebenfund: die zwei gleichzeitig gezeigten Sitzungen im
  Screenshot (VLC + AIReaderX) waren gar kein Bug - beide waren echt aktiv,
  jede App bekommt zu Recht ihre eigene Karte.
- **Echtes "Antworten" im Posteingang (0.39/0.42)**: Mathias' Wunsch, an BBs
  eigener Hub-/Kontakte-/Kalender-Oberflaeche orientiert (Stift-Symbol fuer
  Hinzufuegen, Antworten/Loeschen-Wischgesten im Hub). "Loeschen" gab es
  schon (Wiederherstellen-Leiste, `deleteInApp`). "Antworten" fehlte
  komplett, jetzt in zwei Stufen gebaut:
  - 0.39: `NotificationCollector.findReplyAction()` sucht nach einer Aktion
    mit echter Direkteingabe (`RemoteInput` mit `getAllowFreeFormInput()`) -
    genau das Verfahren, das Telegram/Signal/Molly fuer ihre Direktantwort
    aus der System-Benachrichtigung heraus nutzen. Neue Methode
    `Launcher.send(ctx, pi, fillInIntent, opts)` (Ueberladung), da
    `RemoteInput.addResultsToIntent()` ein "Fill-in"-Intent braucht, das die
    bisherige `send()`-Methode nicht durchreichte. `InboxTab` zeigt bei
    solchen Nachrichten ein "↩" neben dem "✕", klappt ein Eingabefeld auf.
  - **Live-Test durch Mathias mit einer echten Hub-Mail zeigte: "Allen
    antworten" bei Hub existiert (per `dumpsys notification --noredact`
    bestaetigt: `actions={[0] "Allen antworten" -> PendingIntent{...
    startActivity...}}`), hat aber KEIN `remoteInputs` - reine
    startActivity-Aktion (oeffnet Hubs eigenen Compose-Bildschirm), keine
    Inline-Eingabe.** Ohne RemoteInput fand `findReplyAction()` sie nicht,
    das "↩" blieb bei der Hub-Mail unsichtbar.
  - **0.42**: `findAnyReplyAction()` ergaenzt - erkennt zusaetzlich Aktionen
    OHNE RemoteInput per Semantik (`SEMANTIC_ACTION_REPLY`) oder Beschriftung
    ("antwort"/"reply", analog zu `findDeleteAction`s Beschriftungs-
    Fallback). `InboxTab` unterscheidet beim Tippen auf "↩": mit
    RemoteInput → Textfeld; ohne → `NotificationCollector.openReplyAction()`
    sendet den PendingIntent direkt (oeffnet Hubs eigenen Antwortbildschirm),
    genau wie ein Tipp auf die Aktion in der System-Benachrichtigung selbst.
  - **Noch ungetestet**: ob 0.42 bei Hub jetzt tatsaechlich in den
    Antwortbildschirm springt, und ob bei Telegram/Molly mit einer frischen
    Nachricht die echte Direkteingabe (Textfeld) erscheint.
- **BBM Enterprise als Chat-Datenquelle geprueft und verworfen (2026-09-24)**:
  Auf Mathias' Wunsch `com.bbm.enterprise` (lokale APK) per `apktool`+`jadx`
  untersucht. Eigener `BBMContentProvider` existiert, ist `exported=true`
  OHNE Manifest-Berechtigung - sieht zunaechst offener aus als der BB-Hub-
  Weg. Der Code selbst prueft aber zwei eigene Rechte
  (`com.bbm.enterprise.permission.conversations.READ/WRITE`), die NIRGENDS
  im Manifest deklariert sind - solche Rechte kann grundsaetzlich KEINE App
  je erhalten (Android erfordert eine `<permission>`-Deklaration durch
  irgendeine installierte App, bevor irgendjemand sie bekommen kann). Deutet
  auf eine versteckte, im Code fest verdrahtete Positivliste hin (typisch
  fuer BlackBerrys Enterprise-/MDM-Begleit-Apps) statt eines normalen,
  gewaehrbaren Zugriffs. **Gleiche Sackgasse wie beim Hub, nur anders
  umgesetzt - nicht weiter verfolgen.**

### Schwebende Symbol-Knoepfe wie bei BB (NEU in 0.43)
Mathias-Wunsch: "Ähnliche Symbole wie bei BB für neue Einträge?" (nach seinen
Screenshots: gruener Stift bei Hub/Kalender, blaues Personen-Symbol bei
Kontakten - runde, farbige, schwebende Knoepfe statt Text-Buttons).
- Neue Helfer in `BaseTab`: `fab(ctx, iconRes, farbHex, onClick)` (rundes
  ImageView mit ovalem `GradientDrawable`-Hintergrund, Schatten via
  `setElevation`) und `withFab(ctx, scroll, fab)` (legt scroll + Symbol in
  ein gemeinsames `FrameLayout`, Symbol bleibt beim Scrollen an fester
  Stelle unten-mittig - selbes Prinzip wie die feste Mediensteuerung-Karte).
- Neue Vektor-Icons `ic_fab_edit.xml` (Stift) und `ic_fab_person_add.xml`
  (Person mit Plus) - Standard-Material-Icon-Pfade, keine BB-Originalgrafik.
- `CalendarTab`: alter Text-Knopf "+ Termin hinzufügen" ersetzt durch
  gruenes (#3DA764) schwebendes Stift-Symbol.
- `ContactsTab`: alter Text-Knopf "+ Kontakt hinzufügen" (war im ⚙-Menue
  versteckt) ersetzt durch blaues (#2E9BE6) schwebendes Personen-Symbol,
  jetzt IMMER sichtbar (waehrend der Kontaktauswahl "picking" ausgeblendet)
  statt hinter dem Zahnrad - naeher an BBs eigenem, staendig erreichbarem
  Knopf.
- Posteingang bewusst ausgelassen: anders als Kalender/Kontakte gibt es
  keine sinnvolle, app-uebergreifende "neue Nachricht"-Absicht (jede
  Messaging-App braucht ihre eigene Compose-Ansicht) - ein FAB dort haette
  kein generisches Ziel.
- **Breiteres "Design insgesamt ähnlicher" (BBs Navyblau-Farbschema etc.)
  noch nicht angegangen** - bewusst auf diesen konkreten, klar umrissenen
  Wunsch beschraenkt; falls Mathias das weiterverfolgen will, gesondert
  angehen (Farbpalette ist ein groesserer, app-weiter Eingriff).

### Weitere schwebende Stift-Knoepfe + farbiger Balken im Posteingang (NEU in 0.44)
Fortsetzung von 0.43, nach Mathias' zwei BB-Screenshots (Posteingang mit
farbigem Balken je Zeile + gelbem Verfassen-Stift; Aufgaben mit gruenem
Stift):
- **Posteingang**: Zeilen sind jetzt wie im Kalender aufgebaut (aeusseres
  horizontales Layout mit farbigem Balken links + Inhalt rechts statt
  einer reinen vertikalen Karte). Farbe kommt aus neuem `BaseTab.colorFor
  (String key)` - da es (anders als bei Kalendern) keine "echte" Farbe je
  Quelle gibt, wird sie stabil aus dem Paketnamen erzeugt (HSV-Hash), damit
  jede Quell-App durchgehend dieselbe Farbe behaelt. Neuer oranger (#F5A623)
  schwebender Stift startet `Intent.ACTION_SENDTO` mit `mailto:` - das ist
  die einzige wirklich universelle "neue Nachricht"-Systemabsicht (jede
  andere Messaging-App braucht ihre eigene Compose-Ansicht, kein
  generischer Weg moeglich).
- **Aufgaben**: neuer gruener (#3DA764) schwebender Stift oeffnet JTX Board
  selbst (`getLaunchIntentForPackage`) - anders als bei Kalender/Kontakten
  gibt es dort keine System-weite "neue Aufgabe"-Absicht, die EdgeTab
  nutzen koennte (JTX Boards Manifest wurde dafuer per `apktool` gezielt
  geprueft: kein passender Intent-Filter ausser dem normalen App-Start und
  einem Text-Teilen-Empfang). Kein farbiger Balken bei Aufgaben - Mathias'
  eigener Aufgaben-Screenshot von BB zeigt dort auch keinen, nur Checkbox +
  Text auf einheitlichem Kartenhintergrund.
- Gemeinsame neue Helfer in `BaseTab`: `fab()`/`withFab()` (aus 0.43) sowie
  `colorFor()` (neu).
- **Ungetestet auf dem Geraet**.

### Posteingang-Gruppierung + Widget-Zahnrad (NEU in 0.45)
- **Posteingang nach Tagen gruppiert** wie der Kalender: HEUTE/GESTERN/
  VORGESTERN, sonst Wochentag+Datum - `InboxTab.dayHeader()`/`dayIndex()`,
  gleiches Prinzip wie `CalendarTab`, nur rueckwaerts (Vergangenheit) statt
  vorwaerts (Zukunft).
- **WidgetTab**: Text-Knopf "Widget auswählen"/"+ Weiteres Widget" ersetzt
  durch schwebendes graues (#5A5A5E) Zahnrad-Symbol (`ic_settings`) - Zahnrad
  statt Stift, weil "Widget hinzufügen" eher Einrichtung als Inhalt ist.
- **Hinweis an Mathias, kein Code-Thema**: die Widget-Karte selbst (seit
  0.15/0.17) erlaubt bereits, BB Hubs eigenes Posteingang-Widget direkt
  einzubetten - eine bestehende, ungenutzte Option als moegliche Ergaenzung/
  Alternative zur eigenen, Benachrichtigungs-basierten Posteingang-Karte.
  Nichts Neues gebaut, nur wieder ins Bewusstsein gerufen.
- **Ungetestet auf dem Geraet**.

### Kontakte-Karte fertiggestellt (NEU in 0.25)
War bisher `PlaceholderTab`. Jetzt echte Karte **ContactsTab** ueber
`ContactsContract` - Mathias' Wunsch: „wahlweise alle/ausgewaehlte/
Favoriten".
- **Neue Berechtigung** `READ_CONTACTS` im Manifest - anders als Kalender
  aber NICHT im Ersteinrichtungs-Zwang, sondern **erst bei Bedarf**
  abgefragt (Prinzip: nur fragen, wenn die Karte auch aktiv genutzt wird).
  Dafuer neuer Ablauf in `MainActivity`: Extra `request_contacts=true` (wie
  `pick_icon`/`pick_widget` aufgebaut, `pendingContactsRequest`-Flag gegen
  doppeltes Auslösen), `onRequestPermissionsResult` neu ueberschrieben -
  baut nach der Systemabfrage (egal ob erlaubt/abgelehnt) die Leiste wieder
  auf; die Karte prueft die Berechtigung bei jedem Aufbau selbst neu.
- **Drei Modi** (`TabInstance.contactsMode`: `"all"`/`"selected"`/
  `"favorites"`, Standard `"favorites"`): "Favoriten" filtert per SQL auf
  `ContactsContract.Contacts.STARRED=1`; "Ausgewählt" filtert clientseitig
  nach `TabInstance.selectedContacts` (Liste von Lookup-Keys); "Alle" zeigt
  ungefiltert, sortiert nach `SORT_KEY_PRIMARY`.
  Umschalten wieder ueber das Ein-Zahnrad-Menue (wie bei ShortcutsTab).
- Bei Modus "Ausgewählt": „Kontakte auswählen…" oeffnet eine Checkbox-Liste
  aller Kontakte (`contactPicker`, gleiches Muster wie ShortcutsTab.appPicker).
- Tippen auf einen Kontakt oeffnet ihn in der System-Kontakte-App
  (`ACTION_VIEW` auf `CONTENT_LOOKUP_URI`); Foto wird aus
  `PHOTO_THUMBNAIL_URI` geladen, Platzhalter sonst `ic_contacts`.
- Bleibt wie die anderen fuenf urspruenglichen Karten **einfach** (nicht
  mehrfach anlegbar) - passt zum bisherigen Muster (nur Widget/Verknuepfungen
  sind mehrfach moeglich).
- **Bug in 0.25, behoben in 0.26**: Mathias' Test - "Favoriten" und "Alle"
  gingen, "Ausgewählt" nicht. Ursache: der "Kontakte auswählen…"-Knopf war
  an `menuOpen` gekoppelt, aber `modeButton` schliesst das Menue sofort beim
  Waehlen eines Modus (`MENU_OPEN.remove`) - der Knopf verschwand also im
  selben Moment, in dem er noetig wurde, der Modus "selected" blieb ohne
  sichtbaren Weg, tatsaechlich Kontakte auszuwaehlen. Fix: die Sichtbarkeit
  des Knopfs haengt jetzt nur noch an `contactsMode=="selected"`, nicht mehr
  zusaetzlich an `menuOpen`.
- **0.26 getestet, funktioniert - ein Rest-Problem gefunden und in 0.27
  behoben**: Der "Kontakte auswählen…"-Dialog erschien, aber die Kaestchen
  waren praktisch unsichtbar (Mathias dachte zunaechst, nichts sei
  auswaehlbar). Ursache: Ohne eigene Farbe bekommt eine `CheckBox` im
  Overlay-Fenster kaum Kontrast zur dunklen Karte (die Service-Ansicht hat
  kein eigenes Theme wie `EdgeTabTheme`, das nur fuer MainActivity gilt).
  Fix: `cb.setButtonTintList(...)` mit dem App-Akzentblau `#2E9BE6`
  - deutlich sichtbar in beiden Zustaenden. **Hinweis fuer spaeter**: andere
  Checkboxen im Projekt (SettingsTab, ShortcutsTab, tabRow) haben dieselbe
  fehlende Faerbung und koennten am selben Problem leiden, wurden aber noch
  nicht angetastet, da Mathias das nur fuer diese eine gemeldet hat -
  bei Bedarf gleiche Loesung anwenden.
- **0.27 (mit dem Kaestchen-Fix) noch nicht bestaetigt** - naechste Sitzung
  zuerst pruefen, ob die Kaestchen jetzt gut sichtbar sind.

### Idee (noch nicht umgesetzt): Registerkarte fuer Mediensteuerung
Mathias' Idee. Eingeschaetzt: **gut machbar, keine der bisherigen
Berechtigungs-Sackgassen** - EdgeTab ist bereits ein
`NotificationListenerService` (fuer den Posteingang), und genau das ist der
offizielle, oeffentlich dokumentierte Weg, wie eine App ohne
Sonderberechtigung an systemweite Medien-Sessions kommt: 
`MediaSessionManager.getActiveSessions(new ComponentName(ctx,
NotificationCollector.class))` liefert `MediaController`-Objekte (Titel,
Interpret, Cover, Play/Pause/Skip/Seek) fuer alle gerade aktiven
Wiedergabe-Apps - kein `MEDIA_CONTENT_CONTROL` noetig (das ist
signaturgeschuetzt), weil ein aktiver Notification-Listener explizit davon
ausgenommen ist. Waere eine neue Karte `MediaTab` nach demselben Muster wie
die anderen. **Noch nicht begonnen** - naechster Schritt, wenn Mathias das
bestaetigt.

### Frage geklaert: Mehrsprachigkeit
Mathias hat gefragt, wie aufwendig andere Sprachen waeren. Antwort: technisch
einfach (Android-Standard: `res/values/strings.xml` + `res/values-en/
strings.xml` usw., automatische Auswahl nach Geraetesprache) - ABER fast
der gesamte Text steht aktuell als Literal direkt im Java-Code, nicht in
`strings.xml`. Der eigentliche Aufwand waere also erst, alle Texte
auszulagern (viele mechanische Aenderungen ueber gut 10 Dateien), bevor
Uebersetzen ueberhaupt anfangen kann. **Bewusst zurueckgestellt** - die App
nutzt aktuell nur Mathias auf Deutsch. Nur anfangen, wenn er das explizit
will.

## 5. Nächste Schritte (grob priorisiert)
1. **0.17 auf dem Gerät testen** (noch ungetestet, s. Abschnitt „Registerkarten-
   Umbau"): eigenes Icon waehlen (erstes EditText/erste Bildauswahl im
   Projekt), umbenennen, mehrere Widget-Karten anlegen, mehrere Widgets in
   einer Karte stapeln, „nach Tippen schließen" bei einem echten Widget
   (Hub) pruefen, Reihenfolge per ▲▼ aendern, aktive Hinterlegung ansehen.
2. Falls „nach Tippen schließen" bei Widgets zu unzuverlaessig ist (siehe
   Einschraenkung oben): ggf. verfeinern oder als reine Bestenfalls-Funktion
   belassen.
3. Falls der Widget-Weg fuer den Posteingang langfristig nicht traegt:
   **IMAP-Client** für einen benachrichtigungsunabhängigen Posteingang erwägen.
4. ~~Hub-Löschen~~ - **erledigt** (2026-09-24, in 0.22, von Mathias bestätigt).
   Ursache und Fix siehe Abschnitt „Bekannte, noch offene Punkte" oben.
5. ~~Aufgaben-Karte~~ - **erledigt** (0.29, Kontobindungs-Bug behoben in 0.30):
   über JTX Boards eigenen, offenen, aber kontobezogenen Content-Provider
   (nicht CalDAV direkt - siehe Abschnitt oben). ~~Notizen-Karte~~ - ebenfalls
   **erledigt** (0.30), gleicher Provider/gleiches Konto, `module = "NOTE"/
   "JOURNAL"`. Beide noch ungetestet mit Mathias' echtem DAVx5-Konto.
6. ~~Kontakte-Karte~~ - **erledigt** (0.25-0.27): Alle/Ausgewählt/Favoriten
   über `ContactsContract`, `READ_CONTACTS` erst bei Bedarf abgefragt.
7. ~~Mediensteuerung-Karte~~ - **erledigt** (0.28), siehe Abschnitt oben.
   Noch ungetestet auf dem Geraet.
8. **Exkurs, kein EdgeTab-Thema**: Mathias wollte lieber die BB-eigenen
   Aufgaben-/Notizen-Apps nutzen (findet sie besser durchdacht als JTX
   Board/iMAP Notes 3), die aber nur ueber ein Exchange/EWS-Konto
   ("Unified" in der BB-Infrastruktur) synchronisieren - siehe Abschnitt
   weiter unten fuer die volle Herleitung (Manifest-Analyse aller
   Kontotypen in `com.blackberry.infrastructure`). Z-Push (freie
   ActiveSync-Bruecke vor bestehenden CalDAV/IMAP-Servern) scheidet aus -
   die dafuer noetige Abspaltung `fmbiete/Z-Push-contrib` ist seit August
   2023 archiviert/tot, mit einem offenen, nie behobenen Bug genau bei
   Aufgaben-Synchronisation. **Vielversprechendere Alternative**:
   [grommunio](https://grommunio.com/) - aktiv gepflegte, quelloffene
   (AGPLv3) Exchange-Alternative, fuer eine Einzelperson kostenlos (Community
   Edition bis 5 Nutzer), mit EAS UND CalDAV/CardDAV als gleichwertige,
   native Zugaenge zu derselben eigenen Groupware-Datenbank ("gromox") -
   kein Uebersetzungs-Bug wie bei Z-Push zu erwarten. Bedeutet aber einen
   Umzug der Mail-/Kalender-/Aufgaben-Daten dorthin, nicht nur eine duenne
   Bruecke vor dem bestehenden Server. **Das ist Mathias' eigenes
   Server-Projekt, unabhaengig von EdgeTab** - hier nur dokumentiert, falls
   das Thema wiederkommt.
9. **Restzeit im Kalender** wie BB („30 MIN"/„03 HR").
10. Eigene Karten/Reihenfolge per Drag; optional live-tickende Uhr.

---

## 6. Kurzregeln fürs Weiterarbeiten
- Deutsch, knapp, ehrlich bei Fehlern (Mathias' Präferenz).
- Jede neue Funktion als eigener **Tab** einklinken – Panel bleibt unberührt.
- Nach jeder Änderung neu bauen, mit **Mathias' Keystore** signieren, APK +
  aktualisiertes Quellcode-Zip liefern, versionName/-Code hochziehen, Übergabe-MD
  aktualisieren.
- Bei d8-Fehlern: `--release 11`, benannte innere Klassen, **kein Comparator**.
- `set -e` im Bau; danach versionName und neue Klassen im APK gegenprüfen.
