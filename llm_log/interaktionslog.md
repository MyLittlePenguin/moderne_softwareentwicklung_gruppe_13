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

---

## Interaktion 9

### Benutzeranfrage
> bitte schreibe das llm_log weiter bei allen anfragen und antworten

### Antwort
- Bestätigt: Alle weiteren Anfragen und Antworten werden fortlaufend im bestehenden Format in `llm_log/interaktionslog.md` protokolliert (beginnend mit Interaktion 9).

---

## Interaktion 10

### Benutzeranfrage
> was muss im backend noch implementiert werden, um abstimmungen zu erstellen. die abstimmungen sollen nur von administrator:innen erstellt werden können. erstelle einen plan.

### Analyse des Ist-Stands & Implementierungsplan
1. **Bereits vorhanden:**
   - Domänenmodell (`abstimmungsverwaltung.domain.model`): `Abstimmung`, `AbstimmungId`, `NutzerId`, `Titel`, `Abstimmungszeitraum`, `AbstimmOption`, `AbstimmungsregelVerletztException`.
   - Eingabevalidierung (`abstimmungsverwaltung.application`): `AbstimmungEingabeValidator` (Titel-Delegation & Datumsprüfung `dd.MM.yyyy HH:mm`).
   - Web- & DB-Basis: Helidon SE (`Main`, `PageService`, `Templates`), H2- und Helidon-DbClient-Dependencies in `pom.xml` sowie `db`-Konfiguration in `application.yaml` (noch ungenutzt).

2. **Noch fehlende Bausteine für das Erstellen von Abstimmungen (nur für Administrator:innen):**
   - **Persistenz-Port & H2-Adapter:** `AbstimmungRepository`-Interface (`speichere`, `findeNachId`, `findeAlle`), DB-Schema (`abstimmungen` & `abstimm_optionen` mit Reihenfolge-Spalte `position`), `H2AbstimmungRepository` mit Helidon `DbClient` (transaktionales Speichern).
   - **Autorisierung & Authentifizierung (Admin / Verwaltungsangestellte):**
     - Port zur Rollenprüfung (z. B. `BerechtigungsPruefung` / `istAdministrator(NutzerId)`) zur losen Kopplung zwischen `abstimmungsverwaltung` und `kontoverwaltung`.
     - Fachliche Exception `KeineBerechtigungException` (HTTP `403`).
     - Schlanke Kontoverwaltung / Auth-Adapter: Benutzer-/Admin-Modell (`Rolle`: `ADMIN`, `BUERGER`) bzw. konfigurierbarer Admin-Account mit sicherem Passwort-Hashing (z. B. PBKDF2) sowie Authentifizierungsmechanismus (HTTP-Login mit HttpOnly-Session-Cookie oder HTTP Basic Auth) inkl. serverseitiger Ableitung der `NutzerId` (nicht aus Client-Eingaben).
   - **Anwendungsservice (`abstimmungsverwaltung.application`):**
     - `AbstimmungService` mit `erstelleAbstimmung(...)` (prüft Admin-Berechtigung, wandelt Eingaben in Domänenobjekte um, ruft `Abstimmung.erstelle(...)` auf, speichert über `AbstimmungRepository`) sowie Lese-Methoden (`findeAlle`, `findeNachId`).
     - Ergänzung einer Parsing-Methode für Datumsstrings in `AbstimmungEingabeValidator`.
   - **Web-Adapter (Helidon SE Routing & Templates/API):**
     - Login-/Logout-Endpunkte bzw. Authentifizierungs-Prüfung.
     - Endpunkte zum Erstellen (`GET /abstimmungen/neu` für Formular, `POST /abstimmungen` für Erstellung – geschützt auf Rolle Admin) mit korrekten Statuscodes (`401`/`302`, `403`, `400`, `303`/`201`) und CSRF-Schutz bei Cookie-Sessions.
     - Anbindung von `GET /` (und optional `GET /abstimmungen/{id}`) an echte Daten aus dem `AbstimmungRepository`.
   - **Composition Root (`Main.java`) & Testkonfiguration:**
     - Verdrahtung von `DbClient`, Schema-Initialisierung, Repositories, Auth-Service, `AbstimmungService` und HTTP-Services per Konstruktor-Injektion; getrennte H2-In-Memory-Konfiguration für Tests.

