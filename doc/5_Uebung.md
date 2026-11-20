# Übung 5: Software- und Architekturmetriken für Codequalität und Architekturoptimierung

Bearbeitung: Hassan

**Gegenstand:** Code aus Übung 4 auf `main` (Stand `e53be0c`, 18.11.2026): Context Abstimmungsverwaltung,
Aggregate Root `Abstimmung` und `AbstimmungEingabeValidator`.

## 1. Überblick und Anwendung einfacher Metriken

**Tool:** SonarQube Community (lokal per Docker), Analyse über
`./mvnw clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar` am 18.11.2026.
Parameteranzahl pro Methode zusätzlich aus dem Quellcode gezählt, da SonarQube dafür keine eigene Kennzahl hat
(nur Regel `java:S107`, die erst ab 8 Parametern meldet).

### Gesamtwerte

| Metrik (Skript S. 129–132) | Wert | Einordnung |
| --- | --- | --- |
| Lines of Code | 464 | kleines Projekt |
| Klassen / Dateien | 11 / 12 | |
| Methoden (Functions) | 43 | |
| Kommentaranteil | 16,7 % | unter der Faustregel 30–60 % (Skript S. 130) |
| Cyclomatic Complexity (McCabe) | 83 | ≈ 1,9 pro Methode, niedrig |
| Cognitive Complexity | 43 | niedrig |
| Open Issues | 1 | siehe Aufgabe 3 |

### Werte pro Datei

| Datei | Cyclomatic Complexity | Methoden | CC pro Methode | max. Parameter |
| --- | --- | --- | --- | --- |
| `Abstimmung.java` | 35 (42 %) | 21 | 1,7 | **6** |
| `Titel.java` | 11 | 2 | **5,5** | 1 |
| `Abstimmungszeitraum.java` | 11 | 4 | 2,75 | 2 |
| `AbstimmungEingabeValidator.java` | 5 | 2 | 2,5 | 1 |
| übrige 7 Dateien | je 1–4 | je 1–3 | | ≤ 2 |

### Erkenntnisse

1. **`Abstimmung.java` ist groß, aber nicht kompliziert.** Die Klasse hat fast ein Drittel der LOC und 21 von 43 Methoden,
   die einzelnen Methoden sind aber einfach (CC 1,7 pro Methode). Als Aggregate Root bündelt sie bewusst die Fachregeln.
2. **Sechs Parameter** haben der (private) Konstruktor und beide `erstelle(...)`-Factories; von außen sind es zwei Methoden,
   von denen eine nur delegiert. Risiko: gleichartige Argumente beim Aufruf vertauschen.
3. **`Titel.java` hat die dichteste Logik** (CC 5,5 pro Methode): Validierung im Konstruktor und die eigene `stripSpaces()`-Schleife.
4. **`Abstimmungszeitraum.java`** prüft in drei Methoden identisch auf `null` (Duplikation) und wirft dort
   `IllegalArgumentException`, im Konstruktor dagegen `AbstimmungsregelVerletztException` (uneinheitlich).
5. **Kommentaranteil** liegt unter der Faustregel des Skripts.

**LLM-Einsatz:** ChatGPT GPT-6 Medium, 18.11.2026, neuer Chat ohne Vorwissen. Prompt mit den Metriken und den drei auffälligen
Dateien, Frage nach Optimierungen, Bewertung (Nutzen/Risiko/Aufwand/DDD) und unproblematischen Werten.
Prompt und vollständige Antwort: [5_1_LLM_Prompt_und_Antwort.md](5_1_LLM_Prompt_und_Antwort.md).

### Kritische Bewertung der LLM-Vorschläge

