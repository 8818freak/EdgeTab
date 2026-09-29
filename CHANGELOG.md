# Changelog — EdgeTab

Alle nennenswerten Änderungen, neueste zuerst. Vor 0.14 nicht im Detail
dokumentiert (allerfrüheste Aufbauphase: Grundgerüst, Rand-Griff, erste
Karten als Platzhalter).

## 0.85
- Intern: Das Absturz-/Diagnose-Protokoll kommt jetzt aus der gemeinsamen
  Bibliothek herbers-android-common (de.herbers.common.DiagLog / Diagnostics)
  statt aus eigenem Code – dieselbe Diagnose wie in Sucher und ActiveFrames.
  Die automatische Dienst-Neustart-Logik nach einem Absturz bleibt unverändert.
  Keine sichtbare Änderung.

## 0.84
- Intern: Die Aufzählung wählbarer Benachrichtigungsquellen (alle startbaren
  Apps) kommt jetzt aus der gemeinsamen Bibliothek herbers-android-common
  (de.herbers.common.Apps) statt aus eigenem Code – dieselbe Logik wie in
  Sucher. Keine sichtbare Änderung.

## 0.83
- Intern: Der Kern des Benachrichtigungs-Mitschnitts (Titel/Text-Auslesen inkl.
  BigText, Gruppen-/Leer-Filter, Inhalts-Signatur und die Aktions-Erkennung für
  Antworten, Löschen und „als gelesen markieren") kommt jetzt aus der
  gemeinsamen Bibliothek „herbers-android-common" (de.herbers.common.
  Notifications) statt aus eigenen Kopien – dieselbe, gepflegte Logik wie künftig
  in Sucher, ActiveFrames und BBMePing. Keine sichtbare Änderung.

## 0.82
- Intern: Einstellungs-Sicherung und die Farbwahl je Quelle (colorFor) kommen
  jetzt aus der gemeinsamen Bibliothek „herbers-android-common" (Git-Submodul,
  de.herbers.common.SettingsBackup / ColorUtil) statt aus eigenen Kopien –
  dieselbe, gepflegte Logik wie in Sucher und ActiveFrames. Keine sichtbare
  Änderung, Sicherungsformat unverändert.

## 0.81
- Posteingang: Neuer Knopf „✓" je Nachricht löst die „Als gelesen markieren"-
  Aktion der System-Benachrichtigung aus (sofern die Quell-App eine anbietet)
  und markiert den Eintrag in EdgeTab als gesehen – ohne die App zu öffnen oder
  die Nachricht zu löschen. Erkennung über die semantische Aktion bzw. die
  Beschriftung („gelesen"/„mark as read").
- Fehlerbehebung Mediensteuerung: Der Tipp auf den Textbereich holt die
  Wiedergabe-App jetzt tatsächlich in den Vordergrund. Zuvor schloss sich zwar
  die Leiste, der App-Start aus dem Dienst wurde von Android 14 aber lautlos
  blockiert; jetzt mit ausdrücklicher Hintergrund-Start-Erlaubnis (wie beim
  Öffnen aus dem Posteingang).

## 0.80
- Mediensteuerung: Ein Tipp auf den Textbereich einer Wiedergabe (App-Name,
  Titel, Interpret) öffnet jetzt direkt die Wiedergabe-App – bevorzugt deren
  eigene „Aktuelle Wiedergabe"-Oberfläche (über die von der Medien-Sitzung
  angebotene Aktion), sonst der normale App-Start; danach schließt sich das
  Panel. Die Steuertasten (⏮ ⏯ ⏭) bleiben davon unberührt.

## 0.79
- Tab-Spalte springt nicht mehr: Das Einstellungs-Zahnrad ist fest oben
  angeheftet, die übrigen Karten-Symbole fest unten – unabhängig davon, welcher
  Tab geöffnet ist. Vorher richtete sich die Höhe des Leisten-Körpers nach dem
  jeweiligen Karteninhalt, wodurch die unten verankerten Symbole je nach Tab
  nach oben oder unten wanderten.

