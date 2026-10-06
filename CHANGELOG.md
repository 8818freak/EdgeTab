# Changelog — EdgeTab

Alle nennenswerten Änderungen, neueste zuerst. Vor 0.14 nicht im Detail
dokumentiert (allerfrüheste Aufbauphase: Grundgerüst, Rand-Griff, erste
Karten als Platzhalter).

## 1.6
- Neu: Der Kalender kann jetzt auch Termine der letzten Tage anzeigen (mit
  GESTERN/VORGESTERN). Standardmäßig ausgeblendet; ein Dreieck oben
  („Vergangene Tage einblenden“) blendet sie bei Bedarf ein und wieder aus.

## 1.5.1
- Verbessert: Die Benachrichtigung bei einer fehlenden Berechtigung ist jetzt
  zweisprachig (Deutsch/Englisch, folgt der Systemsprache).

## 1.5
- Verbessert: Die beiden schwebenden Knöpfe (Haupt-Aktion + „nach oben“) sitzen
  jetzt gleichmäßig links und rechts der Mitte; der Abstand richtet sich nach der
  eingestellten Leistenbreite. Gibt es nur einen Knopf, sitzt er mittig. Der
  „nach oben“-Knopf erscheint jetzt zuverlässig in allen Listen (auch
  Verknüpfungen).
- Verbessert: Das Umschalten zwischen den Karten ist spürbar schneller –
  Kontaktnamen werden zwischengespeichert (Posteingang, SMS und Anrufe schlugen
  sie vorher je Zeile einzeln nach, was mehrere Sekunden dauern konnte).
- Handbücher: aktuelle Bildschirmfotos, persönliche Inhalte pro Wort geschwärzt.

## 1.4
- Neu: „Nach oben“-Knopf in allen Listen-Karten (Posteingang, Anrufe, SMS,
  Kontakte, Termine, Aufgaben, Notizen, Verknüpfungen). Er erscheint nur, wenn
  wirklich gescrollt wurde, sitzt auf der Seite, an der EdgeTab am Bildschirmrand
  klebt, und rückt den Haupt-Knopf dann zur Gegenseite (gleichmäßig verteilt).
- Verbessert: Der Direktanruf beim kurzen Tipp klappt jetzt zuverlässig aus der
  Leiste heraus (Hintergrund-Start ausdrücklich erlaubt); fehlt das Recht
  „Anrufen“, fordert EdgeTab es an, statt still in der Wähl-App zu landen.
- Verbessert: Das Umschalten zwischen den Karten ist flüssiger – der Posteingang
  fragt die aktiven Benachrichtigungen nur noch einmal pro Aufbau ab (vorher je
  Zeile), und eigene Karten-Symbole werden zwischengespeichert statt bei jedem
  Wechsel neu von der Platte gelesen.
- Neu: Hinweis bei den Berechtigungen und in der Anleitung – ändert sich eine
  Berechtigung (z. B. durch ein Update), muss man sie einmal entziehen und neu
  erteilen, sonst greift sie nicht.