3. **Schrittweiser TDD-Umsetzungsplan:**
   - **Phase 1:** Ports & Exceptions (`AbstimmungRepository`, `BerechtigungsPruefung`, `KeineBerechtigungException`).
   - **Phase 2:** Persistenz-Adapter (`schema.sql`, `H2AbstimmungRepository` + Integrationstests mit H2 In-Memory).
   - **Phase 3:** Anwendungsservice (`AbstimmungService` + Parsing-Hilfsmethode + Unit-/Integrationstests für Admin, Nicht-Admin und ungültige Eingaben).
   - **Phase 4:** Authentifizierung & Rollenverwaltung (schlanker `kontoverwaltung`-Context / Admin-Bootstrap + Login/Session oder Basic Auth + Tests).
   - **Phase 5:** HTTP-Adapter, Templates/API & Wiring in `Main.java` + `@ServerTest`-Integrationstests (unangemeldet `401`/Redirect, Bürger:in `403`, Admin `303`/`201`, ungültig `400`).

4. **Offene Klärungspunkte an den Benutzer:**
   - Schnittstellenart: Thymeleaf-HTML-Formulare, JSON-REST-API oder beides?
   - Auth-Variante für den ersten Durchstich: Login + Session-Cookie vs. HTTP Basic Auth?
   - Admin-Datenquelle: Fester konfigurierbarer Admin-Account (gemäß `doc/4_Uebung.md`) oder bereits eine `nutzer`-Tabelle in H2?
   - Fachregel: Darf der `beginn` einer neuen Abstimmung in der Vergangenheit liegen?

---

## Interaktion 11

### Benutzeranfrage
> die ui soll über thymeleaf und htmx umgesetzt werden. für die authentifizierung soll eine 2fa später umgesetzt werden. dafür sollen generierte code listen als zweiter faktor dienen. für den anfang kann als code erstmal 00000 akzeptiert werden. die generierung und tatsächliche prüfung kann später umgesetzt werden. der begin einer neuen fachregel darf nie in der vergangenheit liegen. welche vorteile hätte http basic auf für eine anfängliche implementation?

### Antwort & Abwägung
1. **Vorteile von HTTP Basic Auth für den ersten Durchstich:**
   - **Stateless / kein Session-Management:** Kein `SessionStore`, keine Cookie-Verwaltung (`HttpOnly`, `SameSite`, Ablaufzeiten).
   - **Sehr geringer Startaufwand:** Ein kleiner HTTP-Handler/Filter liest den `Authorization: Basic ...`-Header und prüft Benutzername/Passwort; noch kein Login-/Logout-Template nötig.
   - **Einfaches Testen:** In Helidon `@ServerTest` oder per `curl -u` reicht ein einzelner Request-Header statt Login + Cookie-Handling.
   - **Out-of-the-box im Browser:** Browser zeigt bei `401` + `WWW-Authenticate` automatisch ein Login-Fenster an und sendet den Header bei HTMX-Requests derselben Origin mit.

2. **Nachteile im Kontext von Thymeleaf + HTMX + 2FA (Code-Liste / `00000`):**
   - **Kein Platz für den 2. Faktor:** Basic Auth kennt nur Benutzername + Passwort; ein 2FA-Code (`00000` / später Code-Liste) ließe sich nur über unsaubere Workarounds unterbringen und müsste später komplett umgebaut werden.
   - **Kein sauberer Logout & eingeschränkte UX:** Browser cachen Basic-Auth-Daten bis zum Schließen; eigene HTMX-Fehlermeldungen im Login-UI sind nicht möglich.

3. **Empfehlung & Anpassung des Plans:**
   - **Auth-Empfehlung:** Da 2FA (`00000` → Code-Liste) und Thymeleaf + HTMX bereits feststehen, ist ein **schlankes Login-Formular + In-Memory-Session-Cookie (`HttpOnly`, `SameSite=Lax`)** kaum aufwändiger als Basic Auth, bildet aber den 2FA-Port (`ZweiFaktorPruefung`, anfangs fester Code `"00000"`) und Logout sofort sauber ab. Alternativ kann Basic Auth nur dann sinnvoll sein, wenn erst reine Backend-Routen ohne Login-UI gebaut werden sollen (bei sauberer Trennung hinter Ports bleibt der spätere Tausch isoliert auf die HTTP-Schicht).
   - **Auswirkungen auf den Plan:**
     - **Thymeleaf + HTMX:** HTML-Formulare und HTMX-Fragmente für Validierungsfehler / `HX-Redirect` nach Erstellung.
     - **2FA-Port:** Interface `ZweiFaktorPruefung` (Implementierung 1: akzeptiert `"00000"`, später austauschbar gegen Code-Listen-Adapter in H2).
     - **Fachregel „Beginn nicht in der Vergangenheit“:** Prüfung beim Erstellen über injizierte `Clock` (deterministische Tests bleiben stabil, und das Laden älterer Abstimmungen aus H2 bleibt möglich).

