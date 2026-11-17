# LLM Interaktionslog – Übung 4: TDD & Domänenimplementierung

Dieses Verzeichnis dokumentiert die Interaktionen, Schritte und Entscheidungen bei der testgetriebenen Implementierung (TDD) des Bounded Contexts „Abstimmungsverwaltung“ im Projekt **votee**.

---

## Interaktion 1

### Benutzeranfrage
> sieh dir die tests an. erstelle eine erste implementation, welche die tests erfüllt. gehe dabei eine funktion nach der anderen an. logge bitte unsere interaktionen in einem von dir angelegten verzeichnis mit dem namen llm_log

### 1. Analyse der Testsuite und Ausgangslage
Vor Beginn der Implementierung existierten in `src/test/java/de/mse/votee_backend/` folgende Tests für den Kontext `abstimmungsverwaltung`:
- `domain/model/NutzerIdTest` (4 Tests):
  - Prüft das Value Object `NutzerId(String wert)`.
  - Invariante: Wert darf nicht `null`, leer (`""`) oder rein Whitespace (`"   "`) sein (`IllegalArgumentException`).
- `domain/model/AbstimmungszeitraumTest` (3 Tests):
  - Prüft das Value Object `Abstimmungszeitraum(LocalDateTime beginn, LocalDateTime ende)`.
  - Invariante (Regel 4): `beginn` und `ende` dürfen nicht `null` sein, `ende` muss strikt nach `beginn` liegen (`IllegalArgumentException`).
- `domain/model/AbstimmungTest` (31 Tests):
  - Prüft das Aggregate Root `Abstimmung` und das Value Object `AbstimmOption`.
  - Factory: `Abstimmung.erstelle(NutzerId erstelltVon, String titel, String beschreibung, Abstimmungszeitraum zeitraum, List<AbstimmOption> optionen)`.
  - Fachregeln:
    - Regel 1: `erstelltVon != null`.
    - Regel 2: `titel != null && !titel.isBlank()`.
    - Regel 3: `beschreibung` ist optional (`null` oder blank wird zu `""`).
    - Regel 4: `zeitraum != null`, Frist entspricht `zeitraum.ende()`.
    - Regel 5: Mindestens 2 Optionen, keine Duplikate (Groß-/Kleinschreibung und Randleerzeichen ignoriert), Optionstext nicht leer, keine Null-Werte, defensive Kopie der Optionsliste, unveränderliche Rückgabeliste (`List.copyOf`).
- `application/AbstimmungEingabeValidatorTest` (90 Tests):
  - Prüft `AbstimmungEingabeValidator`:
    - `istGueltigerTitel(String)`: 3–100 Zeichen, Satzzeichen `,.?!:()'"/%€-`, Unicode-NFC-Normalisierung, Randleerzeichen zählen nicht zur Mindestlänge.
    - `istGueltigesDatum(String)`: Format `dd.MM.yyyy HH:mm`, Jahre 2000–2099, strikte Kalenderprüfung (Schaltjahre, Monatslängen) via `DateTimeFormatter` mit `ResolverStyle.STRICT`.

---

### 2. Schrittweises TDD-Vorgehen (Funktion nach Funktion)

#### Schritt 1: Value Object `NutzerId`
- **Datei:** `src/main/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/NutzerId.java`
- **Implementierung:** Java `record NutzerId(String wert)` mit kompakter Validierung im Konstruktor:
  ```java
  public NutzerId {
      if (wert == null || wert.isBlank()) {
          throw new IllegalArgumentException("NutzerId darf nicht leer sein.");
      }
  }
  ```
- **Test-Ergebnis (`NutzerIdTest`):** 4 Tests ausgeführt, 0 Fehler. ✅

#### Schritt 2: Value Object `Abstimmungszeitraum`
- **Datei:** `src/main/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/Abstimmungszeitraum.java`
- **Implementierung:** Java `record Abstimmungszeitraum(LocalDateTime beginn, LocalDateTime ende)`:
  - Prüft `beginn != null`, `ende != null`.
  - Prüft `!ende.isAfter(beginn)` und wirft `IllegalArgumentException` bei Gleichheit oder früherem Ende.
