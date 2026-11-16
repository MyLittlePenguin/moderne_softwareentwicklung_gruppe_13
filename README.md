# moderne_softwareentwicklung_gruppe_13

[![Java CI with Maven](https://github.com/MyLittlePenguin/moderne_softwareentwicklung_gruppe_13/actions/workflows/maven.yml/badge.svg)](https://github.com/MyLittlePenguin/moderne_softwareentwicklung_gruppe_13/actions/workflows/maven.yml)

Dieses Backend wird mit Java 27 und [Helidon SE](https://helidon.io) 27 entwickelt.

- **HTML-Seiten:** [Thymeleaf](https://www.thymeleaf.org), Templates liegen in
  `src/main/resources/templates/`
- **Datenbank:** H2 über den Helidon DB Client (JDBC + HikariCP), konfiguriert in
  `application.yaml`, aber noch nicht im Code verwendet.

## Projekt bauen und testen

```bash
./mvnw verify
```

## Projekt starten

```bash
./mvnw package -DskipTests
java -jar target/votee_backend.jar
```

Danach ist die Startseite unter `http://localhost:8080/` erreichbar.

Die Konfiguration (Port, Datenbank-URL) liegt in `src/main/resources/application.yaml`.
Standardmäßig läuft H2 im Speicher, die Daten gehen also beim Neustart verloren.

## CI-Pipeline

Die GitHub-Actions-Pipeline (`.github/workflows/maven.yml`) läuft bei jedem Push
auf `main` und bei jedem Pull Request. Ein neuer Push bricht einen noch laufenden,
veralteten Lauf auf demselben Branch ab.

Schritte:

1. **Lint:** `./mvnw checkstyle:check` prüft den Code mit den Regeln aus
   `checkstyle.xml` (Formatierung, ungenutzte Imports, Namen, typische Fehlerquellen)
2. **Build und Test:** `./mvnw verify` kompiliert, führt die Tests aus und baut das Jar
3. **Artefakte:** Testberichte werden immer hochgeladen, das Jar samt `libs/` nur
   bei Pushes auf `main`

Lokal lässt sich dasselbe mit `./mvnw checkstyle:check verify` prüfen.

Die Tests in `src/test/java/de/mse/votee_backend/` prüfen:

- `MainTest` startet einen echten Helidon-Webserver auf einem zufälligen Port:
  `/` liefert die gerenderte HTML-Startseite, unbekannte Pfade liefern `404`
- `TemplatesTest` prüft, dass Thymeleaf Variablen ins Template einsetzt
