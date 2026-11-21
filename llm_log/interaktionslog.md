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

---

## Interaktion 16

### Benutzeranfrage
> ja

### Umsetzung Phase 4 (TDD: Kontoverwaltung, 5-stelliger 2FA-Einmalcode & Session-Store)
1. **Rote Phase (Tests zuerst):**
   - [EinmalCodeTest.java](../src/test/java/de/mse/votee_backend/kontoverwaltung/domain/model/EinmalCodeTest.java): 14 Testfälle für exakt 5 ASCII-Ziffern (u. a. Ablehnung von 4/6 Stellen, Buchstaben, Leerzeichen, nicht-ASCII-Ziffern).
   - [Pbkdf2PasswortHasherTest.java](../src/test/java/de/mse/votee_backend/kontoverwaltung/infrastructure/Pbkdf2PasswortHasherTest.java): Gesalzene Hashes, Verifikation, sichere Ablehnung ungültiger Eingaben/Hashes.
   - [FesterCodeZweiFaktorPruefungTest.java](../src/test/java/de/mse/votee_backend/kontoverwaltung/infrastructure/FesterCodeZweiFaktorPruefungTest.java): Akzeptiert exakt `00000`, lehnt andere 5-stellige Codes und `null` ab.
   - [AuthentifizierungsServiceTest.java](../src/test/java/de/mse/votee_backend/kontoverwaltung/application/AuthentifizierungsServiceTest.java): Admin-/Bürger-Login mit Passwort + 5-stelligem Einmalcode, Ablehnung falscher Passwörter/Codes, sowie Prüfung von `KontoBerechtigungsAdapter` gegen `BerechtigungsPruefung`.
   - [SessionStoreTest.java](../src/test/java/de/mse/votee_backend/kontoverwaltung/application/SessionStoreTest.java): Session-Erzeugung mit eindeutiger ID & CSRF-Token, Zeitablauf über injizierte `Clock`, Logout (`entferneSession`) und CSRF-Token-Prüfung.
   - `./mvnw -o -Daether.syncContext.named.factory=noop test` lief erwartungsgemäß rot.

2. **Grüne Phase (Implementierung im Bounded Context `kontoverwaltung`):**
   - **Domäne (`kontoverwaltung.domain.model`):** `EinmalCode` (`^[0-9]{5}$`), `Rolle` (`ADMIN`, `BUERGER`), `NutzerKonto`.
   - **Anwendung (`kontoverwaltung.application`):** Ports `ZweiFaktorPruefung`, `PasswortHasher`, `NutzerKontoRepository`; Services `AuthentifizierungsService`, `Session`, `SessionStore` (mit `SecureRandom` und zeitkonstanter CSRF-Prüfung via `MessageDigest.isEqual`).
   - **Infrastruktur (`kontoverwaltung.infrastructure`):** `Pbkdf2PasswortHasher` (`PBKDF2WithHmacSHA256`, 120.000 Iterationen, 16-Byte-Salt), `FesterCodeZweiFaktorPruefung` (`00000`-Stub), `KonfigurierteNutzerKonten`, sowie `KontoBerechtigungsAdapter` (Brücke zum Port `BerechtigungsPruefung` aus `abstimmungsverwaltung`).

3. **Verifikation:**
   - `./mvnw -o -Daether.syncContext.named.factory=noop checkstyle:check verify`: **244/244 Tests bestanden**, **0 Checkstyle violations**, **BUILD SUCCESS**.

---

## Interaktion 17

### Benutzeranfrage
> ja