- **Test-Ergebnis (`AbstimmungszeitraumTest`):** 3 Tests ausgeführt, 0 Fehler. ✅

#### Schritt 3: Value Object `AbstimmOption`
- **Datei:** `src/main/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/AbstimmOption.java`
- **Implementierung:** Java `record AbstimmOption(String text)`:
  - Prüft im Konstruktor `text == null || text.isBlank()` und wirft `IllegalArgumentException`.
  - Lässt Text mit Randleerzeichen unverändert zu (z. B. `" Ja"`), damit die Aggregate Root die Normalisierung und Duplikatserkennung übernimmt.
- **Verifikation:** Ermöglicht fehlerfreie Instanziierung gültiger Optionen und fängt ungültige leere Optionen ab. ✅

#### Schritt 4: Aggregate Root `Abstimmung`
- **Datei:** `src/main/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/Abstimmung.java`
- **Implementierung:**
  - Privater Konstruktor und statische Factory-Methode `Abstimmung.erstelle(...)`.
  - Invariantenprüfung:
    - Regel 1: `erstelltVon` Pflichtfeld.
    - Regel 2: `titel` Pflichtfeld (nicht blank).
    - Regel 3: `beschreibung` optional (`null` oder blank wird zu `""`).
    - Regel 4: `zeitraum` Pflichtfeld; `getFrist()` delegiert an `zeitraum.ende()`.
    - Regel 5: Optionen auf `null`, Mindestanzahl `>= 2`, `null`-Elemente und blanke Texte geprüft.
    - Duplikatprüfung: Normalisierung via `text.strip().toLowerCase(Locale.ROOT)` in einem `HashSet`.
    - Unveränderlichkeit: Defensive Kopie via `List.copyOf(optionen)`.
- **Test-Ergebnis (`AbstimmungTest`):** 31 Tests ausgeführt, 0 Fehler. ✅

#### Schritt 5: Formular-Validator `AbstimmungEingabeValidator`
- **Datei:** `src/main/java/de/mse/votee_backend/abstimmungsverwaltung/application/AbstimmungEingabeValidator.java`
- **Implementierung:**
  - `istGueltigerTitel(String)`:
    - Unicode-Normalisierung `Normalizer.normalize(titel, Normalizer.Form.NFC)` (z. B. für NFD-Umlaute).
    - Entfernen von führenden/nachgestellten ASCII-Leerzeichen (`' '`), während Steuerzeichen (`\n`, `\t`) erhalten bleiben und die Validierung fehlschlagen lassen.
    - Regex: `^(?=.*[\p{L}0-9])[\p{L}0-9 ,.?!:()'"/%€-]{3,100}$`.
  - `istGueltigesDatum(String)`:
    - Regex: `^(0[1-9]|[12][0-9]|3[01])\.(0[1-9]|1[0-2])\.20[0-9]{2} ([01][0-9]|2[0-3]):[0-5][0-9]$` für Format und Jahresbereich 2000–2099.
    - Strikte Kalenderprüfung: `DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT)` fängt Schaltjahre und ungültige Monatslängen ab.
- **Test-Ergebnis (`AbstimmungEingabeValidatorTest`):** 90 Tests ausgeführt, 0 Fehler. ✅

---

### 3. Gesamttests und Linting

1. **Gesamte Testsuite (`./mvnw test`):**
   - 131 Tests insgesamt (4 NutzerIdTest, 3 AbstimmungszeitraumTest, 31 AbstimmungTest, 90 AbstimmungEingabeValidatorTest, 2 MainTest, 1 TemplatesTest).
   - Ergebnis: **131 passed, 0 failures, 0 errors**.
2. **Checkstyle Linting (`./mvnw checkstyle:check`):**
   - Ergebnis: **0 Checkstyle violations**.
3. **Verify (`./mvnw verify`):**
   - Ergebnis: **BUILD SUCCESS**.