## 0.78
- Karte „Aktive Kacheln" (Widget 2): Der rote „Neues"-Stern ist jetzt
  derselbe fünfstrahlige Stern wie im Home-Screen-Widget. Vorher wurde das
  Schriftzeichen „✳" verwendet, das je nach Geräteschriftart sechs- oder
  achtstrahlig erschien.
- Karte „Aktive Kacheln" scrollt jetzt zuverlässig, wenn mehr Kacheln als
  sichtbar eingestellt sind: Das Raster wird nicht mehr über ein GridLayout
  gebaut (das misst seine Höhe in einer ScrollView unzuverlässig und scrollte
  dann nicht), sondern über verschachtelte Reihen.

## 0.77
- Neue Karte „Aktive Kacheln" (Widget 2): zeigt die zuletzt benutzten Apps
  als Kacheln in der Leiste - wie BlackBerry OS10, oben links die zuletzt
  geöffnete App, mit rotem Stern bei Neuem und dem letzten App-Foto bzw.
  Benachrichtigungsbild. Tippen öffnet die App (und nimmt den Stern weg),
  das ✕ blendet die Kachel aus.
- Diese Karte holt Reihenfolge, Sterne und die echten App-Fotos aus der App
  „Active Frames" (über deren Datenkanal) - EdgeTab selbst braucht dafür
  KEINE zusätzlichen Berechtigungen (kein Nutzungsdaten-/Bedienungshilfe-
  Zugriff).
- Weil EdgeTab die Kacheln in seiner eigenen Leiste selbst zeichnet (kein
  Launcher-Host), scrollt diese Karte immer zuverlässig - anders als ein
  Home-Screen-Widget auf älteren Launchern. Eigene Einstellungen je Karte:
  Scrollen an/aus, Spalten, Kachelhöhe, Anzahl, große obere Reihen und Höhe
  der kleineren Reihen. Hinzufügen unter Einstellungen → Registerkarten.

## 0.76
- Absturzsicherheit: Stürzt EdgeTab ab, startet der Dienst jetzt automatisch
  wieder neu (bisher blieb die Randleiste danach ganz weg, bis die App von
  Hand geöffnet wurde). Möglich, weil EdgeTab die Overlay-Berechtigung hält
  und damit von Androids Hintergrund-Startsperre für Vordergrunddienste
  ausgenommen ist.
- Die Fehlermeldung eines Absturzes wird jetzt in ein Fehlerprotokoll
  gesichert und unter „Über EdgeTab" angezeigt (erscheint nur, wenn es einen
  gab) - vorher lag sie nur flüchtig im Systemprotokoll und war ohne Kabel
  kaum einzusehen. Mit Knopf zum Löschen.
- Behebt einen Absturz beim Neuaufbau des Panels (u. a. nach dem Tippen auf
  eine Verknüpfung): Androids Fokus-Neuvergabe konnte beim Leeren des Panels
  in einen Nullzeiger-Fehler laufen; der Fokus wird jetzt vorher sauber vom
  Panel gelöst.

## 0.75
- Sicherung von "Über EdgeTab" nach "Dienst" verschoben (dort wird zuerst
  danach gesucht).
- Sichern/Wiederherstellen schlägt jetzt den Ordner "DaSis" als Startort
  vor, falls vorhanden.

## 0.74
- Neu: Einstellungen sichern/wiederherstellen (Einstellungen → Über EdgeTab
  → Sicherung) - sichert/liest den kompletten Stand (alle Einstellungen und
  Registerkarten inkl. Reihenfolge/Namen/Icon-Pfaden) als einfache
  Textdatei. Icon-Bilddateien selbst wandern nicht mit, nur ihre Pfade.

## 0.73
- Eingebettete Sammel-Widgets (z.B. BlackBerry Hubs Posteingang-Liste)
  erzeugten bei jedem Karten-Neuaufbau (Tab-Wechsel, Bildschirmdrehung) eine
  komplett neue Widget-Ansicht - das löste jedes Mal eine frische Verbindung
  zum `RemoteViewsService` der Fremd-App aus, inklusive vollständiger
  Neuübertragung der ganzen Liste samt Bildern über Binder. Je nach Widget
  und Listengröße konnte das dessen App zum Absturz bringen (dort
  beobachtet: "Could not write bitmap blob file descriptor" beim Verpacken
  der Bilder). Die Widget-Ansicht wird jetzt wiederverwendet und nur bei
  Bedarf in die neue Karte umgehängt, statt bei jedem Neuaufbau neu erzeugt
  zu werden - genau wie ein normaler Homescreen-Launcher es handhabt.