| LLM-Vorschlag | Bewertung | Begründung |
| --- | --- | --- |
| Null-Prüfungen in `Abstimmungszeitraum` in `pruefeZeitpunkt()` zusammenführen | ✅ sinnvoll (Maßnahme in Aufgabe 3) | echte Duplikation, geringes Risiko; dabei Exception-Typ vereinheitlichen (vom LLM übersehen) |
| 6 Parameter in Record `AbstimmungErstellung` bündeln | ⏸ vorerst nicht | verschiebt die 6 Werte nur in ein anderes Objekt; lohnt erst bei mehr Aufrufstellen (sagt das LLM selbst) |
| `pruefePflichtfelder()` aus `erstelle(...)` auslagern | ⏸ optional | CC sinkt nicht, es entsteht eine neue Methode mit 4 Parametern |
| `hatOption(AbstimmOption)` ohne `map()` | ❌ nicht übernehmen | Vorschlag berechnet `normalisierterText()` des Suchbegriffs für jede Option neu, unser Code nur einmal |
| „`hatOption(String)` kann Exception werfen“ | ❌ trifft nicht zu | `AbstimmOption` wirft nur bei `null`/leer, genau das fängt `hatOption(String)` vorher ab; LLM kannte die Datei nicht |
| `stripSpaces()` nicht durch `strip()` ersetzen | ✅ zustimmen | `strip()` würde Tabs entfernen statt abzulehnen, bewusste Entscheidung aus Übung 4 |
| Fachlich benannte Methoden (`kannAbgestimmtWerden()` …) behalten | ✅ zustimmen | drücken unterschiedliche Fachbedeutung aus (DDD) |
| Kommentaranteil 16,7 % „unproblematisch“ | ⚠️ Widerspruch | Skript nennt 30–60 % (S. 130); LLM argumentiert „Qualität statt Menge“ |
| Fristgrenze (Ende inklusiv?) fachlich klären | ✅ offene Fachfrage | wichtiger als die Metrik, im Team zu klären |

**Entscheidung:** In Aufgabe 1 werden die Vorschläge nur dokumentiert. Umsetzbare Maßnahmen werden in Aufgabe 3
(Technical Debt) priorisiert.

## 2. Test Coverage erweitern und Code Coverage verbessern

**Tool:** JaCoCo 0.8.16 (erste Version mit offizieller Java-27-Unterstützung), eingebunden in `pom.xml`
(`prepare-agent` + `report` in der Phase `verify`). Bericht nach `./mvnw clean verify` unter
`target/site/jacoco/index.html`.

**Begriffe (Skript S. 134):** *Instruction Coverage* misst, welcher Anteil der Bytecode-Befehle beim Testlauf ausgeführt
wurde. *Branch Coverage* misst, welcher Anteil der Verzweigungen (`if`, `&&`, `||`, Schleifen) in beiden Richtungen
durchlaufen wurde. Beides sagt nur, *dass* Code lief, nicht, *ob* die Tests das fachlich Richtige prüfen.

### Ausgangslage und Ergebnis

| Paket | Instructions vorher | Instructions nachher | Branches vorher | Branches nachher |
| --- | --- | --- | --- | --- |
| `abstimmungsverwaltung.domain.model` | 98 % | **100 %** | 94 % | **97 %** |
| `abstimmungsverwaltung.application` | 100 % | 100 % | 100 % | 100 % |
| `de.mse.votee_backend` (Helidon-Start) | 80 % | 80 % | n/a | n/a |
| **Gesamt** | **94 %** | **96 %** | **95 %** | **97 %** |

Das Ziel von ca. 80 % war schon vorher erreicht. Deshalb wurde gezielt der **kritische Bereich** untersucht: die
Domäne (`domain.model`) mit den Fachregeln der Aggregate Root `Abstimmung` und dem Value Object `Titel`.
Der Startcode in `de.mse.votee_backend` (`Main`, `PageService`, `Templates`) enthält keine Fachlogik und wird bewusst
nicht weiter abgedeckt.

### Gefundene Lücken in `domain.model`