---

## Interaktion 2

### Benutzeranfrage
> wenn ich "mvn test" ausführe, schlägt die ausführung fehl. warum?

### Diagnose & Ursache
1. **Verwendete Java-Version:**
   - Standardmäßig ist im Terminal / System **Java 25** (`openjdk version "25.0.4.1"`) aktiv.
2. **Projektanforderung:**
   - Das Projekt verwendet Helidon SE 27.0.1 und Java 27 (`<maven.compiler.release>27</maven.compiler.release>`).
   - Die Helidon-Bibliotheken sowie das Projekt selbst kompilieren gegen Bytecode-Version 71.0 (Java 27).
3. **Fehlerbild bei `mvn test` ohne gesetztes `JAVA_HOME`:**
   - Beim Kompilieren (`javac`): `Fatal error compiling: Fehler: Releaseversion 27 nicht unterstützt` (Java 25 unterstützt Releases nur bis Version 25).
   - Beim Ausführen von vorkompilierten Tests (`surefire`): `... has been compiled by a more recent version of the Java Runtime (class file version 71.0), this version of the Java Runtime only recognizes class file versions up to 69.0`.

### Lösung
- Setzen von `JAVA_HOME` auf eine Java-27-Installation vor dem Maven-Aufruf, z. B.:
  ```bash
  JAVA_HOME=/tmp/jdk27-extracted/usr/lib/jvm/java-latest-openjdk mvn test
  ```
  bzw. dauerhaft per `export JAVA_HOME=...` oder durch Installation des Pakets `java-latest-openjdk-devel` via `sudo dnf install java-latest-openjdk-devel`.


---

## Interaktion 3

### Benutzeranfrage
> hab ich gemacht. die tests schlagen immernoch fehl.

### Ursachenanalyse
1. **Untersuchung des Surefire-Dump-Logs:**
   - In `target/surefire-reports/2026-11-17T06-05-30_657-jvmRun1.dump` stand:
     `java.lang.UnsupportedClassVersionError: de/mse/votee_backend/abstimmungsverwaltung/domain/model/NutzerIdTest has been compiled by a more recent version of the Java Runtime (class file version 71.0), this version of the Java Runtime only recognizes class file versions up to 69.0`.
2. **Entdeckung in `/etc/java/maven.conf`:**
   - Fedora bringt eine systemweite Maven-Konfigurationsdatei mit (`/etc/java/maven.conf`), in der fest `JAVA_HOME=/usr/lib/jvm/jre-25-openjdk` eingetragen ist.
   - Der Starter `/usr/bin/mvn` liest diese Datei ein, wenn `JAVA_HOME` in der aktuellen Shell nicht exportiert ist:
     ```sh
     if [ -z "$JAVA_HOME" ] ; then
       . /etc/java/maven.conf
     fi
     ```
   - Dadurch überschreibt `/usr/bin/mvn` jede Änderung an `alternatives --config java` wieder mit Java 25.
3. **Vergleich mit Maven Wrapper (`./mvnw`):**
   - Der Wrapper `./mvnw` liest `/etc/java/maven.conf` **nicht** ein, sondern nutzt direkt das im Pfad liegende `java` (welches jetzt auf Java 27 zeigt).
   - `./mvnw test` läuft daher sofort fehlerfrei durch (131/131 Tests grün).

### Empfohlene Lösungen für den Benutzer
1. **Nutzung des Projekt-Wrappers (empfohlen lt. README):**
   ```bash
   ./mvnw test
   ```
2. **Dauerhaftes Überschreiben der Fedora-Maven-Konfiguration für `mvn`:**
   Entweder in `~/.mavenrc`:
   ```bash
   echo "export JAVA_HOME=/usr/lib/jvm/java-27" >> ~/.mavenrc
   ```
   oder in der aktuellen Shell:
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-27
   mvn test
   ```


---

## Interaktion 4

### Benutzeranfrage
> wie kann ich das projekt starten

### Erklärung & Befehle
1. **Bauen & Paketieren (JAR + Abhängigkeiten in `target/libs`):**
   ```bash
   ./mvnw package -DskipTests
   ```
2. **Starten der Anwendung mit Java 27:**
   ```bash
   java -jar target/votee_backend.jar
   ```
3. **Erreichbarkeit:**
   - Webserver startet auf Port 8080: `http://localhost:8080/`.
   - Die Startseite wird über Helidon SE und Thymeleaf (`src/main/resources/templates/index.html`) gerendert.
   - Konfiguration über `src/main/resources/application.yaml`.


