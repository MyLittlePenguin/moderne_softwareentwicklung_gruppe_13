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
- **Paketstruktur nur exemplarisch.** Unsere Paketstruktur zu Übung 3 war nur exemplarisch und sollte
  keine feste Vorgabe darstellen. Das LLM hat hier einen Punkt aufgebracht, der nicht relevant war.
- **In-Memory-Repository** wird in Form einer H2-In-Memory-DB dargestellt und es werden nicht extra
  In-Memory-Datenstrukturen erstellt. Das würde die Testimplementierung verkomplizieren.
- **Teststrategie klargestellt (a2).** Den Widerspruch in Punkt 5 („nur die Domain-Logik … somit nur
  die Adapter“) hat das LLM zu Recht gefunden. Gemeint war die Trennung, die wir in Aufgabe 2 schon
  umsetzen: Domänentests laufen ohne Framework und ohne Datenbank. Integrationstests schreiben wir nur
  bei Bedarf und dann mit einer In-Memory-Datenbank und mit Helidons `@ServerTest` für HTTP.
- **Domain-Events und Reihenfolge (b, c).** Die wichtigsten Domain-Events sind `AbstimmungErstellt`
  und `StimmeAbgegeben`, dazu `AbstimmungBeendet`. Die Umsetzung erfolgt als Durchstich pro Event in
  dieser Reihenfolge, beginnend mit `AbstimmungErstellt`. Die Kontoverwaltung kommt zuletzt, weil die
  VA ein fester Superuser ist und die Tests aus Aufgabe 2 als Ersteller nur eine `NutzerId` brauchen.

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

Bearbeitung: Ronny (Implementierung), Hassan (Doku)

**LLM-Einsatz:** ChatGPT 5.6 Sol, 17.11.2026, Branch `abgabe_04_implementation`. Interaktionen 1–8
stehen im Interaktionslog ([../llm_log/interaktionslog.md](../llm_log/interaktionslog.md)), das
LLM-Code-Review in [DDD_Review.md](DDD_Review.md). Der letzte Schritt (Value Object `Titel`) ist dort noch
nicht protokolliert.

**Pair-Programming-Prozess:**
Wir haben dem LLM unsere Testsuite aus Aufgabe 2 gegeben und es gebeten, eine Implementierung zu erstellen,
die die Tests erfüllt, und dabei eine Funktion nach der anderen anzugehen. Das LLM ging von innen nach
außen vor (`NutzerId`, `Abstimmungszeitraum`, `AbstimmOption`, dann die Aggregate Root `Abstimmung` mit
Factory-Methode und zuletzt der `AbstimmungEingabeValidator`) und führte nach jedem Schritt die passende
Testklasse aus, sodass jeder Schritt einzeln nachvollziehbar blieb. Hilfreich war es auch bei der
Fehlersuche, als `mvn test` lokal scheiterte: Fedora erzwingt über `/etc/java/maven.conf` Java 25, während
der Maven-Wrapper `./mvnw` das aktive Java 27 nutzt. Anschließend ließen wir das LLM den Code auf
DDD-Prinzipien reviewen; es fand vier Lücken: keine Identität der Abstimmung, ein anämisches Modell ohne
fachliches Verhalten, kein Domain Event und eine Titelregel nur im Validator der Application-Schicht.
Daraufhin haben wir testgetrieben `AbstimmungId` (Record, Factory `neu()` mit UUID, ID-basiertes
`equals`/`hashCode`) und fachliche Methoden wie `istLaufend` und `kannErgebnisEingesehenWerden` ergänzt und
die Titelregel in ein Value Object `Titel` verschoben, an das der Validator nun delegiert. Gut eingesetzt
hat das LLM Records mit kompaktem Konstruktor zur Selbstvalidierung und `List.copyOf` für eine
unveränderliche Optionsliste. Streams, Lambdas, Optional und Generics hat es dagegen nicht verwendet:
Duplikatprüfung und `hatOption` sind klassische Schleifen, wo `anyMatch` lesbarer wäre. Für Wartbarkeit
und Lesbarkeit kritisch sind außerdem zwei überladene `erstelle`-Methoden (mit `String` und mit `Titel`)
und ein englischer Alias `Title` neben `Titel`, der unserer Ubiquitous Language widerspricht und keinen
Mehrwert hat. Korrigieren mussten wir auch, dass das Review nach dem letzten Refactoring die Titelregel
noch als offen führte; Aussagen des LLM zum eigenen Stand haben wir deshalb gegen den Code geprüft. Die
vom LLM gemeldeten Testergebnisse (zuletzt 149 grüne Tests vor dem `Titel`-Schritt) haben wir lokal mit
`./mvnw test` (Java 27) nachvollzogen: Nach dem `Titel`-Schritt laufen alle 173 Tests grün, darunter 24 neue
Tests für `Titel`. Offen für Aufgabe 4 bleiben das Domain
Event `AbstimmungErstellt` (die im 4.2-Testentwurf vorgeschlagene Methode `domainEvents()` hatten wir
bewusst entfernt), das `AbstimmungRepository` und fachliche Exceptions statt `IllegalArgumentException`.

