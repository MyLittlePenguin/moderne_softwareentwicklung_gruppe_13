# Übung 4.4: LLM-Prompt und Antwort (Test-Erweiterung und Refactoring)

**LLM:** Claude (Opus 5.5), Projekt-Thread, 17.11.2026. Grundlage: Branch `abgabe_04_refactoring`
(= Stand PR #16, `8236217`, 173 Tests grün).

## Prompt

> Hier ist unsere Domänenlogik für „Abstimmung erstellt“ (Context `abstimmungsverwaltung`, Java 27,
> JUnit 5, DDD, Domäne frameworkfrei): `Abstimmung`, `AbstimmungId`, `NutzerId`, `Titel`, `Title`,
> `AbstimmOption`, `Abstimmungszeitraum`, `AbstimmungEingabeValidator` sowie die Testklassen dazu.
> 1. Welche Edge-Cases und Fehlerbedingungen decken unsere Tests noch nicht ab? Belege jeden Fall mit
>    einem konkreten Eingabewert und dem tatsächlichen Verhalten.
> 2. Welche Code-Smells siehst du? Mach Vorschläge zu Lesbarkeit, Performance und besserer Nutzung von
>    Java-Features (Streams, Lambdas, Optional, Records, Pattern Matching).
> 3. Bewerte jeden Vorschlag selbst: Nutzen, Risiko, Auswirkung auf bestehende Tests.
> Schreib noch keinen Code.

## Antwort des LLM

### Teil 1: Fehlende Edge-Cases und Fehlerbedingungen

Alle Fälle wurden mit dem kompilierten Code ausgeführt (Stand `8236217`).

| Nr. | Fall | Eingabe | Tatsächliches Verhalten | Einschätzung |
|---|---|---|---|---|
| E1 | Doppelte Option in zerlegter Unicode-Form | `"Grün"` (NFC) und `"Grün"` (NFD, z. B. von macOS) | **nicht** als Duplikat erkannt | **Fehler.** Beim Titel normalisiert ihr NFC, bei Optionen nicht. Gleiches Problem wie in 4.2 beim Titel. |
| E2 | Duplikat mit ß/SS | `"Straße"` und `"STRASSE"` | nicht erkannt | Grenzfall. `toLowerCase` faltet ß nicht. Fachlich klären, eher niedrige Priorität. |
| E3 | Beschreibung mit Rand-Leerzeichen | `"  Text  "` | wird ungetrimmt gespeichert | Inkonsistent zum `Titel`, der getrimmt wird. |
| E4 | Zeitpunkt genau am Fristende | `zeitpunkt == ende` | `istLaufend = true`, `istBeendet = false` | Konsistent, aber fachlich offen: Zählt eine Stimme genau um 18:00 bei Frist 18:00? Als Test festhalten. |
| E5 | `null` als Zeitpunkt in `Abstimmung.istLaufend` usw. | `null` | `IllegalArgumentException` | Verhalten ok, aber nur in `AbstimmungszeitraumTest` getestet, nicht am Aggregat. |
| E6 | Geschütztes Leerzeichen am Titelende | `"Titel "` | abgelehnt („ungültige Zeichen“) | Vertretbar, aber bewusst entscheiden und testen (Copy-Paste aus Word erzeugt das). |
| E7 | IDs mit Rand-Leerzeichen | `new AbstimmungId(" x ")` | als `" x "` übernommen | `" x "` und `"x"` wären verschiedene Abstimmungen. Trimmen oder UUID-Format prüfen. |
| E8 | Beginn in der Vergangenheit | Beginn gestern | erlaubt | Offene Frage aus 4.2. Bräuchte eine injizierte `Clock`. Eher Thema für den Application Service. |

### Teil 2: Code-Smells und Refactoring-Vorschläge

| Nr. | Smell | Stelle | Vorschlag | Bewertung durch das LLM |
|---|---|---|---|---|
| R1 | Toter Code / Doppelung der Fachsprache | `Title.java` + Test | Klasse und Test löschen | **Empfohlen.** Kein Aufrufer, widerspricht der Ubiquitous Language. Kein Risiko. |
| R2 | Duplizierte Logik (DRY) | `strip().toLowerCase(Locale.ROOT)` in `erstelle` und `hatOption` | Methode `AbstimmOption.normalisierterText()` (inkl. NFC → behebt E1) | **Empfohlen.** Eine Stelle für die Vergleichsregel. |
| R3 | Unerreichbarer Code | `option.text() == null \|\| isBlank()` in `erstelle` und `hatOption(AbstimmOption)` | entfernen, `AbstimmOption` garantiert das schon | **Empfohlen.** Der Record validiert selbst (Always-Valid). |
| R4 | Lange Methode | `erstelle(...)` ≈ 50 Zeilen | `pruefeOptionen(optionen)` als private Methode extrahieren | **Empfohlen.** Lesbarkeit, Tests unverändert. |
| R5 | Schleife statt Stream | `hatOption(String)` | `optionen.stream().anyMatch(o -> o.normalisierterText().equals(gesucht))` | **Empfohlen.** Deklarativ, kürzer. |
| R6 | Schleife statt Stream | Duplikatprüfung in `erstelle` | Stream mit `Collectors.groupingBy` oder `distinct().count()` | **Nicht empfohlen.** Die `HashSet.add`-Schleife bricht beim ersten Duplikat ab und kann es in der Fehlermeldung nennen. Ein Stream wäre hier unleserlicher. |
| R7 | Allgemeine Exceptions | überall `IllegalArgumentException` | eigene fachliche Exception, z. B. `AbstimmungUngueltigException extends IllegalArgumentException` | **Empfohlen** (Blatt nennt „Exceptions“). Durch das Erben von `IllegalArgumentException` bleiben alle bestehenden `assertThrows` grün. |
| R8 | Doppelte Konstanten | `Titel`: Länge 3/100 als Konstanten **und** im Regex `{3,100}` | Länge zuerst prüfen, Regex ohne Längenangabe (`+`) | Sinnvoll. Eine Quelle der Wahrheit. |
| R9 | Exceptions als Kontrollfluss | `AbstimmungEingabeValidator.istGueltigerTitel` fängt Exception ab | statische Methode `Titel.istGueltig(String)` | Optional. Funktioniert, ist aber ein bekannter Smell. |
| R10 | Wiederholte Null-Prüfung | `Abstimmungszeitraum`: 3× gleiche Prüfung | private Hilfsmethode | Optional, kleiner Gewinn. |
| R11 | Altes `equals`-Muster | `Abstimmung.equals` mit `getClass()` + Cast | Pattern Matching: `o instanceof Abstimmung andere && id.equals(andere.id)` | Empfohlen. Moderner Java-Stil, Klasse ist `final`. |
| R12 | Zwei `erstelle`-Überladungen, `getTitel()` + `getTitelObjekt()` | `Abstimmung` | nur `Titel`-Variante behalten | **Nicht jetzt.** Alle 4.2-Tests rufen mit `String` auf. Großer Testumbau für wenig Nutzen. Eher in Blatt 5. |
| R13 | Optional für Beschreibung | `getBeschreibung()` liefert `""` | `Optional<String>` | **Nicht empfohlen.** Die Fachregel „leer = keine Beschreibung“ ist in 4.2 bewusst so getestet. |
| R14 | Performance | `hatOption` normalisiert bei jedem Aufruf | normalisierte Texte cachen (Set) | **Nicht sinnvoll.** Abstimmungen haben wenige Optionen. Das wäre vorzeitige Optimierung. |

## Kritische Bewertung und Umsetzung

| Vorschlag | Entscheidung | Begründung | Commit |
|---|---|---|---|
| E1 + R2 | übernommen | Echter Fehler, mit Test belegt (rot → grün). Vergleichsregel jetzt nur in `AbstimmOption.normalisierterText()`. | `0d02e37` |
| E2 (ß/SS) | abgelehnt | Seltener Fall. Volle Unicode-Faltung (`Collator`) wäre unverhältnismäßig. | – |
| E3 | übernommen | Beschreibung wird jetzt wie der Titel getrimmt. | `9cdd43e` |
| E4, E5 | **LLM-Fehler** | Beide Fälle waren schon in `AbstimmungTest.FachlichesVerhalten` getestet (`istLaufend(ENDE)`, `istLaufend(null)`). Das LLM hat bestehende Tests übersehen. | – |
| E6 | als Test festgehalten | Ablehnung des geschützten Leerzeichens ist gewollt, jetzt dokumentiert. | `443069b` |
| E7, E8 | vertagt | ID-Format und „Beginn in der Vergangenheit“ gehören zum Application Service (mit `Clock`). | – |
| R1 | übernommen | `Title.java` und Test gelöscht. | `658946d` |
| R3, R4, R5 | übernommen | `pruefeOptionen` extrahiert, unerreichbare Prüfungen entfernt, `hatOption` mit `stream().map().anyMatch()`. | `5516803` |
| R6 | abgelehnt | Schleife bleibt: früher Abbruch und Duplikat in der Fehlermeldung. | – |
| R7 | übernommen | `AbstimmungsregelVerletztException extends IllegalArgumentException`. `null` als Zeitpunkt bleibt bewusst `IllegalArgumentException` (Programmierfehler, keine Fachregel). | `ac0a9e0` |
| R8 | übernommen | Länge nur noch über `MIN_LAENGE`/`MAX_LAENGE`, Regex prüft nur Zeichen. | `5428c02` |
| R9, R10 | abgelehnt | Geringer Nutzen, funktionierender Code. | – |
| R11 | übernommen | `o instanceof Abstimmung andere && id.equals(andere.id)`. | `39aaee8` |
| R12, R13, R14 | abgelehnt | Siehe Bewertung oben (großer Testumbau, bewusste 4.2-Regel, vorzeitige Optimierung). | – |

Ergebnis: 187 Domänen- und Validator-Tests grün (vorher 170), plus 3 Helidon-Tests (`MainTest`, `TemplatesTest`).