### Umsetzung Phase 5 (TDD: Thymeleaf + htmx-4-HTTP-Adapter, Templates & Wiring)
1. **Rote Phase (Tests zuerst):**
   - [TemplatesTest.java](../src/test/java/de/mse/votee_backend/TemplatesTest.java): Test für fragmentweises Rendern (`renderFragment`) ergänzt.
   - [AbstimmungErstellenHttpTest.java](../src/test/java/de/mse/votee_backend/AbstimmungErstellenHttpTest.java): 8 Helidon-`@ServerTest`-Integrationstests gegen isolierte H2-In-Memory-DB und fixe `Clock`:
     - Admin-Login mit 5-stelligem Code `00000` setzt `HttpOnly; SameSite=Lax`-Session-Cookie und sendet `HX-Redirect: /` (`204 No Content`).
     - Ungültiger/falscher Einmalcode liefert `422 Unprocessable Content` als HTMX-Fragment mit `Vary: HX-Request`.
     - Unangemeldete Zugriffe auf `/abstimmungen/neu` und `POST /abstimmungen` liefern `401 Unauthorized` (+ `HX-Redirect: /login` bei HTMX).
     - Bürger:innen erhalten `403 Forbidden`.
     - Admin ohne gültiges CSRF-Token erhält `403 Forbidden`.
     - Admin erstellt Abstimmung via htmx 4 (`GET /abstimmungen/neu`, `GET /abstimmungen/neu/option-feld`, `POST /abstimmungen` mit `X-CSRF-Token`) → `HX-Redirect: /`, neue Abstimmung erscheint auf `/`.
     - Admin mit `beginn` in der Vergangenheit erhält `422 Unprocessable Content` als Formular-Fragment inkl. erhaltener Eingaben (nichts gespeichert).
     - Logout (`POST /logout`) invalidiert die Session.
   - `./mvnw -o -Daether.syncContext.named.factory=noop test` lief erwartungsgemäß rot.

2. **Grüne Phase (Implementierung):**
   - [Templates.java](../src/main/java/de/mse/votee_backend/Templates.java): `renderFragment(template, fragment, variables)` ergänzt.
   - [HtmxHelfer.java](../src/main/java/de/mse/votee_backend/HtmxHelfer.java): Hilfsmethoden für `HX-Request`, `HX-Redirect`, `Vary: HX-Request`, `X-CSRF-Token` / `_csrf`, `HttpOnly; SameSite=Lax`-Cookies und `csrfHeadersJson`.
   - [AuthHttpService.java](../src/main/java/de/mse/votee_backend/AuthHttpService.java): Routen `GET /login`, `POST /login` (`422`-Fragment bei Fehler), `POST /logout` (CSRF-geschützt).
   - [AbstimmungHttpService.java](../src/main/java/de/mse/votee_backend/AbstimmungHttpService.java): Admin-geschützte Routen `GET /abstimmungen/neu`, `GET /abstimmungen/neu/option-feld`, `POST /abstimmungen` (serverseitige `NutzerId` aus Session, `422`-Formular-Fragment bei Fachregelverletzung).
   - [PageService.java](../src/main/java/de/mse/votee_backend/PageService.java): Bindet `GET /` an `AbstimmungService.findeAlle()` und aktive `Session` an.
   - [Main.java](../src/main/java/de/mse/votee_backend/Main.java): Composition Root verdrahtet `DbClient`, `DatenbankInitialisierung`, `H2AbstimmungRepository`, `kontoverwaltung`-Services, `AbstimmungService` und alle HTTP-Services per Konstruktor-Injektion (inkl. `setup(..., Clock)` für Tests).
   - **Templates (Thymeleaf + htmx 4):** [index.html](../src/main/resources/templates/index.html), [login.html](../src/main/resources/templates/login.html), [abstimmung-neu.html](../src/main/resources/templates/abstimmung-neu.html) mit htmx 4 (`hx-headers:inherited`, `hx-disable`, dynamische Optionen, `422`-Auto-Swap, 5-stelliges `einmalCode`-Feld). *(Hinweis aus den Tests: Attributnamen mit Doppelpunkt wie `hx-headers:inherited` müssen in Thymeleaf-`th:attr` einfach gequotet werden: `th:attr="'hx-headers:inherited'=${csrfHeadersJson}"`.)*
   - **Konfiguration:** [application.yaml](../src/main/resources/application.yaml) um `votee.auth`-Abschnitt erweitert; [src/test/resources/application.yaml](../src/test/resources/application.yaml) für isolierte H2-In-Memory-Tests angelegt.

3. **Verifikation:**
   - `./mvnw -o -Daether.syncContext.named.factory=noop checkstyle:check verify`: **253/253 Tests bestanden**, **0 Checkstyle violations**, **BUILD SUCCESS**.

---

## Interaktion 18