## 0.72a
- Nur Änderungsprotokoll-Text bereinigt (keine Personenerwähnung mehr bei
  gemeldeten Fehlern/Wünschen), keine funktionale Änderung.

## 0.72
- Neue Seite „Über EdgeTab" in den Einstellungen: Versionsnummer, Lizenztext
  und ein aufklappbares Änderungsprotokoll (dieses Dokument, direkt in der
  App).
- Quelloffene Veröffentlichung auf GitHub vorbereitet, Lizenz: GNU General
  Public License v3.

## 0.71
- Icon-Spalte ist jetzt eine eigene Scroll-Zone (Zahnrad bleibt oben fix,
  alle Karten darunter erreichbar, auch bei wenig Höhe/Querformat).
- Bildschirmdrehung bei geöffneter Leiste baut das Panel komplett neu auf
  (behebt Render-Glitches bei eingebetteten Widgets nach dem Drehen).

## 0.70
- Englische Übersetzung: 181 Textstellen extrahiert, Deutsch bleibt
  Standard, Englisch kommt automatisch je nach Gerätesprache.
- Kategorien im Posteingang jetzt sprachunabhängig gespeichert (nicht mehr
  als deutsches Wort) - alte gespeicherte Kategorien werden migriert.
- Uhrzeit/Datum in der Kopfzeile folgen jetzt der Gerätesprache statt fest
  Deutsch zu sein.

## 0.63
- Posteingang-Quellen und Sucher-Benachrichtigungsquellen zeigen sofort alle
  installierten Apps (nicht mehr nur die, die schon mal benachrichtigt
  haben) - kein tagelanges Warten mehr auf seltene Kanäle.

## 0.62
- Menüänderungen (Häkchen, Kategorie wählen, Tab wechseln) bauen nur noch
  den Karteninhalt neu, ohne die Leiste zu- und wiederaufzufahren. Nur ein
  echter Seitenwechsel (links/rechts) löst noch den vollen Neuaufbau aus.

## 0.61
- Die "Suche"-Karte (Datei-Volltextsuche) wurde wieder aus EdgeTab entfernt
  und in die eigenständige App **Sucher** ausgelagert (siehe deren eigenes
  Changelog) - anderes Berechtigungsprofil, anderer Anwendungsfall.

## 0.60
- Neue "Suche"-Karte: Volltextsuche über Dateien (Text/Office/PDF/E-Books/
  Comic-Metadaten), Kontakte und Termine. Erste Version mit vendortem
  Apache POI + PDFBox-Android - erste echten Bibliotheks-Abhängigkeiten im
  Projekt. (Kurzlebig - siehe 0.61, wurde als eigene App ausgegliedert.)

## 0.52
- Verknüpfungen: Einträge auch INNERHALB einer Gruppe per ▲▼ sortierbar.

## 0.51
- Kontakte: Anzeige (Vor-/Nachname zuerst) und Sortierung unabhängig
  wählbar.
- Verknüpfungen: Gruppen jetzt gerahmt und per ▲▼ verschiebbar; beim
  "+ App hinzufügen" lassen sich mehrere Apps auf einmal auswählen und
  übernehmen statt einzeln.

## 0.50
- Einstellungen-Ansicht ist jetzt blickdicht (nicht mehr von der
  Leisten-Transparenz betroffen).
- Posteingang-Quellen alphabetisch sortiert; Kategorie-Schaltfläche bleibt
  bei langen App-Namen sichtbar (Checkbox-Text bricht mit "…" ab).

## 0.49
- Dienst startet nach jedem App-Update automatisch neu
  (`MY_PACKAGE_REPLACED`) - kein manuelles "Erzwungen stoppen" mehr nötig.

## 0.48
- Verknüpfungen-Karte: App-Auswahlliste ist jetzt blickdicht (unabhängig
  von der Leisten-Transparenz).