| Stelle | Lücke | Ursache | Maßnahme |
| --- | --- | --- | --- |
| `Abstimmung.erstelle(..., Titel, ...)` | Zweig `titel == null` | alle Tests nutzen die `String`-Überladung, dort wirft schon `new Titel(null)` | neuer Test `titelObjektFehlt` mit `(Titel) null` |
| `Abstimmung.getTitelObjekt()` | nie aufgerufen | keine Verwendung im Code und in Tests (offener Punkt R12 aus Übung 4.4) | neuer Test `titelUeberladungSpeichertTitelObjekt` mit `assertSame` |
| `Abstimmung.equals(Object)` | Zweig „kein `Abstimmung`-Objekt“ | **Testfehler:** `assertNotEquals(null, a)` ruft `a.equals` nie auf, `assertNotEquals("…", a)` ruft `String.equals` auf | bestehenden Test korrigiert: `assertFalse(a.equals(null))`, `assertFalse(a.equals("…"))` |
| `Titel.stripSpaces(String)` | 2 von 8 Zweigen | nur bei reinem Leerzeichen-String erreichbar, den der Konstruktor vorher per `isBlank()` ablehnt | **bewusst akzeptiert**: Schutzabfrage, über die öffentliche Schnittstelle nicht erreichbar |

Die korrigierte `equals`-Prüfung ist die wichtigste Erkenntnis: Der Test war grün, hat aber nicht geprüft, was sein
Name verspricht. Erst die Branch Coverage hat das sichtbar gemacht.

**LLM-Einsatz:** ChatGPT GPT-6 Medium, 18.11.2026, neuer Chat ohne Vorwissen. Prompt mit den vier Lücken und der
Frage nach Edge Cases für die komplexesten Methoden (`Titel`-Konstruktor, `pruefeOptionen`).
Prompt und vollständige Antwort: [5_2_LLM_Prompt_und_Antwort.md](5_2_LLM_Prompt_und_Antwort.md).

### Kritische Bewertung der LLM-Vorschläge

| LLM-Vorschlag | Bewertung | Begründung |
| --- | --- | --- |
| Tests für Lücken 1–3 | ✅ übernommen (angepasst an vorhandene Fixtures) | Erklärung zur Argumentreihenfolge bei `assertNotEquals` korrekt |
| `stripSpaces`-Zweige nicht per Reflection erzwingen | ✅ zugestimmt | private Schutzabfrage, kein fachlicher Fall |
| EC 1: NFD-Umlaut wird normalisiert | ❌ schon vorhanden | `TitelTest.unicodeNfdWirdNormalisiert` |
| EC 2: Zeichen außerhalb der BMP zählen doppelt | ✅ neu (`zeichenAusserhalbBmpZaehlenDoppelt`) | dokumentiert aktuelles Verhalten (50 gültig, 51 zu lang); fachlich offen, ob „100 Zeichen“ so gemeint ist |
| EC 3: Zero Width Space (U+200B) | ✅ neu (`zeroWidthSpaceIstUngueltig`) | unsichtbares Zeichen aus Copy & Paste |
| EC 4: „Ja“ und „ ja “ als Duplikat | ❌ schon vorhanden | `AbstimmungTest`, Abschnitt Annahmen |
| EC 5: Optionsliste unveränderlich | ❌ schon vorhanden | `AbstimmungTest`, Abschnitt Annahmen |

Drei der fünf Edge Cases waren bereits getestet, weil das LLM die Testklassen nicht kannte. Neue Tests insgesamt:
4 (+ 1 korrigierter Test), Testanzahl von 190 auf 194.

**Fazit:** 100 % Coverage ist kein sinnvolles Ziel. Die verbleibenden 2 Zweige sind nicht erreichbar, und die
wertvollste Verbesserung (der `equals`-Test) hat die Coverage nur um einen Zweig erhöht, aber einen falschen Test
aufgedeckt.

## 3. Technical Debt und Regelverletzungen mit LLM analysieren

**Begriff (Skript S. 154–155):** *Technical Debt* fasst Regelverstöße, Fehler und zu hohe Komplexität als „Schuld“
zusammen. Die „Zinsen“ sind der Mehraufwand, den jede spätere Änderung verursacht. Werkzeuge wie SonarQube rechnen diese
Schuld in Arbeitszeit um.

**Tools (19.11.2026, Stand `64fae10` auf `abgabe_05`):**

