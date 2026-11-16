# Übung 4: Advanced Java, Test-Driven Design (TDD) und LLM-gestütztes Entwickeln

## 1. Review der Implementierungsstrategie

**Gegenstand:** Implementierungsstrategie, Domänenmodell, Services und Bounded Contexts aus
[3_Uebung.md](3_Uebung.md).

### (a) Schwachstellen der Strategie

1. **Event-Namen passen nicht zum Board.** Die Strategie nennt „Bürgerin erstellt“, das Board
   „Bürger registriert“. „Abstimmung abgeschlossen“ gibt es auf dem Board nicht, dort stehen nur die
   Policy „Nach Fristablauf“ und „Ergebnis angezeigt“.
2. **Widerspruch in der Teststrategie.** „Wir testen nur die Domain-Logik. Wir testen somit nur die
   Adapter.“ Sinnvoll wären reine Unit-Tests ohne Framework für die Domäne und wenige
   Integrationstests für die Adapter (H2-Repository, HTTP).
3. **Paketstruktur widerspricht Punkt 4.** Laut Einrückung liegen `domain`, `application` und
   `infrastructure` neben `abstimmungsverwaltung` statt darunter. Stimmabgabe, Kontoverwaltung und
   Abstimmungsansicht fehlen ganz.
4. **Anonymität vs. „nur einmal wählen“.** Die Stimme soll keinen Bezug zum Bürger haben, aber
   `stimmeAb(abstimmungsId, abstimmOption, buergerinnenId)` und die Beziehung Bürgerin–Stimmabgabe im
   Domänenmodell koppeln beides. Vorschlag: eine **Teilnahme** (wer hat gewählt, sichert „nur
   einmal“) getrennt von der anonymen **Stimme** (was wurde gewählt).
5. **AbstimmungsService ist kein Domain Service.** Er lädt und speichert über das Repository und ist
   damit ein Anwendungsservice. Ein Domain Service wäre z. B. „E-Mail nur einmal je Kontoart“.
6. **Unklar, wer „Abstimmung beendet“ auslöst.** Ohne Scheduler ist es einfacher, den Status aus dem
   Abstimmungszeitraum und einer injizierten `Clock` abzuleiten, statt ein Event aktiv auszulösen.

### (b) Strukturierung der Domain-Events

- **Einheitliche Namen** in der Vergangenheitsform, je einem Context zugeordnet:

  | Context               | Events                                   |
  | --------------------- | ---------------------------------------- |
  | Kontoverwaltung       | `BuergerRegistriert`, `ZweiFaktorAktiviert` |
  | Abstimmungsverwaltung | `AbstimmungErstellt`, `AbstimmungBeendet`   |
  | Stimmabgabe           | `StimmeAbgegeben`                           |

- **„Ergebnis angezeigt“ und „Authentifiziert“ streichen.** Das Anzeigen ist eine Abfrage, keine
  fachliche Zustandsänderung, und die Authentifizierung ist technisch. Falls fachlich relevant, besser
  `ErgebnisFestgestellt`.
- **Payload nur mit IDs und dem Nötigsten:** `AbstimmungErstellt(abstimmungId, zeitraum, optionIds)`,
  `StimmeAbgegeben(abstimmungId, optionId)` ohne Bürger-ID.
- **Wichtigste Events:** `AbstimmungErstellt`, `StimmeAbgegeben`, `AbstimmungBeendet`.

### (c) Entwicklungsreihenfolge

1. **Durchstich pro Event statt Schicht für Schicht:** Value Objects und Entities → Repository-Port →
   Anwendungsservice mit In-Memory-Repository → erst dann der H2-Adapter. So sind Service und Regeln
   testbar, bevor es eine Datenbank gibt.
2. **Mit dem Kern beginnen:** `AbstimmungErstellt` → `StimmeAbgegeben` → `AbstimmungBeendet` mit
   Ergebnis.
3. **Kontoverwaltung zuletzt.** Sie ist eine generische Subdomäne. Bis dahin genügt ein Fake hinter der
   Schnittstelle `IstVa`/`IstBuerger` (die VA ist ohnehin ein fester Superuser, dazu ein Test-Bürger).
   Die Schnittstelle existiert von Anfang an, damit die Authentifizierung nicht nachträglich
   eingebaut werden muss.
4. **Zwei-Faktor ganz am Ende:** fachlich am wenigsten wert, technisch am aufwändigsten.

### Bewertung

- **Keine eventbasierte Architektur.** Die Domain-Events aus dem Event Storming beschreiben die
  Fachlichkeit und legen die Reihenfolge der Anwendungsfälle fest. Eine technische Event-Infrastruktur
  folgt daraus nicht, das LLM hatte dies gefolgert. Die Contexts kennen einander nur über IDs und
  definierte Schnittstellen. Punkt 4 der Strategie in [3_Uebung.md](3_Uebung.md) ist entsprechend angepasst.
- **Teststrategie klargestellt (a2).** Den Widerspruch in Punkt 5 („nur die Domain-Logik … somit nur
  die Adapter“) hat das LLM zu Recht gefunden. Gemeint war die Trennung, die wir in Aufgabe 2 schon
  umsetzen: Domänentests laufen ohne Framework und ohne Datenbank. Integrationstests schreiben wir nur
  bei Bedarf und dann mit einer In-Memory-Datenbank und mit Helidons `@ServerTest` für HTTP.

<!-- Domain-Events (mind. 2), angepasste Entwicklungsstrategie, übernommene/abgelehnte LLM-Vorschläge (ca. 5–6 Sätze) -->