## 1.3
- Neu: In der Anrufliste und im Posteingang zeigt eine Anruf-Zeile jetzt die
  Rufnummern-Kennung aus dem Telefonbuch (Mobil/Arbeit/Privat …) und bei
  bekannten Kontakten zusätzlich die Nummer. Kurzer Tipp ruft direkt an
  (braucht das neue Recht „Anrufen"; sonst öffnet sich die Wähl-App), langer
  Tipp öffnet ein Menü (Wählen, SMS, im Telefonbuch öffnen, Nummer kopieren,
  aus der Anrufliste löschen).
- Neu: Langer Druck auf ein Textfeld fügt den Text aus der Zwischenablage ein –
  auch dort, wo die schwebende System-Auswahlleiste im Overlay nicht erscheint.
- Neu: Je Quelle einstellbar, bereits aus der Statusleiste entfernte
  Benachrichtigungen nicht erneut (in der Quell-App) zu löschen – schützt vor
  komischem Verhalten/Abstürzen durch veraltete Lösch-Aktionen (für den
  BlackBerry Hub ist das voreingestellt an).
- Neu: Zweiter schwebender Knopf unten links im Posteingang springt an den
  Listenanfang.
- Verbessert: Die Scroll-Position im Posteingang bleibt erhalten, auch wenn die
  Leiste zugeht oder man zwischendurch eine andere App benutzt.
- Verbessert: Eingebettete fremde Widgets (z. B. Hub-Posteingang) werden nur
  noch bedient, solange ihre Karte wirklich offen ist – ein deaktivierter Tab
  oder eine geschlossene Leiste stößt die fremde App nicht mehr an (die sonst
  abstürzen konnte). „Entfernen" hebt die Widget-Bindung jetzt vollständig auf.

## 1.2
- Behoben: Im Posteingang wurde bei älteren Einträgen der App-Name senkrecht
  (Buchstabe für Buchstabe) umgebrochen – der lange Datum-/Uhrzeit-Stempel hatte
  die Namensspalte auf null Breite gequetscht. Der App-Name ist jetzt einzeilig
  (bei Bedarf gekürzt), der Zeitstempel steht in einer eigenen Zeile darunter.

## 1.1
- Neu: **Suche in der SMS- und in der Anrufe-Karte.** Die Lupe öffnet ein Feld:
  „Hier suchen" filtert die Liste nach Name/Nummer/Text, „In Sucher suchen"
  öffnet Sucher für die Volltextsuche über alles. Besonders nützlich, seit die
  Listen (0 = alle) lang werden können.

## 1.0
- Verbessert: Bei „Einträge zeigen der letzten … Tage" bedeutet jetzt **0 = alle
  (unbegrenzt)** – wie bei der Aufbewahrung. Neuer **Standard ist 0 (alle)**,
  damit lange Konversationen (z. B. viele SMS eines Kontakts) von vornherein
  vollständig erscheinen. Ein leeres Feld übernahm zuvor unbemerkt den alten
  Wert – für „alles" bitte 0 eintragen.

## 0.99
- Verbessert: Die Relativ-Zeit-Schwelle ist jetzt in **Minuten, Stunden oder
  Tagen** einstellbar (Wert + Einheit-Umschalter), nicht mehr nur in Tagen – so
  lässt sich z. B. „relativ bis 2 Stunden, danach Datum + Uhrzeit" einstellen.
  Standard ist jetzt **2 Stunden**.

## 0.98
- Neu: **Datum + Uhrzeit** in Posteingang, Anrufen und SMS. Junge Einträge
  zeigen weiterhin die relative Angabe („vor 3 Min"), ältere das Datum
  („09:15 Uhr, 28.09.2026").
- Neu: In den Einstellungen (Posteingang → „Zeitanzeige & Umfang") einstellbar:
  ob relative Zeit überhaupt gezeigt wird und **bis zu welchem Alter in Tagen**
  (1–999) – darüber Datum + Uhrzeit.
- Neu: **Die Listenlänge ist nicht mehr fest begrenzt**, sondern über das Alter
  in Tagen einstellbar (1–999) – gilt für Posteingang, SMS und Anrufe.

## 0.97
- Verbessert: **Die Sicherung nimmt jetzt auch die Daten mit**, nicht nur die
  Einstellungen – den kompletten Posteingang-Verlauf (erfasste
  Benachrichtigungen samt Gelesen-Status, gesendeter Antworten und des
  vollständigen Abzugs jeder Benachrichtigung). Gesichert wird als eine
  Zip-Datei (`edgetab-sicherung.zip`); „Wiederherstellen" spielt Einstellungen
  **und** Daten zurück. Ältere reine Text-Sicherungen (nur Einstellungen)
  werden beim Wiederherstellen weiterhin erkannt und eingelesen.

## 0.96
- Neu: **Antworten, „als gelesen" und Löschen bleiben verfügbar, auch wenn die
  Benachrichtigung schon aus der Statusleiste verschwunden ist** – EdgeTab hält
  die „Drähte" (Aktions-Verknüpfungen) jeder Benachrichtigung fest, solange es
  läuft. So kann man z. B. eine bereits weggewischte Mail noch löschen oder in
  einer Konversation noch antworten. (Grenzen: überlebt keinen Geräte-Neustart
  und kein Update/Beenden der Quell-App – dann ist der Draht ungültig; EdgeTab
  versucht dann automatisch, den Antwortbildschirm der App zu öffnen.)
- Verbessert: Der Schutz der gespeicherten Nachricht ist jetzt treffsicherer –
  ein bloßer Bestätigungs-/Leer-Neupost („Geantwortet.") ersetzt eine echte
  Nachricht nicht mehr, **echte neue Nachrichten derselben Konversation kommen
  aber weiter durch** (löst die gröbere Antwort-Sperre aus 0.95 ab, die neue
  Nachrichten hätte verschlucken können).
- Intern: Inhaltsschutz und Draht-Ablage sitzen in der gemeinsamen Bibliothek
  (`Notifications.isConversational`/`looksLikeReplyConfirmation`,
  `NotificationActionCache`) – für alle vier Apps nutzbar.

## 0.95
- Neu: Jede erfasste Benachrichtigung wird jetzt **verlustfrei** gespeichert –
  alle Felder, die sie mitbrachte (über die Bibliothek `Notifications.toJson`),
  nicht mehr nur Titel und eine Textzeile. So geht nichts Auslesbares verloren
  und alles bleibt später auswertbar.
- Neu: Eine im Posteingang gesendete Antwort bleibt unter der Nachricht stehen
  (baut einen kleinen Verlauf auf). Die Originalnachricht wird dabei nicht mehr
  vom bloßen „Geantwortet."-Neupost mancher Apps (BlackBerry Hub/BBMe)
  überschrieben.

## 0.94
- Neu: Der Berechtigungs-Abschnitt in den Einstellungen zeigt jetzt auch
  **SMS senden** und **Anrufliste ändern** als eigene, erklärte Einträge (zuvor
  nur SMS/Anrufliste lesen). Die Erinnerung bei Verlust eines einmal erteilten
  Rechts greift damit auch für diese beiden.

## 0.93
- Behoben: Beim Auf-/Zuklappen und Antworten sprang die Liste an den Anfang –
  die Scroll-Position bleibt jetzt erhalten (Posteingang und SMS-Karte).
- Verbessert: Die Aufklapp-Schaltflächen der Konversationen sind jetzt große,
  leicht treffbare Flächen (volle Breite) statt winziger Pfeile.
- Neu: Die SMS-Karte gruppiert jetzt ebenfalls zu aufklappbaren Konversationen
  und hat eine Direktantwort (Schreibfeld, braucht SMS-Senden-Recht).
- Verbessert: Anruf-Farben – grün = erfolgreich, rot = verpasst, blau =
  vergeblich (abgehend, niemanden erreicht).
- Verbessert: SMS werden in voller Länge angezeigt; Benachrichtigungen zeigen
  jetzt den reicheren Inhalt (Chat-Zeilen, Mehrzeiler, Zusatzzeile), soweit die
  Benachrichtigung noch aktiv ist.
- Behoben: In Eingabefeldern war die Schrift auf dunklem Grund fast unsichtbar
  (schwarz); jetzt hell. Häkchen-Kästchen sind jetzt sichtbar (blauer Rahmen).

## 0.92
- Neu: SMS und Anrufe können jetzt auch direkt im Posteingang erscheinen
  (zwei Schalter in den Posteingang-Einstellungen) – zusätzlich zu den eigenen
  SMS-/Anrufe-Karten.
- Neu: Nachrichten werden zu aufklappbaren Konversationen gruppiert – SMS je
  Kontakt, Benachrichtigungen je Absender/Betreff. Der neueste Eintrag steht
  oben, „N weitere in dieser Konversation" klappt den Verlauf auf. Abschaltbar.
- Neu: Direkt auf eine empfangene SMS antworten (↩ im Posteingang, braucht das
  SMS-Senden-Recht) und verpasste Anrufe als gesehen markieren (✓, braucht das
  Anrufliste-Schreiben-Recht).
- Verbessert: Erfolglose ausgehende Anrufe (niemanden erreicht) werden als
  „ausgehend · nicht erreicht" gekennzeichnet (Posteingang und Anrufe-Karte).
- SMS löschen ist nicht möglich: Android erlaubt das nur der Standard-SMS-App.

## 0.91
- Neu: SMS- und Anrufe-Karten. Über die Einstellungen („+ Neue SMS-/Anrufe-
  Registerkarte“) hinzufügbar – zeigen die letzten Kurznachrichten bzw. Anrufe
  mit Name/Nummer, Richtung und Zeit; Tippen öffnet die SMS-App bzw. Wähl-App.
  Braucht die Rechte SMS bzw. Anrufliste (erst bei Bedarf).
- Neu: Suche im Posteingang. Über die Lupe öffnet sich ein Suchfeld: „Hier
  suchen“ filtert den EdgeTab-Posteingang, „In Sucher suchen“ öffnet (falls
  installiert) Sucher mit dem Begriff für die Volltextsuche über alles.

## 0.90
- Neu: In der Berechtigungen-Karte lässt sich der Posteingang gezielt leeren
  („Erfasste Benachrichtigungen löschen“, mit Bestätigung durch nochmaliges
  Tippen). Die erfassten Benachrichtigungen bleiben sonst auch nach Entzug des
  Zugriffs erhalten – so kann man sie bewusst entfernen.

## 0.89
- Verbessert: Die Berechtigungen sind jetzt eine eigene Karte in den
  Einstellungen (vorletzter Punkt) statt oben eingeklappt – übersichtlicher,
  wo welche Einstellung zu finden ist.
- Neu: Im Posteingang steht vor jedem Eintrag das Benachrichtigungs-Icon
  (Statusleisten-Symbol der App, in der Akzentfarbe). Emoji im Text werden
  ohnehin normal dargestellt.

## 0.88
- Neu: In der Posteingang-Karte werden große Bilder aus Benachrichtigungen
  (BigPictureStyle, z. B. ein Foto in einer Chat-Nachricht) angezeigt, solange
  die Benachrichtigung noch aktiv ist.
- Neu: Erinnerung, wenn eine einmal erteilte Berechtigung fehlt (z. B. nach
  einem System-Update) – Overlay, Benachrichtigungszugriff, Kalender, Kontakte.
  Die Meldung führt direkt zum Erteilen und lässt sich „Ignorieren“.
- Neu: Aufklappbarer Abschnitt „Berechtigungen“ in den Einstellungen (Dreieck
  ▸/▾) – zeigt je Berechtigung Status und wofür sie gebraucht wird; ein Tipp
  führt in die passende Systemeinstellung.

## 0.86
- Verbessert: Das Diagnose-/Fehlerprotokoll steht jetzt fest ganz unten in den
  Einstellungen, zeigt die neuesten Einträge zuerst, und alle Schaltflächen
  (Anzeigen-Schalter, „Protokoll löschen“) stehen darüber. Neuer Schalter
  „Protokoll anzeigen“ blendet es bei Bedarf aus. (Einheitlich in allen Apps.)

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