## 4. Test-Erweiterung und Refaktorisierung (TDD Schritt 3)

<!-- Umgesetzte und abgelehnte Refactoring-Vorschläge (ca. 5–6 Sätze) -->

Bearbeitung: Hassan

**LLM-Einsatz:** Claude (Opus 5.5), 17.11.2026, Branch `abgabe_04_refactoring`. Prompt, Antwort und unsere
Bewertung jedes Vorschlags stehen in [4_4_LLM_Prompt_und_Antwort.md](4_4_LLM_Prompt_und_Antwort.md); jeder
Arbeitsschritt ist ein eigener Commit (Test und Code eines Schritts gemeinsam, die rote Phase wurde vor dem
Commit lokal geprüft).

Das LLM fand mit Optionen in zerlegter Unicode-Form (z. B. „Grün“ von macOS) einen echten Fehler: Sie wurden
nicht als Duplikat erkannt; wir haben das zuerst mit einem roten Test belegt und dann die Vergleichsregel in
`AbstimmOption.normalisierterText()` gebündelt, statt sie doppelt in `Abstimmung` zu pflegen. Außerdem haben
wir den überflüssigen Alias `Title` gelöscht, die Optionsprüfung aus der langen Factory-Methode extrahiert,
unerreichbare Prüfungen entfernt, `hatOption` auf `stream().anyMatch()` und `equals` auf Pattern Matching
umgestellt, die Titellänge nur noch an einer Stelle geprüft und eine fachliche
`AbstimmungsregelVerletztException` eingeführt, die von `IllegalArgumentException` erbt, sodass alle Tests
aus 4.2 unverändert grün bleiben. Nicht sinnvoll waren ein Stream für die Duplikatprüfung (die Schleife bricht
früher ab und nennt das Duplikat), `Optional` für die Beschreibung (widerspricht der in 4.2 festgelegten Regel
„leer = keine Beschreibung“) und ein Cache für `hatOption`, weil Abstimmungen nur wenige Optionen haben und
das vorzeitige Optimierung wäre. Zwei vom LLM als fehlend gemeldete Edge-Cases (Zeitpunkt genau am
Fristende, `null` als Zeitpunkt) waren bereits getestet; das LLM hatte vorhandene Tests übersehen, weshalb
wir jede Behauptung gegen den Code geprüft haben. Nach allen Schritten laufen 190 Tests grün (vorher 173).

## 5. Modularität und Testbarkeit via CI/CD

<!-- Modularität, Bounded Contexts, CI-Pipeline inkl. Tests (ca. 8–10 Sätze) -->

Bearbeitung: Hassan