- Notizen-Karte: neuer Stift-Button legt eine neue Notiz direkt in
  ImapNotes3 an (Weiterreichen per `ACTION_SEND`, kein eigener
  IMAP-Client).

## 0.47
- Posteingang: Kategorie-Zuordnung je Quell-App (färbt die Karte ein,
  erlaubt Schnellfilter) und Kanal-Feinauswahl je App (z. B. bei eBay
  "Neue Artikel" abwählen, "Nachrichten" behalten).

## 0.46
- **Signierschlüssel gewechselt**: alter Schlüssel (`keystore.p12`, Alias
  `bbresign`) trug als Zertifikats-Eigentümer `CN=BlackBerry Ltd` (Rest
  vom frühen `bbresign`-Umsignier-Versuch) - unbrauchbar sobald APKs
  geteilt/veröffentlicht werden. Neuer, sauberer Schlüssel
  (`edgetab-keystore.p12`, `CN=Mathias Herbers`) ab hier verbindlich;
  einmalige Deinstallation+Neuinstallation nötig (Signaturwechsel),
  bewusst in Kauf genommen.
- GitHub-Vorbereitung: lokales Git-Repo, `.gitignore`, `README.md` (EN),
  `LICENSE` (MIT) - noch kein Push, wartet auf grünes Licht.

## 0.45
- Posteingang nach Tagen gruppiert (HEUTE/GESTERN/VORGESTERN/…), wie der
  Kalender, nur rückwärts (Vergangenheit statt Zukunft).
- Widget-Karte: Text-Knopf "Widget auswählen" durch schwebendes graues
  Zahnrad-Symbol ersetzt.

## 0.44
- Posteingang-Zeilen bekommen einen farbigen Balken links (Farbe stabil
  aus dem Paketnamen erzeugt, HSV-Hash) plus einen orangen schwebenden
  Stift, der `ACTION_SENDTO`/`mailto:` startet (einzige wirklich
  universelle "neue Nachricht"-Systemabsicht).
- Aufgaben-Karte: grüner schwebender Stift öffnet JTX Board selbst (keine
  System-weite "neue Aufgabe"-Absicht verfügbar).

## 0.43
- Schwebende, farbige Rundsymbole (FAB) statt Text-Buttons für "+ Termin
  hinzufügen" (grün) und "+ Kontakt hinzufügen" (blau, jetzt immer
  sichtbar statt im Zahnrad-Menü versteckt) - neue `BaseTab.fab()`/
  `withFab()`-Helfer.
- BBM Enterprise als Chat-Datenquelle geprüft und verworfen: eigener
  ContentProvider verlangt zwei Rechte, die nirgends im Manifest
  deklariert sind - kann keine App je erhalten (versteckte Positivliste
  vermutet), gleiche Sackgasse wie beim Hub.

## 0.38–0.42
- **Zahnrad-Richtung korrigiert (0.38)**: das Haupt-Zahnrad oben wird zum
  simplen "⚙"-Zeichen (0.37 hatte es versehentlich andersherum gebaut).
- **Termin/Kontakt hinzufügen über System-Apps (0.38)**: eigenes
  Termin-Formular aus 0.37 komplett zurückgebaut, ersetzt durch
  `Intent.ACTION_INSERT` auf die installierte Kalender-/Kontakte-App
  ("mehrere, verschiedene zu bedienende sind doof").
- **Mediensteuerung, zwei Ursachen behoben**: (0.40) dieselbe Sitzung
  erschien doppelt (kompakte Leiste + separate Info-Karte in der Liste) -
  gefixt, nur noch eine feste Vollkarte. (0.41) der eigentliche Grund für
  "Position ändert sich nicht": `EdgeService.renderContent()` gab dem
  Karteninhalt keine feste Höhe, ein `FrameLayout` mit `Gravity`-
  Positionierung hatte dadurch nie echte Restfläche zum Bewegen.
- **Echtes Antworten im Posteingang (0.39/0.42)**: erst RemoteInput-
  Direkteingabe (wie Telegram/Signal/Molly), dann nach Live-Test mit
  einer echten Hub-Mail auch reine Aktions-PendingIntents ohne
  RemoteInput (Hubs "Allen antworten" öffnet direkt dessen eigenen
  Antwortbildschirm).