- **SonarQube Community** (Regelwerk „Sonar way“) für Regelverletzungen und Code-Duplizierung
- **PMD 7.29 mit CPD** (Copy-Paste-Detector) über `./mvnw pmd:pmd pmd:cpd`, eigenes Regelwerk `pmd-ruleset.xml`
  (bestpractices, errorprone, design, multithreading, performance; Schwellen: Klassen-CC 30, Methoden-CC 10, Meldung ab
  6 Parametern, Cognitive Complexity 15, max. 20 Methoden; CPD ab 30 Tokens)
- **versions-maven-plugin 2.7** über `./mvnw versions:display-dependency-updates versions:display-plugin-updates`
  für veraltete Abhängigkeiten

### Ergebnisse

| Bereich | Ergebnis |
| --- | --- |
| SonarQube Issues | vorher 1 (Reliability, High, 5 min: `assertNotEquals("…", a)` vergleicht verschiedene Typen), nachher **0**; der Fehler wurde schon in Aufgabe 2 behoben |
| SonarQube Maintainability / Debt | 0 Issues, Quality Gate bestanden |
| Code-Duplizierung | SonarQube **0,0 %** (0 Zeilen, 0 Blöcke), PMD-CPD **0** Duplikate |
| PMD-Regelverletzungen | **8** Meldungen (s. u.) |
| Abhängigkeiten und Plugins mit eigener Versionsangabe in der `pom.xml` | alle aktuell (z. B. Thymeleaf, Checkstyle, PMD, JaCoCo) |
| Veraltet, über den Helidon-Parent verwaltet | Helidon-Parent 27.0.1 → 27.0.2 (inkl. `helidon-licensing`), H2 2.4.240 → 2.5.252 (eingebunden, zum Analysezeitpunkt noch nicht genutzt), `hamcrest-all` 1.3 (2012, eingestellt) |

Das versions-Plugin listet rund 450 Updates. Fast alle stammen aus der Stückliste (BOM) von Helidon und betreffen
Bibliotheken, die wir nicht nutzen (z. B. Jackson, MongoDB, Testcontainers). Neuere Versionen von JUnit (6.2.0-M1) und
SLF4J (2.1.0-alpha1) sind Vorabversionen und werden bewusst nicht übernommen.

Die Komplexität von `Abstimmung` misst PMD mit 42, SonarQube mit 35. Beide Tools zählen Verzweigungen offenbar
unterschiedlich (vermutlich `&&`, `||` und `? :`), was nicht im Detail geprüft wurde.

### Technische Schuld nach Schweregrad

Die Einstufung ist unsere fachliche Bewertung (nach Abgleich mit dem LLM, s. u.) und keine direkte Übernahme der
PMD-Prioritäten (1 = kritisch bis 5 = gering).