### Benutzeranfrage
> beachte das llm_log. bitte ergänzen mit den interaktionen.
>
> die ui dieser applikation ist noch ungestyled. erstelle mithilfe von css einen vorschlag für eine einfache und seriöse oberfläche.

### Gestaltungsentwurf & Umsetzung (Schlichtes, seriöses CSS für Votee)
1. **Gestaltungskonzept:**
   - **Farb- und Kontrastkonzept (CSS Custom Properties in `:root`):** Ruhige, behördennahe Farbpalette mit gedecktem Dunkelblau (`#1b3a5c`) als Primärfarbe, neutralem Hellgrau (`#f4f6f9`) als Seitenhintergrund, klaren weißen Kartenflächen (`#ffffff`), feinen Rahmen (`#d5dde5`) sowie deutlicher Fehlerhervorhebung (`.fehler` mit roter linker Akzentlinie).
   - **Typografie & Datenschutz:** System-Schriftarten-Stack ohne externe Font-CDNs (schnell und datenschutzfreundlich), Tabellenziffern (`tabular-nums`) für Zeiträume sowie Monospace-Schrift mit erhöhtem Zeichenabstand für den 5-stelligen 2FA-Einmalcode.
   - **Struktur & Barrierefreiheit:** Einheitliche Kopfzeile mit oberer Akzentlinie, zentriertes Inhaltslayout (`880px` Übersicht, `660px` Formular, `460px` Anmeldung), sichtbare `:focus-visible`-Rahmen, visuelles Feedback für `hx-disable` (`button:disabled`) und Responsive-Anpassungen unter `640px`.

2. **Änderungen im Projekt:**
   - [style.css](../src/main/resources/static/css/style.css): Vollständiges Stylesheet ohne externe CSS-Frameworks oder Build-Schritt erstellt.
   - [StatischeDateienService.java](../src/main/java/de/mse/votee_backend/StatischeDateienService.java): Schlanker Helidon-`HttpService`, der `/css/style.css` aus dem Classpath mit `text/css; charset=UTF-8` ausliefert.
   - [Main.java](../src/main/java/de/mse/votee_backend/Main.java): `StatischeDateienService` im Routing registriert.
   - **Templates:** [index.html](../src/main/resources/templates/index.html), [login.html](../src/main/resources/templates/login.html) und [abstimmung-neu.html](../src/main/resources/templates/abstimmung-neu.html) mit `/css/style.css` verknüpft und semantisch (einheitliche Kopf-/Fußzeile, Karten, Formulargruppen, Leerzustand) unter Beibehaltung aller htmx-4- und CSRF-Attribute strukturiert.
   - [MainTest.java](../src/test/java/de/mse/votee_backend/MainTest.java): Prüfung auf Stylesheet-Einbindung in `/` und neuen Test `stylesheetIsServed()` für `GET /css/style.css` ergänzt.

3. **Verifikation:**
   - `./mvnw -o -Daether.syncContext.named.factory=noop checkstyle:check verify`: **254/254 Tests bestanden**, **0 Checkstyle violations**, **BUILD SUCCESS**.

---

## Interaktion 19

### Benutzeranfrage
> sieht gut aus. die antwortoptionen möchte ich untereinander angeordnet bekommen. die sollen nicht in form von buttons, sondern mit radio-buttons umgesetzt sein. bitte nicht gleich committen. ich möchte das erst ansehen

### Umsetzung
1. **Template ([index.html](../src/main/resources/templates/index.html)):**
   - Antwortoptionen als semantisches `<fieldset class="optionen-bereich">` mit `<legend class="meta-label">Antwortoptionen</legend>` strukturiert.
   - Jede Option wird als `<label class="option-auswahl">` mit nativem `<input type="radio" th:name="'option-' + ${abstimmung.id}" th:value="${option}">` dargestellt, sodass die Auswahl eindeutig pro Abstimmung gruppiert ist.
2. **Styling ([style.css](../src/main/resources/static/css/style.css)):**
   - `.optionen-liste` auf vertikale Anordnung (`flex-direction: column`) umgestellt.
   - Button-/Pill-Rahmen entfernt; stattdessen ruhige Radio-Zeilen (`.option-auswahl`) mit `accent-color: var(--farbe-primar)` und dezenter Hover-Hervorhebung.