## 0.37
- Zahnrad-Icons von ShortcutsTab/ContactsTab auf das echte
  `ic_settings`-Vektorsymbol vereinheitlicht (später in 0.38 als falsch
  herum erkannt und korrigiert).
- Registerkarten-Spalte umsortiert: gewichteter Spacer drückt alle
  regulären Karten an den unteren Rand, nur das Zahnrad bleibt oben.
- Eigenes "Termin hinzufügen"-Formular gebaut (Feldset per `apktool` von
  BBs eigener Kalender-App abgeleitet, schlanker: ohne Wiederholung/
  Teilnehmer/Kategorien) - in 0.38 nach Feedback wieder verworfen.

## 0.33–0.36
- **Aufgaben abhakbar (0.33)**: neue `WRITE`-Berechtigung für JTX Board;
  wichtiger Fund: JTX Boards `update()` setzt `dirty`/`lastmodified`
  NICHT automatisch - EdgeTab muss `dirty=1` selbst setzen, sonst wird
  die Erledigung nie zum Server hochgeladen. Wiederkehrende Aufgaben
  rücken beim Abhaken über EdgeTab nicht automatisch vor (nur JTX Boards
  eigene App tut das).
- **Mediensteuerung vergrößert (0.35)**: Cover 48→72dp, Schrift/Knöpfe
  deutlich größer.
- **Feste, positionierbare Schnellleiste (0.36)**: `Settings.
  mediaControlsPos()` (oben/mittig/unten) - Bedienelemente sollen
  einhändig mit dem Daumen der haltenden Hand erreichbar sein.

## 0.29–0.32 — Aufgaben- und Notizen-Karte
- **0.29**: TasksTab fertig, liest JTX Boards offenen, nicht-
  signaturgeschützten Content-Provider (`at.techbee.jtx.provider`, kein
  eigener CalDAV-Client nötig). Bug: Provider ist strikt kontobezogen,
  fehlende `account_name`/`account_type`-Parameter führen zu NPE.
- **0.30**: Fix per `AccountManager.newChooseAccountIntent()`; NotesTab
  fertig (gleicher Provider, `module = "NOTE"/"JOURNAL"`). Praxistest:
  Kontowähler zeigte kein Konto (Android-8-Sichtbarkeitsregeln - DAVx5
  gibt fremden Apps standardmäßig keine Kontosicht frei).
- **0.31**: eigener Einrichtungsbildschirm mit manuellem Eingabefeld für
  den Kontonamen (System-Kontowähler bleibt als Alternative daneben),
  jederzeit wiederholbar über Einstellungen.
- **0.32**: zweiter Bug gefunden (`caller_is_syncadapter=true` fehlte als
  Pflichtparameter, reine Parameter-Anwesenheit ohne echte Prüfung) -
  behoben, Aufgaben funktionieren seitdem zuverlässig.

## 0.28 — Mediensteuerung-Karte
Neu: `MediaTab`, zeigt laufende Wiedergaben (Titel/Interpret/Cover) mit
Steuerung über `MediaSessionManager` - keine neue Berechtigung nötig, ein
aktiver `NotificationListenerService` (den EdgeTab für den Posteingang
ohnehin ist) darf das ohne die sonst signaturgeschützte
`MEDIA_CONTENT_CONTROL`-Berechtigung.

## 0.25–0.27 — Kontakte-Karte
- **0.25**: ContactsTab fertig (Alle/Ausgewählt/Favoriten über
  `ContactsContract`), `READ_CONTACTS` erst bei Bedarf abgefragt.
- **0.26**: Bug behoben - "Kontakte auswählen…"-Knopf war fälschlich an
  das Menü-offen-Flag gekoppelt und verschwand im selben Moment, in dem
  er gebraucht wurde.
- **0.27**: Checkboxen im Auswahldialog waren ohne eigenes Tinting kaum
  sichtbar (kein App-Theme im Overlay-Fenster) - App-Blau als
  `ButtonTintList` ergänzt.