---

## Interaktion 12

### Benutzeranfrage
> passe den plan an. berücksichtige für die implementation auch, dass htmx 4 genutzt werden soll

### Angepasster Implementierungsplan (Thymeleaf + htmx 4 + 2FA-Vorbereitung + Vergangenheitsregel)
1. **Architektur-Besonderheiten für htmx 4 im Backend & in Templates:**
   - **Error-Swapping:** htmx 4 swappt standardmäßig alle HTTP-Antworten außer `204` und `304` (inkl. `4xx`/`5xx`). Validierungsfehler beim Erstellen können daher semantisch sauber mit `422 Unprocessable Entity` und dem neu gerenderten HTML-Formular-Fragment zurückgegeben werden; optional gezieltes Routing per `hx-status:422` / `hx-status:403`.
   - **Explizite Attribut-Vererbung:** Attribute (z. B. `hx-headers` für CSRF-Token oder `hx-target`) vererben sich in htmx 4 nur mit dem Modifier `:inherited` (z. B. `hx-headers:inherited`).
   - **`Vary: HX-Request` & kein History-Cache:** Da htmx 4 keinen lokalen History-Cache mehr nutzt (Zurück-Button löst echtes `GET` aus), liefern `GET`-Routen ohne `HX-Request: true` immer die vollständige Seite und setzen `Vary: HX-Request`.
   - **Multi-Target-Updates & Redirects:** Unterstützung für `<hx-partial>` (Server-gesteuerte Teilaktualisierungen) sowie `HX-Redirect` (HTMX) / `303 See Other` (Fallback ohne JS) nach erfolgreicher Erstellung.
   - **Attribute/Events:** Aktuelle htmx-4-Syntax verwenden (`hx-disable` statt `hx-disabled-elt`, `hx-ignore` statt altem `hx-disable`, Event-Syntax `htmx:phase:action` auf `fetch()`-Basis).
   - **Thymeleaf-Erweiterung:** `Templates.java` um `renderFragment(template, fragment, variables)` erweitern; htmx 4 als statische Datei oder eingebundenes Skript bereitstellen.

2. **Angepasster 5-Phasen-TDD-Plan:**
   - **Phase 1 – Domäne, Ports & Exceptions:**
     - Prüfmethode für „Beginn nicht in der Vergangenheit“ (mit explizitem `LocalDateTime jetzt`, sodass `AbstimmungTest` deterministisch bleibt und das Laden alter Abstimmungen aus der DB nicht fehlschlägt).
     - `AbstimmungRepository` (`domain.repository`), `BerechtigungsPruefung` (Admin-Port) und `KeineBerechtigungException`.
   - **Phase 2 – H2-Persistenz:**
     - `schema.sql` (`abstimmungen`, `abstimm_optionen` mit `position`), `DatenbankInitialisierung`, transaktionales `H2AbstimmungRepository` + H2-In-Memory-Integrationstests.
   - **Phase 3 – Anwendungsservice (`abstimmungsverwaltung.application`):**
     - `AbstimmungService` mit injiziertem `AbstimmungRepository`, `BerechtigungsPruefung` und `Clock`: prüft Admin-Rolle → prüft `beginn >= jetzt` → baut `Abstimmung` → speichert.
     - `parseDatum` in `AbstimmungEingabeValidator` (ggf. inkl. HTML-`datetime-local`-Unterstützung) + umfassende Unit-Tests.
   - **Phase 4 – Kontoverwaltung, 2FA-Port (`00000`) & Session-Auth:**
     - Schlanker Kontext `kontoverwaltung`: `Rolle` (`ADMIN`, `BUERGER`), Passwort-Hashing (`PBKDF2WithHmacSHA256`), konfigurierbarer Admin-Account + Adapter für `BerechtigungsPruefung`.
     - Port `ZweiFaktorPruefung` (`pruefeCode(NutzerId, String)`) mit Start-Implementierung `FesterCodeZweiFaktorPruefung` (akzeptiert `"00000"`), später austauschbar durch Code-Listen in H2.
     - Serverseitiger `SessionStore` (`HttpOnly`, `SameSite=Lax`, `SecureRandom`-IDs) + CSRF-Token pro Session.
   - **Phase 5 – Thymeleaf + htmx-4-HTTP-Adapter & Wiring (`Main.java`):**
     - `Templates.renderFragment(...)`, Login-/Logout-Routen (`GET/POST /login`, `POST /logout`), Abstimmung-Routen (`GET /`, `GET /abstimmungen/neu`, `POST /abstimmungen`) mit `HX-Request`, `Vary: HX-Request`, `422` bei Validierungsfehlern, `401`/`HX-Redirect: /login` unangemeldet, `403` für Nicht-Admins, `HX-Redirect` / `303` bei Erfolg, dynamische Optionsfelder via htmx 4.
     - Verdrahtung in `Main.setup(...)` + `@ServerTest`-Integrationstests (inkl. HTMX-Header-Tests).

