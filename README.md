# moderne_softwareentwicklung_gruppe_13 – Votee

[![Java CI with Maven](https://github.com/MyLittlePenguin/moderne_softwareentwicklung_gruppe_13/actions/workflows/maven.yml/badge.svg)](https://github.com/MyLittlePenguin/moderne_softwareentwicklung_gruppe_13/actions/workflows/maven.yml)

**Votee** ist das Backend und die serverseitig gerenderte Weboberfläche für ein digitales Abstimmungssystem. Das Projekt wird mit **Java 27** und **[Helidon SE](https://helidon.io) 27** nach Prinzipien des Domain-Driven Design (DDD) und einer hexagonalen Architektur entwickelt.

## Technologie-Stack

- **Webserver & Routing:** Helidon SE 27 (`Main`, `PageService`, `AuthHttpService`, `AbstimmungHttpService`, `StatischeDateienService`)
- **UI:** [Thymeleaf](https://www.thymeleaf.org) (`src/main/resources/templates/`) + **htmx 4** + eigenes CSS-Stylesheet (`src/main/resources/static/css/style.css`)
- **Datenbank:** H2 über den Helidon DB Client (JDBC + HikariCP), Schema in `src/main/resources/schema.sql` (automatische, idempotente Initialisierung beim Start über `DatenbankInitialisierung`)
- **Qualitätssicherung:** JUnit 5, Helidon `@ServerTest`, Checkstyle (`checkstyle.xml`)

## Funktionsumfang (aktueller Stand)

- **Abstimmungsübersicht (`GET /`):** Zeigt alle gespeicherten Abstimmungen aufsteigend nach Enddatum (nächste Frist zuerst) an. Die Antwortoptionen werden untereinander als Radio-Buttons dargestellt (aktuell deaktiviert, da die Stimmabgabe noch nicht implementiert ist).
- **An- und Abmeldung (`GET/POST /login`, `POST /logout`):** Anmeldung mit Benutzername, Passwort (`PBKDF2WithHmacSHA256`) und 5-stelligem 2FA-Einmalcode (aktuell fester Entwicklungs-Stub `00000`, vorbereitet für spätere Code-Listen). Serverseitige Sessions mit `HttpOnly; SameSite=Lax`-Cookie und CSRF-Schutz (`X-CSRF-Token` / `_csrf`).
- **Abstimmungen erstellen (`GET /abstimmungen/neu`, `POST /abstimmungen`):** Ausschließlich für Administrator:innen zugänglich (`401` unangemeldet, `403` für Bürger:innen oder ungültiges CSRF-Token). Unterstützt `datetime-local`-Eingabefelder, dynamisches Hinzufügen von Antwortoptionen (`GET /abstimmungen/neu/option-feld`), fachliche Validierung (u. a. Titel 3–100 Zeichen, Beginn nicht in der Vergangenheit, Ende nach Beginn, mindestens 2 eindeutige Optionen) sowie htmx-4-Teilaktualisierung bei Validierungsfehlern (`422 Unprocessable Content`).

## Architekturübersicht

- `de.mse.votee_backend.abstimmungsverwaltung`
  - `domain.model`: `Abstimmung` (Aggregate Root), `AbstimmungId`, `NutzerId`, `Titel`, `Abstimmungszeitraum`, `AbstimmOption`, `AbstimmungsregelVerletztException`
  - `domain.repository`: Port `AbstimmungRepository`
  - `application`: `AbstimmungService`, `ErstelleAbstimmungBefehl`, `AbstimmungEingabeValidator`, Port `BerechtigungsPruefung`, `KeineBerechtigungException`
  - `infrastructure`: `H2AbstimmungRepository`, `DatenbankInitialisierung`
- `de.mse.votee_backend.kontoverwaltung`
  - `domain.model`: `NutzerKonto`, `Rolle` (`ADMIN`, `BUERGER`), `EinmalCode`
  - `application`: `AuthentifizierungsService`, `Session`, `SessionStore`, Ports `NutzerKontoRepository`, `PasswortHasher`, `ZweiFaktorPruefung`
  - `infrastructure`: `Pbkdf2PasswortHasher`, `FesterCodeZweiFaktorPruefung`, `KonfigurierteNutzerKonten`, `KontoBerechtigungsAdapter`
- `de.mse.votee_backend`
  - Composition Root (`Main`) und HTTP-/Template-Adapter (`PageService`, `AuthHttpService`, `AbstimmungHttpService`, `StatischeDateienService`, `Templates`, `HtmxHelfer`)

## Projekt bauen und testen

Voraussetzung ist **Java 27** (empfohlen über den Maven Wrapper `./mvnw`):

```bash
./mvnw checkstyle:check verify
```

## Projekt starten

Direkt mit Maven kompilieren und starten:

```bash
./mvnw compile exec:exec
```

Alternativ als paketiertes JAR:

```bash
./mvnw package -DskipTests
java -jar target/votee_backend.jar
```

Danach ist die Anwendung unter `http://localhost:8080/` erreichbar.

### Konfiguration & Entwicklungs-Zugangsdaten

Die Laufzeitkonfiguration liegt in `src/main/resources/application.yaml`:

- **Port:** `8080`
- **Datenbank:** Dateibasierte H2-Datenbank unter `./data/votee` (Daten bleiben über Neustarts hinweg erhalten; Tests verwenden isolierte H2-In-Memory-Datenbanken gemäß `src/test/resources/application.yaml`).
- **Entwicklungs-Standardkonten** (für den Produktivbetrieb über Umgebungsvariablen überschreiben):
  - Administrator:in: `admin` / `admin-dev-password`
  - Bürger:in: `buerger` / `buerger-dev-password`
  - 2FA-Einmalcode (Entwicklungs-Stub): `00000`

## Tests & CI-Pipeline

Die GitHub-Actions-Pipeline (`.github/workflows/maven.yml`) läuft bei jedem Push auf `main` und bei jedem Pull Request. Ein neuer Push bricht einen noch laufenden, veralteten Lauf auf demselben Branch ab.

Schritte:

1. **Lint:** `./mvnw -B checkstyle:check` prüft den Code mit den Regeln aus `checkstyle.xml` (Formatierung, ungenutzte Imports, Namen, typische Fehlerquellen)
2. **Build und Test:** `./mvnw -B verify` kompiliert, führt die gesamte Testsuite aus und baut das JAR
3. **Artefakte:** Testberichte werden immer hochgeladen, das JAR samt `libs/` nur bei Pushes auf `main`

Die Testsuite in `src/test/java/de/mse/votee_backend/` umfasst aktuell 255 Tests:

- **Domäne & Validierung (`abstimmungsverwaltung`):** `AbstimmungTest`, `AbstimmungIdTest`, `TitelTest`, `AbstimmungszeitraumTest`, `AbstimmOptionTest`, `NutzerIdTest`, `AbstimmungsregelVerletztExceptionTest`, `AbstimmungEingabeValidatorTest`, `KeineBerechtigungExceptionTest`
- **Persistenz & Anwendungsservice:** `H2AbstimmungRepositoryTest`, `AbstimmungServiceTest` (gegen isolierte H2-In-Memory-DBs mit injizierter `Clock`)
- **Kontoverwaltung & Sicherheit (`kontoverwaltung`):** `EinmalCodeTest`, `Pbkdf2PasswortHasherTest`, `FesterCodeZweiFaktorPruefungTest`, `AuthentifizierungsServiceTest`, `SessionStoreTest`
- **Web- & HTTP-Integration (`@ServerTest`):**
  - `MainTest` prüft Startseite (`/`), Stylesheet (`/css/style.css`) und `404` bei unbekannten Pfaden
  - `TemplatesTest` prüft Voll- und Fragment-Rendering (`render` / `renderFragment`)
  - `AbstimmungErstellenHttpTest` prüft Login mit 5-stelligem 2FA-Code, Session-Cookies, CSRF-Schutz, Rollenprüfung (`401`/`403`), htmx-4-Fehlerfragmente (`422`), Abstimmungserstellung inkl. Sortierung nach Enddatum sowie Logout

## Weitere Dokumentation

- `doc/`: Übungsunterlagen, Entwürfe und DDD-Review (`doc/DDD_Review.md`)
- `llm_log/interaktionslog.md`: Protokoll der Entwicklungsinteraktionen