**(a) Modularität und Testbarkeit.** Jeder Bounded Context aus Übung 3 bekommt einen eigenen Paketbaum unter
`de.mse.votee_backend`; umgesetzt ist bisher nur der Startpunkt `abstimmungsverwaltung` mit den Schichten
`domain.model` (Aggregate Root `Abstimmung`, Value Objects, fachliche Exception) und `application`
(`AbstimmungEingabeValidator`). Die Abhängigkeiten zeigen nur nach innen: `application` nutzt `domain.model`
(z. B. delegiert der Validator an das Value Object `Titel`), die Domäne importiert ausschließlich Klassen aus
`java.*` und kennt weder Helidon noch die Datenbank. Dadurch lässt sich die gesamte Fachlogik mit reinen
JUnit-5-Tests ohne Server, Mocks oder Datenbank prüfen; die Testpakete spiegeln die Paketstruktur des Codes.
Die anderen Contexts (Kontoverwaltung, Stimmabgabe, Abstimmungsansicht) sollen nur über IDs wie `AbstimmungId`
und `NutzerId` verbunden werden. Offen gegenüber der Strategie aus Übung 3 sind noch das Repository-Interface
`AbstimmungRepository` und die `infrastructure`-Schicht (H2 als dateibasierte Datenbank), die als nächster Schritt
folgen.

**(b) CI-Pipeline.** Die GitHub-Actions-Pipeline aus Übung 2 (`.github/workflows/maven.yml`) läuft bei jedem Push
auf `main` und bei jedem Pull Request: Sie richtet JDK 27 (Temurin) ein, prüft zuerst den Code-Stil mit
`./mvnw -B checkstyle:check` und führt dann mit `./mvnw -B verify` Kompilierung, alle Tests und das Paketieren aus.
Die in Aufgabe 2 bis 4 geschriebenen Tests laufen damit automatisch mit, ohne dass die Pipeline angepasst werden
musste, weil Maven Surefire alle Testklassen unter `src/test/java` findet. Schlägt ein Test fehl, wird der PR rot
markiert; das haben wir in Aufgabe 2 bewusst genutzt (TDD-Rot im Draft-PR #13, bis die Implementierung aus
Aufgabe 3 vorlag). Die Testberichte werden immer als Artefakt hochgeladen, auch bei Fehlern, damit man die
Ursache nachlesen kann.

## 6. Kritische Reflexion zu TDD, DDD und LLM-gestützter Entwicklung

<!-- Chancen und Risiken (ca. 10–15 Sätze) -->

### Reflexion Hassan

TDD hat mich gezwungen, die Fachregeln zu klären, bevor eine Zeile Code existierte: Erst beim Schreiben der Tests
mussten wir festlegen, welche Zeichen ein Titel enthalten darf, welche Jahre gültig sind und wie zerlegte Umlaute
behandelt werden. Dass der Draft-PR in der CI so lange rot blieb, bis die Implementierung aus Aufgabe 3 vorlag, hat
für mich sichtbar gemacht, dass „erst Test, dann Code“ tatsächlich funktioniert. DDD hat uns eine gemeinsame Sprache
gegeben, etwa `Titel` als Value Object und die Fachregeln in der Aggregate Root `Abstimmung` statt in einem Service.
Schwierig war, dass das Teammodell und mein eigenes Modell aus Übung 3 auseinanderlagen und wir uns erst auf eine
gemeinsame Basis einigen mussten. Die LLMs haben meinen Prozess beschleunigt, weil sie in kurzer Zeit viele
Testideen, Regex-Entwürfe und Reviews liefern; der Großteil meiner Zeit ging dafür aber in das Prüfen dieser
Vorschläge. Dabei habe ich deutliche Grenzen erlebt: Das LLM hat bereits vorhandene Tests übersehen, Aussagen über
Code getroffen, den es gar nicht gesehen hatte, und „Verbesserungen“ vorgeschlagen, die schlechter waren als unser
Code. Ein weiteres Risiko betrifft die Arbeit in einer Firma: Gute Antworten bekommt man nur mit viel Kontext, also
mit eigenem Code und der Projektstruktur. Damit verlassen aber vertrauliche Firmeninformationen das Haus, und sobald
Testdaten oder Logs echte Personendaten enthalten, ist auch der Datenschutz nach DSGVO betroffen. Weniger Kontext
schützt die Daten, verschlechtert aber genau die Antworten, wie unsere Erfahrungen zeigen; Abhilfe schaffen nur
Firmenlizenzen ohne Training auf den Eingaben, lokal betriebene Modelle oder bewusst minimierte Ausschnitte. Damit
ich den Code verstehe und nicht nur kopiere, habe ich jeden Vorschlag gegen den Code und die Tests geprüft und in
Bewertungstabellen mit Begründung festgehalten, ob ich ihn übernehme oder ablehne. Gut eignen sich LLMs aus meiner
Sicht für Testideen, Edge Cases, Regex-Entwürfe und Erklärungen. Schlecht geeignet sind sie für fachliche
Entscheidungen, etwa ob eine Stimme genau zum Fristende noch zählt, und für alles, wofür man den Zusammenhang des
ganzen Projekts kennen muss. Mein Fazit: Das LLM ist ein schneller Ideengeber, die Verantwortung für den Code bleibt
aber bei mir.