---

## Interaktion 5

### Benutzeranfrage
> wie kann ich das projekt direkt mit mvnw starten?

### Erklärung & Befehle
- Das Helidon-Parent-POM (`helidon-applications`) bringt eine Vorkonfiguration für das `exec-maven-plugin` mit, die `${mainClass}` (`de.mse.votee_backend.Main`) referenziert.
- Der Server kann daher direkt mit einem einzigen Maven-Befehl kompiliert und gestartet werden:
  ```bash
  ./mvnw compile exec:exec
  ```
- **Vorteil:** Kein vorheriger manueller Paketierungs-Schritt (`package`) und kein separater Aufruf von `java -jar` notwendig. Eventuelle Code-Änderungen werden vor dem Start automatisch neu kompiliert.

---

## Interaktion 6

### Benutzeranfrage
> führe ein review durch. prüfe ob die DDD prinzipien eingehalten wurden.
> schreibe dieses ergebnis in eine markdown datei, falls noch nicht geschehen

### Durchgeführte Analyse & Ergebnis
1. **Analysierte Komponenten:**
   - Domänenmodell (`abstimmungsverwaltung.domain.model`): `Abstimmung`, `AbstimmOption`, `Abstimmungszeitraum`, `NutzerId`.
   - Anwendungsschicht (`abstimmungsverwaltung.application`): `AbstimmungEingabeValidator`.
   - Konzeption & Entwurf aus Übung 3 und 4 (`doc/3_Uebung.md`, `doc/4_Uebung.md`).

2. **Hauptergebnisse des DDD-Reviews:**
   - **Stärken:** 100 % Framework-unabhängiger Domain-Kern; saubere Value Objects (`record`); strikter Schutz von Invarianten via Always-Valid Factory; Kapselung durch defensive Kopien; lose Kopplung zwischen Kontexten via `NutzerId`.
   - **Defizite & Lücken:**
     - **Fehlende Entitäts-Identität:** `Abstimmung` besitzt noch keine `AbstimmungId` und keine ID-basierten `equals()`/`hashCode()`-Methoden.
     - **Anämisches Modell:** Noch keine Methoden für fachliche Lebenszyklus-Zustände (`istAktiv()`, etc.).
     - **Domain Events:** Event `AbstimmungErstellt` noch nicht im Aggregat implementiert.
     - **Primitive Obsession / Validierung:** Titellängenbeschränkung liegt derzeit nur in der Application-Schicht (`AbstimmungEingabeValidator`), nicht als Invariante in der Domäne.

3. **Dokumentation:**
   - Der vollständige Review-Bericht wurde in [doc/DDD_Review.md](../doc/DDD_Review.md) abgelegt.

---

## Interaktion 7

### Benutzeranfrage
> im bezug auf die refactoring check liste: plane Änderungen für den ersten Punkt, das einführen der AbstimmungId
> folge deiner empfehlung für abstimmung.erstelle. stelle eine factory-methode direct am value object bereit

### Umsetzung
1. **Planung:**
   - Detaillierter TDD-Plan im Artefakt `refactoring_plan_abstimmung_id.md` erstellt.
   - Festlegung: `AbstimmungId` als Value Object (`record`), statische Factory `AbstimmungId.neu()` mit UUID, strikte Signaturanpassung von `Abstimmung.erstelle(AbstimmungId id, ...)` und identitätsbasierte Gleichheit (`equals`/`hashCode`).