| Schweregrad | Befund | Quelle | Maßnahme |
| --- | --- | --- | --- |
| Hoch | – | | |
| Mittel | `hamcrest-all` 1.3 seit 2012 nicht mehr gepflegt, genutzt in `MainTest` und `TemplatesTest` | versions | Artefakt gegen `org.hamcrest:hamcrest` 3.0 tauschen (Imports bleiben gleich) |
| Mittel | Uneinheitlicher Exception-Vertrag in `Abstimmungszeitraum`: Konstruktor wirft `AbstimmungsregelVerletztException`, drei Methoden `IllegalArgumentException`, dazu dreifach identische Null-Prüfung | eigene Analyse (Aufgabe 1) | Regel festlegen und per Javadoc dokumentieren (Fachregel → eigene Exception, `null`-Argument → `IllegalArgumentException`), Null-Prüfung in eine Methode zusammenführen |
| Mittel | Zwei `erstelle`-Überladungen (`Titel`/`String`) und zwei Getter `getTitel()`/`getTitelObjekt()` | eigene Analyse, PMD (6 Parameter) | `Titel`-Variante als bevorzugte API festlegen und dokumentieren; Getter erst bei einer größeren API-Überarbeitung umbauen |
| Niedrig | Helidon 27.0.1 statt 27.0.2 (Patch-Release, auch `helidon-licensing`) | versions | Parent-Version anheben, danach `./mvnw clean verify` und Server-Start prüfen |
| Niedrig | `System.out.println` in `Main` (PMD Priorität 2) | PMD | `System.Logger` des JDK verwenden |
| Niedrig | Fehlende `serialVersionUID` in `AbstimmungsregelVerletztException` | PMD | Konstante ergänzen |
| Niedrig | Exception als Kontrollfluss in `AbstimmungEingabeValidator.istGueltigerTitel` mit breitem `catch (IllegalArgumentException)` | eigene Analyse | auf `AbstimmungsregelVerletztException` einengen; `Titel` wirft an allen 4 Stellen nur diese |
| Niedrig | `maven-site-plugin` ohne Version, keine Mindest-Maven-Version | versions | Version festlegen; Enforcer nicht nötig, da Maven Wrapper (Maven 3.10.0) und CI (Java 27) die Umgebung schon festlegen |
| Niedrig | H2 2.4.240 statt 2.5.252 (zum Analysezeitpunkt noch nicht genutzt) | versions | behalten und beim Einbau des H2-Repositorys (Aufgabe 4) mit aktualisieren |
| Kein Handlungsbedarf | CC 10 in `erstelle` und `pruefeOptionen`, Klassen-CC 42, 6 Parameter | PMD | Komplexität entsteht aus den Invarianten der Aggregate Root; Aufteilen würde die Regeln nur verstreuen |
| Kein Handlungsbedarf | ungenutzter Parameter `request` in `PageService` | PMD | Fehlalarm: Helidon schreibt die Handler-Signatur vor |
| Kein Handlungsbedarf | JUnit 6.2.0-M1, SLF4J 2.1.0-alpha1 | versions | Vorabversionen, auf stabiles Release warten |

**Entscheidung:** Die Maßnahmen werden in dieser Aufgabe nur dokumentiert und nicht umgesetzt. Der Code bleibt auf dem
getesteten Stand (194 Tests grün). Die Maßnahmen mit Schweregrad „Mittel“ sind für die nächste Iteration vorgesehen.

**LLM-Einsatz:** ChatGPT GPT-6 Medium, 19.11.2026, neuer Chat ohne Vorwissen. Prompt mit allen Messergebnissen, den
drei eigenen Beobachtungen und den betroffenen Code-Auszügen; Frage nach Schweregrad, Maßnahmen mit Aufwand,
Top 3 und den Unterschieden zwischen SonarQube und PMD.
Prompt und vollständige Antwort: [5_3_LLM_Prompt_und_Antwort.md](5_3_LLM_Prompt_und_Antwort.md).

### Kritische Bewertung der LLM-Vorschläge

| LLM-Vorschlag | Bewertung | Begründung |
| --- | --- | --- |
| Komplexität und 6 Parameter in `Abstimmung` belassen, kein Builder | ✅ zugestimmt, Einstufung leicht abweichend | Invariantenprüfung gehört in die Aggregate Root; Builder wäre Overengineering. Das LLM stuft Klassen-CC 42 und eine 6-Parameter-Meldung als „Niedrig“ (beobachten) ein, wir als „kein Handlungsbedarf“ |
| `request`-Parameter als Fehlalarm akzeptieren | ✅ zugestimmt | Signatur ist von Helidon vorgegeben |
| Exception-Vertrag festlegen statt alles zu vereinheitlichen | ✅ übernommen | Unterscheidung Fachregel vs. Programmierfehler ist begründbar |
| Top 1: `hamcrest-all` ersetzen (Tests auf JUnit-Assertions umschreiben, sofern keine speziellen Matcher nötig; sonst aktuelle Hamcrest-Version) | ✅ Alternative des LLM gewählt | Die Tests nutzen Matcher (`is`, `containsString`); der Tausch auf `hamcrest` 3.0 ändert nur die `pom.xml` |
| Top 2: Maven Enforcer (Schweregrad „Mittel“) | ❌ überbewertet → Niedrig | Maven Wrapper legt Maven 3.10.0 fest, CI legt Java 27 fest; das LLM empfiehlt den Wrapper „zusätzlich“, obwohl er existiert |
| H2 prüfen und nach YAGNI ggf. entfernen | ✅ behalten | Das LLM rät selbst, H2 zu behalten, wenn es bald gebraucht wird. Das ist der Fall: Das H2-Repository war laut Strategie (Übung 4.1) der nächste Schritt und ist mit Aufgabe 4 als dateibasierte Datenbank umgesetzt |
| `catch` im Validator auf `AbstimmungsregelVerletztException` einengen | ✅ zugestimmt, gegen Code geprüft | `Titel` wirft nur diese Exception; das LLM schrieb die Methode fälschlich als `static` |
| Logging mit SLF4J | ⚠️ angepasst | der JDK-eigene `System.Logger` reicht ohne zusätzliche Abhängigkeit im Code |
| Selbstkorrektur: `erstelle(id, nutzer, null, …)` ist mehrdeutig | ✅ korrekt | kompiliert nicht; deshalb nutzt unser Test aus Aufgabe 2 `(Titel) null` |
| „0 Issues heißt nicht keine Schuld“ | ✅ zentrale Erkenntnis | Ergebnis hängt vom Regelwerk ab: SonarQube 0, PMD 8 Meldungen auf demselben Code |