## 0.23–0.24
- **0.23**: Einstellungen auf zweistufiges Kategorien-Menü umgebaut (statt
  einer langen Liste); Verknüpfungen-Icongröße reagiert jetzt korrekt auf
  die Spaltenzahl; Rand-Wischgeste bekommt eine `setSystemGestureExclusionRects()`-
  Ausschlusszone (Androids eigene Zurück/Vorwärts-Randgesten fingen die
  Berührung sonst vorher ab).
- **0.24**: Wisch-Aufziehen als funktionierend bestätigt (war teils
  Bedienfehler - Wischrichtung vertikal statt horizontal versucht). Griff-
  Design 1:1 nach BB-Original nachgebaut (per `apktool` dekompiliert:
  dreiteilige, spitz zulaufende Form mit Schattensaum und Punkt-Andeutung).
  Größere ✕-Trefferflächen. Icongröße der Registerkarten-Spalte
  einstellbar (18–44dp). Verknüpfungen-Karte: nur noch ein Zahnrad statt
  zweier Knöpfe.

## 0.22 — Hub-Löschen endlich gelöst
Jahrelang offenes Problem. Ursache per `adb logcat`-Vergleich (Löschen
über System-Benachrichtigung vs. über EdgeTab) gefunden:
`Launcher.fireAfterForeground` holte den Hub vor dem Senden des
Löschen-PendingIntent erst in den Vordergrund - das brachte Hub dazu, die
Benachrichtigung binnen Millisekunden mehrfach neu zu posten, wodurch der
vorher gemerkte PendingIntent ungültig wurde. Fix: PendingIntent wird
sofort gesendet, kein Vorab-Foregrounding mehr.

## 0.21 — Verknüpfungen-Karte
Neuer Kartentyp `ShortcutsTab`: reine App-Icon-Verknüpfungen, gruppierbar
(jede Gruppe mit eigener Ansicht: Raster/Liste + Spaltenzahl). "Echte"
tiefe App-Verknüpfungen (z. B. direkt zu einem Chat) sind für
Drittanbieter-Apps auf modernem Android nicht zugänglich - bewusst auf
reine App-Icons beschränkt.

## 0.17–0.20 — Registerkarten-Umbau
- **0.17**: Registerkarten sind jetzt Instanzen (`TabInstance`/`BaseTab`)
  statt einer festen Menge - beliebig viele Widget-Karten, mehrere
  Widgets je Karte gestapelt, umbenennbar, eigenes Icon (Galerie-Foto
  oder mitgeliefertes Symbol), "nach Tippen schließen" je Karte,
  Reihenfolge per ▲▼, aktiver Tab optisch hinterlegt.
- **0.18**: Sortieren sprang nach dem Neuaufbau immer an den Seitenanfang
  zurück - behoben durch eine gemerkte Scroll-Position. Dunkles Theme
  für MainActivitys Vollbildseiten (`EdgeTabTheme`) statt des weißen
  Standard-Themes. Icon-Satz auf Nachrichten/Produktivität zugeschnitten
  (Koffer/Fahne/Globus u. Ä. entfernt, 15 passende Symbole dazu).
- **0.19**: mitgelieferter Icon-Satz (`IconSet`) als eigene Auswahl neben
  der freien Bildauswahl.
- **0.20**: Icon-Namen im Auswahlraster liefen bei langen Wörtern aus dem
  Bildschirm - feste Zellbreite, zweizeilige Beschriftung.

## 0.16 — Widget-Picker-Bug
Das Hub-Posteingang-Widget fehlte komplett in EdgeTabs eigener
Widget-Auswahlliste, obwohl der native Launcher-Picker es zeigte.
Ursache: fehlender `<queries>`-Block im Manifest - seit Android 11
filtert `AppWidgetManager.getInstalledProviders()` alle nicht per
`<queries>` sichtbar gemachten Pakete heraus. Behoben.

## 0.15 und davor
Grundgerüst: Rand-Griff (Antippen/Wischen zur Mitte öffnet, Zuwischen
schließt), Panel mit Icon-Spalte, sechs ursprüngliche Karten (Kalender,
Posteingang, Aufgaben, Notizen, Kontakte, Einstellungen - anfangs
teils nur Platzhalter), Kopfzeile mit Uhr/Datum/Akku, erste Version der
Widget-Karte (0.15, embettet ein frei wählbares App-Widget über einen
eigenen `AppWidgetHost`).