---

## Interaktion 13

### Benutzeranfrage
> passe den plan noch so an, dass der einmalcode immer exakt 5 stellen haben soll. dann beginne mit phase 1

### Plan-Anpassung (Phase 4)
- **Einmalcode (2FA):** Neues Value Object `EinmalCode` im Bounded Context `kontoverwaltung`, das strikt genau 5 Ziffern (`^\d{5}$`) erzwingt. Der Start-Stub `FesterCodeZweiFaktorPruefung` prüft gegen `new EinmalCode("00000")`; spätere Code-Listen erzeugen und prüfen ebenfalls `EinmalCode`-Instanzen. Im Login-Formular entsprechend `inputmode="numeric"`, `pattern="\d{5}"`, `minlength="5"`, `maxlength="5"`.

### Umsetzung Phase 1 (TDD: Domänenregel, Ports & Exceptions)
1. **Rote Phase (Tests zuerst):**
   - [AbstimmungszeitraumTest.java](../src/test/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/AbstimmungszeitraumTest.java): Test `pruefeBeginnNichtInVergangenheit()` ergänzt (Zukunft und exakt `jetzt` erlaubt; `1 Sekunde`/`1 Minute` in Vergangenheit wirft `AbstimmungsregelVerletztException`; `null` wirft exakt `IllegalArgumentException`).
   - [KeineBerechtigungExceptionTest.java](../src/test/java/de/mse/votee_backend/abstimmungsverwaltung/application/KeineBerechtigungExceptionTest.java): Test für Meldungsübernahme und klare Trennung von `IllegalArgumentException` erstellt.
   - `./mvnw -o -Daether.syncContext.named.factory=noop test` lief erwartungsgemäß rot (Kompilierfehler vor Implementierung).

2. **Grüne Phase (Implementierung):**
   - [Abstimmungszeitraum.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/domain/model/Abstimmungszeitraum.java): Methode `pruefeBeginnNichtInVergangenheit(LocalDateTime jetzt)` ergänzt.
   - [AbstimmungRepository.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/domain/repository/AbstimmungRepository.java): Repository-Port mit `speichere`, `findeNachId`, `findeAlle` angelegt.
   - [BerechtigungsPruefung.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/application/BerechtigungsPruefung.java): Rollenprüfungs-Port `istAdministrator(NutzerId)` angelegt.
   - [KeineBerechtigungException.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/application/KeineBerechtigungException.java): Fachliche Exception für fehlende Admin-Berechtigung angelegt.

3. **Verifikation:**
   - `./mvnw -o -Daether.syncContext.named.factory=noop checkstyle:check verify`: **192/192 Tests bestanden**, **0 Checkstyle violations**, **BUILD SUCCESS**.

---

## Interaktion 14

### Benutzeranfrage
> ja