3. **Verifikation & Status:**
   - `./mvnw -o -Daether.syncContext.named.factory=noop checkstyle:check test`: **254/254 Tests bestanden**, **0 Checkstyle violations**.
   - Änderungen wunschgemäß noch nicht committet (zur vorherigen Ansicht durch den Benutzer).

---

## Interaktion 20

### Benutzeranfrage
> du darfst die änderungen committen.

### Umsetzung
- Die Änderungen aus Interaktion 19 ([index.html](../src/main/resources/templates/index.html), [style.css](../src/main/resources/static/css/style.css)) sowie die Aktualisierung des Interaktionslogs wurden in einem eigenen Commit auf Branch `abgabe_05_4` gesichert (`Übung 5.4: Antwortoptionen vertikal als Radio-Buttons dargestellt`).

---

## Interaktion 21

### Benutzeranfrage
> momentan kann noch nicht abgestimmt werden. disable entsprechend die radio-buttons.
>
> die datums-felder im formular zum anlegen neuer abstimmungen sollen um date-time-picker ergänzt werden, damit man bei bedarf auch darüber ein datum auswählen kann

### Umsetzung
1. **Deaktivierte Radio-Buttons auf der Startseite:**
   - [index.html](../src/main/resources/templates/index.html): Radio-Buttons der Antwortoptionen mit `disabled` und Klasse `.option-auswahl-inaktiv` versehen.
   - [style.css](../src/main/resources/static/css/style.css): Deaktivierte Auswahlzeilen ruhig dargestellt (`cursor: default`, kein irreführender Hover-Effekt, dezente Text- und Kontrollfarben).
2. **Ergänzende Date-Time-Picker im Erstellungsformular:**
   - [abstimmung-neu.html](../src/main/resources/templates/abstimmung-neu.html): Neben/unter den bestehenden Textfeldern (`TT.MM.JJJJ HH:MM`) jeweils ein nativer `<input type="datetime-local">` (`beginn-picker`, `ende-picker` mit `data-datum-ziel`, ohne `name`-Attribut) ergänzt und `/js/datum-picker.js` eingebunden.
   - [datum-picker.js](../src/main/resources/static/js/datum-picker.js): Schlankes, ereignisdelegiertes Skript zur bidirektionalen Synchronisierung zwischen `datetime-local` (`YYYY-MM-DDTHH:MM`) und dem deutschen Eingabeformat (`TT.MM.JJJJ HH:MM`) – inklusive automatischer Neusynchronisierung nach htmx-4-Swaps (`422`-Validierungsfehler).
   - [StatischeDateienService.java](../src/main/java/de/mse/votee_backend/StatischeDateienService.java): Route `GET /js/datum-picker.js` (`text/javascript; charset=UTF-8`) ergänzt.
   - [style.css](../src/main/resources/static/css/style.css): Passende, unaufdringliche Gestaltung für `.datum-picker-zeile`, `.datum-picker-label` und `.datum-picker`.
3. **Tests & Verifikation:**
   - [MainTest.java](../src/test/java/de/mse/votee_backend/MainTest.java): Test `datumPickerScriptIsServed()` für `GET /js/datum-picker.js` ergänzt.
   - [AbstimmungErstellenHttpTest.java](../src/test/java/de/mse/votee_backend/AbstimmungErstellenHttpTest.java): Prüfungen für `datetime-local`-Picker, `/js/datum-picker.js` und deaktivierte Radio-Buttons ergänzt.
   - `./mvnw -o -Daether.syncContext.named.factory=noop checkstyle:check verify`: **255/255 Tests bestanden**, **0 Checkstyle violations**, **BUILD SUCCESS**.

---

## Interaktion 22

### Benutzeranfrage
> du kannst die separaten textfelder für die datums- und zeiteingabe entfernen.

### Umsetzung
1. **Formular ([abstimmung-neu.html](../src/main/resources/templates/abstimmung-neu.html)):**
   - Separate Textfelder entfernt; `beginn` und `ende` sind nun direkt `<input type="datetime-local">` (`AbstimmungHttpService.parseZeitpunkt` unterstützt das Format `yyyy-MM-dd'T'HH:mm` bereits nativ).
   - Einbindung von `/js/datum-picker.js` entfernt.
