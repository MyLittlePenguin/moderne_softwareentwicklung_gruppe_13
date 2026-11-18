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
| Null-Prüfungen in `Abstimmungszeitraum` in `pruefeZeitpunkt()` zusammenführen | ✅ übernehmen (in Aufgabe 3) | echte Duplikation, geringes Risiko; dabei Exception-Typ vereinheitlichen (vom LLM übersehen) |
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

**Tool:**

**LLM-Einsatz:**

## 3. Technical Debt und Regelverletzungen mit LLM analysieren

**Tool:**

**LLM-Einsatz:**

## 4. Frontend-Entwicklung und Erweiterung der Anwendung

**LLM-Einsatz:**

## 5. Reflexion zum Einsatz von Metriken und LLM