### Umsetzung Phase 2 (TDD: H2-Persistenz)
1. **Rote Phase (Tests zuerst):**
   - [H2AbstimmungRepositoryTest.java](../src/test/java/de/mse/votee_backend/abstimmungsverwaltung/infrastructure/H2AbstimmungRepositoryTest.java) mit 7 Integrationstests gegen eine isolierte H2-In-Memory-Datenbank erstellt:
     - Speichern & Laden aller Felder inkl. exakter Optionsreihenfolge.
     - Leere Beschreibung sowie Umlaute/Sonderzeichen.
     - Unbekannte `AbstimmungId` liefert `Optional.empty()`.
     - `findeAlle()` bei leerer DB und bei mehreren Abstimmungen (sortiert nach `beginn ASC`, ohne N+1-Abfragen).
     - Laden gespeicherter Abstimmungen mit `beginn` in der Vergangenheit.
     - Idempotente Schema-Initialisierung.
     - Transaktions-Rollback bei doppelter `AbstimmungId`.
   - `./mvnw -o -Daether.syncContext.named.factory=noop test` lief erwartungsgemäß rot.

2. **Grüne Phase (Implementierung):**
   - [schema.sql](../src/main/resources/schema.sql): Tabellen `abstimmungen` und `abstimm_optionen` (mit `position`-Spalte und Foreign Key `ON DELETE CASCADE`) definiert.
   - [DatenbankInitialisierung.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/infrastructure/DatenbankInitialisierung.java): Liest `/schema.sql` aus dem Classpath und führt alle DDL-Anweisungen idempotent über den Helidon `DbClient` aus.
   - [H2AbstimmungRepository.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/infrastructure/H2AbstimmungRepository.java): Implementiert `AbstimmungRepository` mit transaktionalem `speichere(...)` (`DbTransaction` mit `commit`/`rollback`), `findeNachId(...)` und `findeAlle()` (Batch-Laden der Optionen ohne N+1-Queries).

3. **Verifikation:**
   - `./mvnw -o -Daether.syncContext.named.factory=noop checkstyle:check verify`: **199/199 Tests bestanden**, **0 Checkstyle violations**, **BUILD SUCCESS**.

---

## Interaktion 15

### Benutzeranfrage
> ja

### Umsetzung Phase 3 (TDD: Anwendungsservice & Datumsparser)
1. **Rote Phase (Tests zuerst):**
   - [AbstimmungEingabeValidatorTest.java](../src/test/java/de/mse/votee_backend/abstimmungsverwaltung/application/AbstimmungEingabeValidatorTest.java): Neue Testgruppe `@Nested class ParseDatum` (8 Testausführungen für gültige Daten inkl. Schaltjahr sowie ungültige Formate/Daten).
   - [AbstimmungServiceTest.java](../src/test/java/de/mse/votee_backend/abstimmungsverwaltung/application/AbstimmungServiceTest.java): 11 Tests gegen H2-In-Memory-DB (`H2AbstimmungRepository`, gemäß `doc/4_Uebung.md`) und fixer `Clock`:
     - Admin erstellt gültige Abstimmung (inkl. Grenzfall `beginn == jetzt`) → gespeichert & per `findeNachId`/`findeAlle` ladbar.
     - Nicht-Admin (`BUERGER`) und `null`-Akteur → `KeineBerechtigungException`, nichts gespeichert.
     - Autorisierung wird vor der Eingabevalidierung geprüft.
     - Beginn in der Vergangenheit, ungültiger Titel, ungültige Optionen (`null`, zu wenige, leer, Duplikat) → `AbstimmungsregelVerletztException`, nichts gespeichert.
     - Deterministisches Verhalten der injizierten `Clock` bei Zeitverschiebung.
   - `./mvnw -o -Daether.syncContext.named.factory=noop test` lief erwartungsgemäß rot.

2. **Grüne Phase (Implementierung):**
   - [AbstimmungEingabeValidator.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/application/AbstimmungEingabeValidator.java): `parseDatum(String)` ergänzt; `istGueltigesDatum(String)` delegiert nun ohne Duplizierung an `parseDatum`.
   - [ErstelleAbstimmungBefehl.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/application/ErstelleAbstimmungBefehl.java): Unveränderlicher Command-Record für die Anwendungsfall-Eingaben.
   - [AbstimmungService.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/application/AbstimmungService.java): Anwendungsservice mit Konstruktor-Injektion (`AbstimmungRepository`, `BerechtigungsPruefung`, `Clock`), `erstelleAbstimmung(...)`, `findeAlle()` und `findeNachId(...)`.

3. **Verifikation:**
   - `./mvnw -o -Daether.syncContext.named.factory=noop checkstyle:check verify`: **218/218 Tests bestanden**, **0 Checkstyle violations**, **BUILD SUCCESS**.