2. **Bereinigung:**
   - Nicht mehr benötigtes Skript `src/main/resources/static/js/datum-picker.js` gelöscht.
   - [StatischeDateienService.java](../src/main/java/de/mse/votee_backend/StatischeDateienService.java): Route `/js/datum-picker.js` entfernt.
   - [style.css](../src/main/resources/static/css/style.css): Nicht mehr benötigte Klassen `.datum-picker-*` entfernt und `font-variant-numeric: tabular-nums` direkt für `input[type="datetime-local"]` belassen.
3. **Tests & Verifikation:**
   - [MainTest.java](../src/test/java/de/mse/votee_backend/MainTest.java): Obsoleten Test für `/js/datum-picker.js` entfernt.
   - [AbstimmungErstellenHttpTest.java](../src/test/java/de/mse/votee_backend/AbstimmungErstellenHttpTest.java): Prüfung auf direkte `datetime-local`-Felder (`id="beginn"`, `id="ende"`) sowie End-to-End-Erstellung mit `datetime-local`-Werten (`2027-03-01T09:00` / `2027-03-15T18:00`) und Darstellung im deutschen Format auf `/` angepasst.
   - `./mvnw -o -Daether.syncContext.named.factory=noop clean checkstyle:check verify`: **254/254 Tests bestanden**, **0 Checkstyle violations**, **BUILD SUCCESS**.

---

## Interaktion 23

### Benutzeranfrage
> ändere die sortierung der anzeige nach dem end-datum der abstimmungen

### Umsetzung
1. **Sortierung ([H2AbstimmungRepository.java](../src/main/java/de/mse/votee_backend/abstimmungsverwaltung/infrastructure/H2AbstimmungRepository.java)):**
   - `SELECT_ALLE` von `ORDER BY beginn ASC, id ASC` auf `ORDER BY ende ASC, id ASC` umgestellt, sodass Abstimmungen aufsteigend nach Enddatum (nächste Frist zuerst) geladen und auf der Startseite angezeigt werden.
2. **Tests & Verifikation:**
   - [H2AbstimmungRepositoryTest.java](../src/test/java/de/mse/votee_backend/abstimmungsverwaltung/infrastructure/H2AbstimmungRepositoryTest.java): Testfall in `findeAlleLeerUndMitMehrerenAbstimmungen()` mit gegensätzlichem Beginn/Ende angepasst, sodass die Sortierung explizit nach Enddatum geprüft wird.
   - [AbstimmungServiceTest.java](../src/test/java/de/mse/votee_backend/abstimmungsverwaltung/application/AbstimmungServiceTest.java): Erwartete Reihenfolge in `findeAlleUndFindeNachId()` auf Sortierung nach Enddatum aktualisiert.
   - [AbstimmungErstellenHttpTest.java](../src/test/java/de/mse/votee_backend/AbstimmungErstellenHttpTest.java): Neuen HTTP-Integrationstest `startseiteSortiertAbstimmungenNachEnddatum()` ergänzt.
   - `./mvnw -o -Daether.syncContext.named.factory=noop checkstyle:check verify`: **255/255 Tests bestanden**, **0 Checkstyle violations**, **BUILD SUCCESS**.

---

## Interaktion 24

### Benutzeranfrage
> die README.md ist veraltet. aktualisiere die

### Umsetzung
- [README.md](../README.md) auf den aktuellen Projektstand aktualisiert:
  - Technologie-Stack um htmx 4, eigenes CSS (`StatischeDateienService`) und die aktive H2-Persistenz (`schema.sql`, `DatenbankInitialisierung`, `H2AbstimmungRepository`) ergänzt.
  - Aktuellen Funktionsumfang (Übersicht mit Enddatum-Sortierung und deaktivierten Radio-Buttons, Login/Logout mit 5-stelligem 2FA-Stub `00000`, Session/CSRF, Admin-Abstimmungserstellung mit `datetime-local`) sowie die DDD-/Hexagonal-Architekturübersicht dokumentiert.
  - Startbefehle (`./mvnw compile exec:exec` und JAR-Start), dateibasierte H2-Konfiguration (`./data/votee`) und Entwicklungs-Standardkonten aus `application.yaml` ergänzt.
  - Testübersicht (255 Tests über Domäne, Persistenz, Kontoverwaltung und `@ServerTest`-HTTP-Integration) sowie Verweise auf `doc/` und `llm_log/interaktionslog.md` aktualisiert.