## 2. Testfälle mit LLM generieren und validieren (TDD Schritt 1)

Bearbeitung: Hassan

**Gegenstand:** Event „Abstimmung erstellt“ im Context Abstimmungsverwaltung (Startpunkt aus Übung 3):
Fachregeln der Aggregate Root `Abstimmung` und Eingabevalidierung per Regex für Titel und Datum
(`AbstimmungEingabeValidator`).

**LLM-Einsatz:** Claude Pro (Opus 5.5), 16.11.2026, neuer Chat ohne Vorwissen. Der Prompt beschrieb
Kontext (Java, JUnit 5, DDD), die Signatur von `Abstimmung.erstelle(...)`, die fünf Fachregeln und die
Eingaberegeln und verlangte Happy-Path-Tests, Grenzfälle, Negativtests, Regex mit Erklärung und
JUnit-5-Testklassen, aber keine Implementierung.

**Testsuite** (`src/test/java/de/mse/votee_backend/abstimmungsverwaltung/`):

| Testklasse                                   | Inhalt                                                  | Testaufrufe |
| -------------------------------------------- | ------------------------------------------------------- | ----------- |
| `domain/model/AbstimmungTest`                | Happy Path, Grenzfälle, Negativtests je Regel, Annahmen | 31          |
| `domain/model/AbstimmungszeitraumTest`       | Ende nach Beginn, fehlende Werte                        | 3           |
| `domain/model/NutzerIdTest`                  | Wert gesetzt / leer                                     | 4           |
| `application/AbstimmungEingabeValidatorTest` | Titel- und Datumsformat                                 | 90          |

**Regex:**

- Titel (vorher Unicode-Normalisierung NFC und Rand-Leerzeichen entfernen):
  `^(?=.*[\p{L}0-9])[\p{L}0-9 ,.?!:()'"/%€-]{3,100}$`
- Datum `dd.MM.yyyy HH:mm`, Jahr 2000–2099:
  `^(0[1-9]|[12][0-9]|3[01])\.(0[1-9]|1[0-2])\.20[0-9]{2} ([01][0-9]|2[0-3]):[0-5][0-9]$`,
  zusätzlich `DateTimeFormatter` `dd.MM.uuuu HH:mm` mit `ResolverStyle.STRICT`, weil eine Regex
  keine Monatslängen und Schaltjahre kennt (31.04., 29.02.2027).

**Bewertung der LLM-Tests:**
Gut waren die systematischen Grenzwertpaare (2/3 und 100/101 Zeichen, Ende gleich Beginn vs. eine Minute
später, eine vs. zwei Optionen) und die Kombination aus Regex und strengem Datumsparser. Besonders
hilfreich war der Test mit nachgestelltem Zeilenumbruch, der einen echten Fehler aufdeckt, wenn die Regex
mit `find()` statt `matches()` geprüft wird. Die Aussage des LLM, alle 120 Tests seien grün, war nicht
nachprüfbar, weil es eine eigene, nicht gelieferte Implementierung und einen JUnit-Ersatz verwendet hatte;
wir haben die Tests deshalb selbst mit JUnit 5 ausgeführt. Angepasst haben wir die Paketnamen an unsere
Struktur, den Getter-Stil (`getTitel()` wie in der Musterlösung zu Übung 3) und die Lage des Validators
(`application`). Die vom LLM ohne Auftrag eingeführte Schnittstelle `domainEvents()` haben wir
entfernt, bis die Domain Events aus Aufgabe 1 abgestimmt sind; redundante Tests wie `genauZweiOptionenSindErlaubt`
(prüfte dasselbe wie der Happy Path mit Ja/Nein) haben wir gestrichen und
Tests für `NutzerId` und `Abstimmungszeitraum` in eigene Klassen verschoben. Ergänzt haben wir Tests zu drei
Lücken, die das LLM übersehen hat: Umlaute in zerlegter Unicode-Form wurden fälschlich
abgelehnt, ein Titel wie `"A  "` erreichte die Mindestlänge nur durch Leerzeichen, und das Jahr 0000 war
gültig. Außerdem haben wir die Titelregel gelockert, weil realistische Titel wie „Café Central“ oder
„Titel (neu)“ abgelehnt wurden.

Prompt und vollständige LLM-Antwort (inkl. ursprünglicher Regex und Testliste) stehen in
[4_2_LLM_Prompt_und_Antwort.md](4_2_LLM_Prompt_und_Antwort.md).

**Offen bis 4.3 bereit ist:** Darf der Beginn in der Vergangenheit liegen? Sollen Titel getrimmt gespeichert
werden? Braucht die Domäne selbst eine Längenregel für den Titel?

**Hinweis TDD:** Die Tests kompilieren erst, wenn die Klassen aus Aufgabe 3 existieren („rot“).
Für `@ParameterizedTest` wurde `junit-jupiter-params` in der `pom.xml` ergänzt.

## 3. Implementierung der Domänenlogik mit LLM-Pair-Programming (TDD Schritt 2)

<!-- Pair-Programming-Prozess mit dem LLM (ca. 10–12 Sätze) -->

## 4. Test-Erweiterung und Refaktorisierung (TDD Schritt 3)

<!-- Umgesetzte und abgelehnte Refactoring-Vorschläge (ca. 5–6 Sätze) -->

## 5. Modularität und Testbarkeit via CI/CD

<!-- Modularität, Bounded Contexts, CI-Pipeline inkl. Tests (ca. 8–10 Sätze) -->

## 6. Kritische Reflexion zu TDD, DDD und LLM-gestützter Entwicklung

<!-- Chancen und Risiken (ca. 10–15 Sätze) -->
