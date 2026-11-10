# CLimit – Stand und offene Punkte

Stand 10.11.2026. Die Ideenliste vom selben Tag ist umgesetzt; hier steht, was davon wie gelöst ist und was noch offen oder ungetestet ist.

## Umgesetzt

**Deine Punkte**
- Live-Vorschau beim Schieben: Rendern wird gebündelt statt abgebrochen; die Vorschau zeigt das echte Widget in seiner echten Größe.
- Hintergrund hinter der Vorschau: vier Beispielhintergründe plus eigenes Bild über die Fotoauswahl (ohne Berechtigung).
- Blur: Samsung One UI Home (ab One UI 7) blurrt das Hintergrundbild hinter einer halbtransparenten Karte. Umsetzung über `android:id/background` (Glance `appWidgetBackground`) und Samsungs Provider-Attribute `widgetStyle="colorful"`. Andere Launcher ignorieren das; die Karte bleibt halbtransparent, die Einstellungen erkennen One UI Home und zeigen einen passenden Hinweis. Mit Blur ist die Karte immer ein abgerundetes Rechteck.
- Tipp-Aktion: Aktualisieren, CLimit öffnen, Claude öffnen (App oder claude.ai), Einstellungen dieses Widgets, nichts.
- Zusammengesetzte Widget-Form: abgerundetes Rechteck oder elf Material-Formen, auf das Widget gestreckt.
- Größen: frei ab etwa 1×1; eigener Mini-Modus für eine Zelle; zweiter Eintrag „Kompakt" (2×2, Cookie-Form) in der Widget-Auswahl.
- Texte: eigene Beschriftung für 5-Stunden- und Wochenlimit, getrennte Größen, Reset als Uhrzeit oder Countdown, Prozentzeichen, verbraucht/verbleibend.

**Material 3 Expressive**
- Verbundene Knopfgruppen, Stil-Kacheln mit gezeichneten Miniaturen, Symbole, morphende Formen bei der Auswahl, schwebende Werkzeugleiste mit Speichern-FAB.
- Roboto Flex (gebündelt, OFL) für große Zahlen in App und Widget, mit Stärke und Breite.
- Bewegung: Anzeigen laufen beim Öffnen ein, Reiter blenden über, Formen morphen.
- Blur hinter der Titelleiste der App (Haze).
- Tablet-Layout, vorausschauende Zurück-Geste, Themed Icon, neues App-Icon, App-Sprache pro App.

**Neue Stile** (jetzt zwölf): verschachtelte Ringe mit Legende, Halbkreis-Tacho, Segmente, Punkte aus Material-Formen, Füllstand-Kacheln, Kombi. Zeitfortschritt als Option für Ringe, Tacho, Balken und Segmente.

**Weitere Optionen**: zwölf Vorlagen, eigene Vorlagen speichern/löschen, Einstellungen von anderem Widget kopieren, Export/Import als Text, freier Farbton, einstellbare Schwellen, Stand-Hinweis und „zuletzt aktualisiert", Innenabstand und Abstände, Reihenfolge und Anordnung, offener Ring, Startpunkt, Drehrichtung, Kontur.

**Technik**: Einstellungsmodell v2 mit Migration alter Widgets, 16 Unit-Tests, Release-Build mit R8 geprüft, F-Droid-Metadaten (en/de) mit Screenshots, README, Demo-Modus nur in Debug-Builds.

## Offen oder ungetestet

- **Samsung-Blur auf echtem Gerät**: nur auf dem Pixel-Emulator geprüft (dort fehlerfrei halbtransparent). Ob One UI Home wirklich blurrt, zeigt erst ein Samsung-Gerät.
- **Echter Abruf nach dem Umbau**: Der Emulator hat seine Anmeldung verloren; alles ab dem Umbau lief mit Demo-Daten. Der Abrufweg selbst ist unverändert, sollte aber einmal mit echtem Konto bestätigt werden.
- **Abmelden** gegen den Server weiterhin nicht getestet.
- **Stand-Hinweis** (Daten älter als eine Stunde) und Tipp-Aktion „Claude öffnen" nicht im Emulator gesehen.
- **Speicher**: Ein 4×2-Widget mit Form und Kontur belegt etwa 5,4 MB Bilddaten (Grenze bei diesem Bildschirm etwa 15 MB). Lässt sich weiter senken, z. B. Karte ohne Bitmap, wenn keine Form gewählt ist (schon so) oder kleinere Auflösung für die Karte.
- **Countdown** kann bis zu 15 Minuten nachgehen (Aktualisierungsintervall); steht als Hinweis in den Einstellungen.
- **Material-Bibliothek** läuft auf 1.5.0-rc01, weil die Expressive-Bausteine in 1.4 fehlen; auf die stabile Version heben, sobald sie erscheint.
- **Verlauf und Prognose**: bewusst zurückgestellt.
- **F-Droid-Einreichung**: Repo veröffentlichen, Release taggen, Merge Request bei fdroiddata (Anti-Feature NonFreeNet).