## Interaktion 25 (Übung 5, Metrik-Setup vom 18.11.2026, aus Branch abgabe_05 übernommen)

### Benutzeranfrage
> create scripts to run sonarcube locally and check this project
> i don't want to use a token for sonarqube
> hast du eine lokale alternative für sonarcube, mit der ich code metriken für dieses projekt generieren kann?
> ja erstelle ein auswertungsskript mit JaCoCo und PMD/CPD

### Umsetzung
1. **Ausgangslage & Entscheidung für eine rein lokale Lösung ohne Server/Token:**
   - Zunächst wurden Start-/Prüfskripte für einen lokalen SonarQube-Community-Container entworfen.
   - Da SonarQube Community Build (`26.11`) beim Scanner-Lauf für `/api/plugins/installed` zwingend eine Authentifizierung bzw. ein Token voraussetzt, wurde auf Wunsch nach einer server- und tokenlosen lokalen Alternative auf **JaCoCo + PMD/CPD + Checkstyle + lokales Auswertungsskript** umgestellt.

2. **Maven-Konfiguration ([pom.xml](../pom.xml) & [pmd-ruleset.xml](../pmd-ruleset.xml)):**
   - **JaCoCo (`jacoco-maven-plugin` `0.8.16`):** Unterstützt Java 27 offiziell (`ASM 9.11.1`). Konfiguriert mit `prepare-agent` und `report` (Phase `test`), sodass bei jedem Testlauf `target/site/jacoco/jacoco.xml`, `jacoco.csv` und der HTML-Report erzeugt werden.
   - **PMD & CPD (`maven-pmd-plugin` `3.28.0` mit PMD `7.29.0`):** Unterstützt Java 27 (`<targetJdk>27</targetJdk>`). Eigenes Regelsatz-Profil in [pmd-ruleset.xml](../pmd-ruleset.xml) für:
     - Best Practices, Error Prone, Multithreading, Performance
     - Design- & Komplexitätsmetriken (`CyclomaticComplexity`, `CognitiveComplexity`, `ExcessiveParameterList`, `TooManyMethods`)
     - Copy-Paste-Detector (`cpd` ab 30 Tokens)

3. **Lokales Auswertungsskript ([scripts/check-metrics.sh](../scripts/check-metrics.sh)):**
   - Führt `./mvnw test jacoco:report pmd:pmd pmd:cpd checkstyle:check` aus (oder wertet mit `--report-only` bestehende Reports direkt aus).
   - Analysiert die Java-Quelldateien unter `src/main/java/` sowie die XML-Reports (`jacoco.xml`, `pmd.xml`, `cpd.xml`, `checkstyle-result.xml`) und generiert eine Markdown-Gesamtübersicht im Terminal sowie unter `target/metrics-report.md`:
     - **Aufgabe 1 (Gesamt- & Dateimetriken):** NCLOC (346), Kommentarzeilen (163), Kommentaranteil (32,0 %), Klassen (11), Methoden (46), Cyclomatic Complexity (87 gesamt, Ø 1,89 pro Methode), max. Parameteranzahl pro Datei.
     - **Aufgabe 2 (Code Coverage):** Line Coverage (93,7 %), Branch Coverage (95,1 %), Method Coverage (95,7 %), inklusive exakter Angabe nicht abgedeckter Zeilen und teilweise abgedeckter Branches pro Datei.
     - **Aufgabe 3 (Technical Debt & Regelverletzungen):** 8 PMD-Meldungen, 0 CPD-Duplikate, 0 Checkstyle-Verstöße.

4. **Verifikation:**
   - `./scripts/check-metrics.sh`: 190/190 Tests bestanden, alle Berichte erfolgreich generiert.