**Fazit:** Die messbare technische Schuld ist gering. Die relevanten Schulden hat keines der Analyse-Tools für den
Code (SonarQube, PMD) gemeldet: Die veraltete Testbibliothek fand erst das versions-Plugin, den unklaren
Exception-Vertrag und die doppelte API die eigene Analyse aus Aufgabe 1. Umgekehrt waren die meisten PMD-Meldungen keine echte Schuld. Kennzahlen zeigen also, *wo* man
hinschauen sollte, die Bewertung braucht aber fachliches Verständnis.

## 4. Frontend-Entwicklung und Erweiterung der Anwendung

**LLM-Einsatz:**

## 5. Reflexion zum Einsatz von Metriken und LLM

Bearbeitung: Hassan

Ich habe SonarQube in dieser Übung zum ersten Mal benutzt, und die Einrichtung mit Docker und Token hat mir sogar Spaß
gemacht. Gelernt habe ich vor allem, dass Metriken ein Wegweiser sind und kein Urteil. Die Zahlen zeigen, wo man
hinschauen sollte. Ob dort wirklich ein Problem liegt, musste ich selbst bewerten: Die Komplexität von 10 in `erstelle`
und die sechs Parameter sind für eine Aggregate Root mit ihren Invarianten in Ordnung. Mein größter Aha-Moment war die
Branch Coverage in Aufgabe 2. Ein grüner Test mit `assertNotEquals(null, a)` hat gar nichts geprüft, und erst die
fehlende Verzweigung hat das sichtbar gemacht. Außerdem widersprechen sich die Tools: 464 Codezeilen bei SonarQube, 346
im Auswertungsskript von Ronny, eine Klassenkomplexität von 35 oder 42, null oder acht Meldungen. Bevor man eine Zahl
verwendet, muss man wissen, wie das Tool zählt.

Das LLM hat am meisten beim Einordnen geholfen, zum Beispiel bei der Frage, ob eine Meldung echte Schuld oder ein
Fehlalarm ist. Bei den Edge Cases brachte es zwei neue Ideen, zum Beispiel das unsichtbare Zero Width Space. Am
wenigsten hilfreich war es bei Aussagen über unseren konkreten Code. Drei von fünf vorgeschlagenen Edge Cases waren
schon getestet, es empfahl einen Maven Wrapper, den wir längst haben, und schrieb eine unserer Methoden fälschlich als
`static`. Deshalb habe ich jede Aussage des LLM gegen den Code geprüft. Das kostet Zeit, verhindert aber eine falsche
Dokumentation.

Erschwert wurde die Übung durch den Ausstieg eines Projektmitarbeiters. Das hat Ronny und mich zeitlich zurückgeworfen,
sodass wir keine Präsentation halten und kein Feedback erhalten konnten.

Mein Fazit: Metriken und LLM ergänzen sich gut, weil das Tool misst und das LLM beim Einordnen hilft. Die Verantwortung
für die Bewertung bleibt aber beim Menschen.