2. **TDD-Implementierung:**
   - **Schritt 1 (Value Object Test & Code):**
     - Testklasse: [AbstimmungIdTest.java](../src/test/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/AbstimmungIdTest.java) (5 Tests für Wertübernahme, `neu()`, `null` und Blank).
     - Implementierung: [AbstimmungId.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/AbstimmungId.java) mit kompakter Validierung und `neu()`.
   - **Schritt 2 (Aggregat & Entitäts-Semantik):**
     - [AbstimmungTest.java](../src/test/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/AbstimmungTest.java) erweitert:
       - Prüfung von `abstimmung.getId()`.
       - Negativtest: `Abstimmung.erstelle(null, ...)` wirft `IllegalArgumentException`.
       - Neue Testgruppe `EntitaetsGleichheit` (3 Tests für ID-basierte Gleichheit, Ungleichheit bei abweichender ID und Typvergleiche).
     - [Abstimmung.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/Abstimmung.java) angepasst:
       - Feld `private final AbstimmungId id` aufgenommen.
       - Regel 0: Invariantenprüfung `id != null`.
       - Getter `getId()` bereitgestellt.
       - `equals(Object)` und `hashCode()` strikt auf Basis von `id` implementiert.

3. **Verifikation:**
   - `./mvnw test`: 140/140 Tests bestanden.
   - `./mvnw checkstyle:check`: 0 Violations.
   - `./mvnw verify`: BUILD SUCCESS.

---

## Interaktion 8

### Benutzeranfrage
> im bezug auf dein review: implementiere nun das fachliche Verhalten im Aggregat

### Umsetzung
1. **Entwurf des fachlichen Verhaltens (Rich Domain Model):**
   - **Value Object `Abstimmungszeitraum`:**
     - `enthaelt(LocalDateTime zeitpunkt)`: Prüft, ob ein Zeitpunkt innerhalb des Intervalls liegt.
     - `istVorbei(LocalDateTime zeitpunkt)`: Prüft, ob der Zeitraum abgelaufen ist.
     - `istInZukunft(LocalDateTime zeitpunkt)`: Prüft, ob der Zeitraum noch bevorsteht.
     - Null-Prüfung bei allen Methoden.
   - **Aggregate Root `Abstimmung`:**
     - `istLaufend(LocalDateTime zeitpunkt)`: Delegiert an `zeitraum.enthaelt(zeitpunkt)`.
     - `istBeendet(LocalDateTime zeitpunkt)`: Delegiert an `zeitraum.istVorbei(zeitpunkt)`.
     - `istNochNichtGestartet(LocalDateTime zeitpunkt)`: Delegiert an `zeitraum.istInZukunft(zeitpunkt)`.
     - `kannAbgestimmtWerden(LocalDateTime zeitpunkt)`: Fachliche Semantik der Ubiquitous Language (aktiv während Laufzeit).
     - `kannErgebnisEingesehenWerden(LocalDateTime zeitpunkt)`: Fachliche Semantik (erst nach Fristablauf).
     - `hatOption(AbstimmOption)` und `hatOption(String)`: Prüft das Vorhandensein von Antwortoptionen unter Berücksichtigung von Whitespace-Normalisierung und Groß-/Kleinschreibung, ohne dass Aufrufer direkt auf die innere Optionsliste zugreifen müssen (Information Hiding).

2. **TDD-Vorgehen:**
   - **Tests in `AbstimmungszeitraumTest.java`:** 3 neue Tests für Grenzwerte und Invarianten (insgesamt 6 Tests).
   - **Tests in `AbstimmungTest.java`:** Neue Testgruppe `@Nested class FachlichesVerhalten` mit 6 Tests (insgesamt 39 Tests in `AbstimmungTest`).
   - **Implementierung:** In `Abstimmungszeitraum.java` und `Abstimmung.java`.

3. **Verifikation:**
   - `./mvnw test`: 149/149 Tests bestanden (0 Fehler, 0 Failures).
   - `./mvnw checkstyle:check`: 0 Violations.
   - [doc/DDD_Review.md](../doc/DDD_Review.md) aktualisiert.