### Erfahrungsbericht Ronny (aus Aufgabe 3 hierher verschoben)

Vorweg möchte ich mitteilen, dass ich auf Arbeit auch bereits genötigt werde, mit
LLM-Systemen zur Software-Entwicklung zu arbeiten. Das ist der Grund, warum ich
nach 15 leidenschaftlichen Jahren als Software-Entwickler, Bewerbungen schreibe,
um diesen Job aufzugeben und nur noch in meiner Freizeit Software für mich zu
entwickeln.

**zu meinem Erlebnis bei dieser Aufgabe:** Ich nutze ein kostenloses Kontingent,
da ich nicht bereit bin, Geld für LLM-Tools auszugeben. Das geht
selbstverständlich mit Einschränkungen einher. Ich werde regelmäßig darauf
hingewiesen, dass die Last auf den Servern gerade zu hoch sei und ich es in
wenigen Minuten erneut versuchen soll. Das ist frustrierend. Grundsätzlich eine
erste Implementation zu generieren, welche die Tests erfüllt hat dennoch relativ
Problemlos funktioniert. Der Quellcode war gut lesbar und verständlich. Das
einzige, was schwer zu verstehen ist, ist einer der regulären Ausdrücke für die
Input-Validierung. Das ist allerdings auch allgemein nicht unüblich bei
regulären ausdrücken, wenn man diese zu einem späteren Zeitpunkt sich erneut
ansehen möchte. Dieses inhärente Problem von regulären Ausdrücken kann nicht
seriös LLM's angelastet werden.

Ein Review mit diesen LLM-Tools funktioniert meiner Erfahrung unzuverlässig gut.
Oft werden Probleme gefunden, die man als Mensch schnell übersehen kann.
Allerdings werden auch Probleme gefunden, die nicht real sind. Beim beheben
kommt es gelegentlich durchaus vor, dass andere Teile "beschädigt" werden und
Fehler eingebaut werden. Grundsätzlich funktionieren Reviews oft in einer
neuen Session besser. In diesem noch sehr kleinen Projekt hat das Review gut
funktioniert. Die Prinzipien von DDD wurden mit überprüft und sinnvolle
Anmerkungen für die Korrektheit und die Einhaltung von DDD wurden zurück
gegeben, mit denen nach und nach in Dialog-Form die Probleme behoben werden
konnten. Grundsätzlich sollte man, wenn man mit LLM's Code generiert immer
mehrere LLM-Review-/Problembehebungsschleifen durchführen, da im Review oft
festgestellt wird, dass der von der LLM generierte Code noch Fehler enthält oder
Refaktoriert werden sollte. Menschliches Review wird auch immer empfohlen, wird
allerdings mit zunehmender Code Menge, die generiert wird, immer schwieriger.

Mein Kostenloses Kontingent an Tokens war relativ schnell erreicht. Ich konnte
nicht einmal die Aufgabe 4.3 vollständig bearbeiten, sodass ein Team-Kollege
übernehmen musste.
