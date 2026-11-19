# 5.3 LLM-Prompt und Antwort

**LLM:** ChatGPT GPT-6 Medium, 19.11.2026, neuer Chat ohne Vorwissen. Code-Auszüge sind im Prompt enthalten (Stand `abgabe_05`, `64fae10`).

## Prompt

---

Du hilfst mir, die technische Schuld (Technical Debt) meines Java-Projekts zu bewerten und zu reduzieren. Ich will **Empfehlungen mit Begründung**, keinen komplett neu geschriebenen Code. Kurze Code-Skizzen sind okay.

**Projekt:** eVote „votee“, Online-Abstimmungssystem, Studienprojekt. Java 27, Helidon SE 27.0.1, Maven, JUnit 5 (194 Tests grün, Coverage 96 % Instructions / 97 % Branches). Domain-Driven Design: `Abstimmung` ist Aggregate Root, Value Objects sind Records, Domäne ohne Framework. Fachliche Fehler werfen `AbstimmungsregelVerletztException` (Unterklasse von `IllegalArgumentException`). `Main`, `PageService`, `Templates` stammen aus dem Helidon-Projekttemplate.

## Messergebnisse

**SonarQube Community (Regelwerk „Sonar way“):** 0 Issues, 0,0 % Duplikate, Quality Gate bestanden. Ein früheres Issue (Reliability/High: `assertNotEquals("x", a)` vergleicht verschiedene Typen) ist schon behoben.

**PMD 7.29 (eigenes Regelwerk: bestpractices, errorprone, design; Schwellen: Klassen-CC 30, Methoden-CC 10, max. 6 Parameter):** 8 Meldungen, CPD (Copy-Paste-Detector, min. 30 Tokens): 0 Duplikate.

| # | Datei | Regel | PMD-Priorität | Meldung |
| --- | --- | --- | --- | --- |
| 1 | `Main.java` Z. 24 | SystemPrintln | 2 | Usage of System.out/err |
| 2 | `PageService.java` Z. 27 | UnusedFormalParameter | 3 | ungenutzter Parameter `request` in `index(ServerRequest, ServerResponse)` (Helidon-Handler-Signatur) |
| 3 | `Abstimmung.java` Z. 11 | CyclomaticComplexity | 3 | Klasse gesamt 42 (höchste Methode 10) |
| 4 | `Abstimmung.java` Z. 45 | CyclomaticComplexity | 3 | `erstelle(...)` hat 10 |
| 5 | `Abstimmung.java` Z. 45 | ExcessiveParameterList | 3 | 6 Parameter |
| 6 | `Abstimmung.java` Z. 87 | CyclomaticComplexity | 3 | `pruefeOptionen(...)` hat 10 |
| 7 | `Abstimmung.java` Z. 119 | ExcessiveParameterList | 3 | zweite `erstelle`-Überladung, 6 Parameter |
| 8 | `AbstimmungsregelVerletztException.java` | MissingSerialVersionUID | 3 | keine `serialVersionUID` |

**Veraltete Abhängigkeiten (versions-maven-plugin):** alle direkt gepflegten Abhängigkeiten und Plugins aktuell. Genutzt und veraltet: Helidon 27.0.1 → 27.0.2 (Patch), H2 2.4.240 → 2.5.252 (DB noch ungenutzt), `hamcrest-all` 1.3 (2012, eingestellt; nur in 2 Template-Tests). JUnit 6.2.0-M1 und slf4j 2.1.0-alpha1 sind Vorabversionen. `maven-site-plugin` ohne Version, keine Mindest-Maven-Version (kein `maven-enforcer-plugin`).

**Eigene Beobachtungen aus der Metrik-Analyse (von keinem Tool gemeldet):**
- A: `Abstimmungszeitraum` prüft in drei Methoden identisch `zeitpunkt == null` und wirft dort `IllegalArgumentException`, der Konstruktor dagegen `AbstimmungsregelVerletztException` (uneinheitlich).
- B: `Abstimmung` hat zwei `erstelle`-Überladungen (`Titel` und `String`) sowie zwei Getter `getTitel()` (String) und `getTitelObjekt()` (Titel).
- C: `AbstimmungEingabeValidator.istGueltigerTitel` nutzt eine Exception als Kontrollfluss (`try { new Titel(t); return true; } catch (IllegalArgumentException e) { return false; }`).

## Code (Auszüge)

```java
// Abstimmung.java (Auszug)
public static Abstimmung erstelle(AbstimmungId id, NutzerId erstelltVon, Titel titel,
                                  String beschreibung, Abstimmungszeitraum zeitraum,
                                  List<AbstimmOption> optionen) {
    if (id == null) throw new AbstimmungsregelVerletztException("AbstimmungId muss angegeben werden.");
    if (erstelltVon == null) throw new AbstimmungsregelVerletztException("Ersteller (NutzerId) muss angegeben werden.");
    if (titel == null) throw new AbstimmungsregelVerletztException("Titel darf nicht leer sein.");
    String bereinigteBeschreibung = beschreibung == null ? "" : beschreibung.strip();
    if (zeitraum == null) throw new AbstimmungsregelVerletztException("Abstimmungszeitraum muss angegeben werden.");
    List<AbstimmOption> gepruefteOptionen = pruefeOptionen(optionen);
    return new Abstimmung(id, erstelltVon, titel, bereinigteBeschreibung, zeitraum, gepruefteOptionen);
}

private static List<AbstimmOption> pruefeOptionen(List<AbstimmOption> optionen) {
    if (optionen == null) throw new AbstimmungsregelVerletztException("Optionsliste darf nicht null sein.");
    if (optionen.size() < 2) throw new AbstimmungsregelVerletztException("Eine Abstimmung muss mindestens zwei Optionen haben.");
    // Bewusst Schleife statt Stream: bricht beim ersten Duplikat ab und nennt es in der Meldung
    Set<String> normalisierteTexte = new HashSet<>();
    for (AbstimmOption option : optionen) {
        if (option == null) throw new AbstimmungsregelVerletztException("Option darf nicht null sein.");
        if (!normalisierteTexte.add(option.normalisierterText()))
            throw new AbstimmungsregelVerletztException("Doppelte Option gefunden: " + option.text());
    }
    return List.copyOf(optionen);
}

public static Abstimmung erstelle(AbstimmungId id, NutzerId erstelltVon, String titel,
                                  String beschreibung, Abstimmungszeitraum zeitraum,
                                  List<AbstimmOption> optionen) {
    return erstelle(id, erstelltVon, new Titel(titel), beschreibung, zeitraum, optionen);
}
// außerdem: getTitel() liefert titel.wert(), getTitelObjekt() liefert titel;
// istLaufend, istBeendet, istNochNichtGestartet, kannAbgestimmtWerden, kannErgebnisEingesehenWerden,
// hatOption(AbstimmOption), hatOption(String), equals/hashCode über id
```

```java
// Abstimmungszeitraum.java
public record Abstimmungszeitraum(LocalDateTime beginn, LocalDateTime ende) {
    public Abstimmungszeitraum {
        if (beginn == null) throw new AbstimmungsregelVerletztException("Beginn darf nicht null sein.");
        if (ende == null) throw new AbstimmungsregelVerletztException("Ende darf nicht null sein.");
        if (!ende.isAfter(beginn)) throw new AbstimmungsregelVerletztException("Das Ende muss nach dem Beginn liegen.");
    }
    public boolean enthaelt(LocalDateTime zeitpunkt) {
        if (zeitpunkt == null) throw new IllegalArgumentException("Zeitpunkt darf nicht null sein.");
        return !zeitpunkt.isBefore(beginn) && !zeitpunkt.isAfter(ende);
    }
    public boolean istVorbei(LocalDateTime zeitpunkt) {
        if (zeitpunkt == null) throw new IllegalArgumentException("Zeitpunkt darf nicht null sein.");
        return zeitpunkt.isAfter(ende);
    }
    public boolean istInZukunft(LocalDateTime zeitpunkt) {
        if (zeitpunkt == null) throw new IllegalArgumentException("Zeitpunkt darf nicht null sein.");
        return zeitpunkt.isBefore(beginn);
    }
}
```

```java
// Main.java, Z. 24
System.out.println("Votee backend runs at http://localhost:" + server.port() + "/");

// PageService.java (Helidon-Handler)
private void index(ServerRequest request, ServerResponse response) {
    String html = templates.render("index", Map.of("title", "Votee"));
    response.header(HeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
    response.send(html);
}

// AbstimmungsregelVerletztException.java
public class AbstimmungsregelVerletztException extends IllegalArgumentException {
    public AbstimmungsregelVerletztException(String meldung) { super(meldung); }
}
```

## Fragen

1. **Schweregrad:** Ordne jeden Befund (PMD 1–8, Abhängigkeiten, Beobachtungen A–C) einer Stufe **Hoch / Mittel / Niedrig / kein Handlungsbedarf** zu. Begründe kurz, ob es sich um echte Schuld oder um einen Fehlalarm des Tools handelt.
2. **Maßnahmen:** Nenne zu jedem Befund mit Handlungsbedarf eine konkrete Maßnahme mit geschätztem Aufwand (Minuten) und Risiko für bestehende Tests.
3. **Priorisierung:** Welche 3 Maßnahmen würdest du zuerst umsetzen und warum?
4. **Komplexität von `Abstimmung`:** Ist die zyklomatische Komplexität von `erstelle` und `pruefeOptionen` (je 10) für eine Factory mit Invariantenprüfung ein Problem? Wäre ein Parameter-Objekt oder Builder gegen die 6 Parameter in einem DDD-Aggregate sinnvoll oder Overengineering?
5. **Grenzen der Tools:** Warum melden SonarQube (0 Issues) und PMD (8 Meldungen) so unterschiedlich viel? Was sagt das über Technical-Debt-Kennzahlen aus?

## Antwort (vollständig, unverändert)

~~~text
Technical-Debt-Analyse – eVote „votee“
Bewertungsgrundlage: Java 27, Helidon SE, Maven, JUnit 5, Domain-Driven Design, SonarQube Community und PMD 7.29.
Gesamtbewertung
Dein Projekt weist aktuell eine geringe technische Schuld auf. Die Messergebnisse sprechen für einen guten technischen Zustand. Allerdings gibt es einige kleinere Wartbarkeitsprobleme und Verbesserungsmöglichkeiten, die durch die statische Analyse nicht vollständig erfasst werden.

Besonders positiv sind:
- 194 erfolgreiche Tests mit 96 % Instruction- und 97 % Branch-Coverage.
- 0 SonarQube-Issues und bestandenes Quality Gate.
- 0 % Code-Duplikation bei SonarQube und keine CPD-Treffer bei PMD.
- Frameworkunabhängige Domänenschicht, entsprechend der DDD-Architektur.
- Fachliche Invarianten werden direkt bei der Objekterzeugung geprüft.
- Value Objects werden durch Records repräsentiert.
Die acht PMD-Meldungen bedeuten nicht, dass acht echte Probleme existieren.
Meine Einschätzung
Kategorie	Anzahl der Befunde
Hoch	0
Mittel	4
Niedrig	9
Kein Handlungsbedarf	4
Gesamt	17

Die 17 Befunde setzen sich aus acht PMD-Meldungen, sechs Abhängigkeits- und Build-Befunden sowie deinen drei eigenen Beobachtungen zusammen.

Die Einstufungen sind eine fachliche Risikobewertung und keine direkte Übernahme der PMD-Prioritäten.

Wichtig: Hohe Testabdeckung reduziert das Änderungsrisiko, beweist aber nicht die Fehlerfreiheit. Sie sagt insbesondere wenig darüber aus, ob fachlich sinnvolle Tests, Grenzfälle und Architekturregeln vollständig abgedeckt sind.
1. Bewertung der acht PMD-Meldungen
Übersicht
Nr.	PMD-Regel	Schweregrad	Tatsächliche Schuld?	Empfehlung
1	SystemPrintln	Niedrig	Geringfügig	Logging verwenden
2	UnusedFormalParameter	Kein Handlungsbedarf	Nein	Meldung begründet akzeptieren
3	CyclomaticComplexity – Klasse 42	Niedrig	Bedingt	Verantwortlichkeiten beobachten
4	CyclomaticComplexity – erstelle() 10	Kein Handlungsbedarf	Nein	Invariantenprüfung beibehalten
5	ExcessiveParameterList – erstelle()	Niedrig	Bedingt	Aktuell sechs Parameter akzeptieren
6	CyclomaticComplexity – pruefeOptionen() 10	Kein Handlungsbedarf	Nein	Implementierung beibehalten
7	ExcessiveParameterList – Überladung	Kein Handlungsbedarf	Nein	Nicht isoliert umbauen
8	MissingSerialVersionUID	Niedrig	Ja, geringfügig	Konstante ergänzen

PMD 1 – SystemPrintln
Schweregrad: Niedrig

Problem: Die Anwendung verwendet System.out.println() zur Ausgabe einer Startmeldung.

Das ist für eine kleine lokale Anwendung nicht grundsätzlich falsch. In einer Serveranwendung ist strukturiertes Logging aber langfristig besser.

Vorteile:
- Einheitliche Protokollierung.
- Konfigurierbare Log-Level.
- Bessere Integration in Serverumgebungen.
- Einfachere Fehlerdiagnose.
Maßnahme: Den bereits im Projekt verwendeten Logging-Mechanismus nutzen, statt ein zusätzliches Framework einzuführen.

Beispielsweise mit SLF4J:
private static final Logger LOG =
        LoggerFactory.getLogger(Main.class);

// Nach dem Serverstart:
LOG.info("Votee backend runs on port {}", server.port());

Aufwand: 5–10 Minuten
Risiko für bestehende Tests: Sehr gering

Da Main.java aus dem Helidon-Template stammt, handelt es sich eher um eine kleinere Anpassung des Infrastrukturcodes als um eine Architekturverbesserung.
PMD 2 – UnusedFormalParameter
Schweregrad: Kein Handlungsbedarf
private void index(ServerRequest request, ServerResponse response)

Bewertung: Fachlich begründeter Fehlalarm.

Die Methode muss zur Handler-Signatur von Helidon passen. Dass der Parameter request nicht benötigt wird, ist kein Designfehler.

Den Parameter zu entfernen würde die vorgesehene Handler-Signatur verändern.

Empfehlung: Beibehalten.

Falls du einen vollständig bereinigten PMD-Bericht benötigst, kannst du die Meldung gezielt unterdrücken und die Begründung dokumentieren.

Aufwand: 0 Minuten
Testrisiko: Keines
PMD 3 – CyclomaticComplexity der Klasse Abstimmung
Schweregrad: Niedrig

PMD meldet eine Gesamtkomplexität von 42. Dein individuell festgelegter Grenzwert liegt bei 30.

Das bedeutet jedoch nicht automatisch, dass die Klasse übermäßig komplex ist.

Die Klasse bildet einen Aggregate Root ab. Dieser ist im DDD dafür verantwortlich, bestimmte fachliche Regeln und Zustandsübergänge zu kontrollieren.

Bei Abstimmung gehören dazu:
- Validierung bei der Erstellung.
- Verwaltung gültiger Abstimmungsoptionen.
- Prüfung des Abstimmungszeitraums.
- Abfrage des Abstimmungszustands.
- Fachliche Zugriffsregeln auf Abstimmung und Ergebnis.
Diese Verantwortlichkeiten hängen unmittelbar mit dem Aggregat zusammen.

Entscheidend ist, ob die Klasse mehrere unabhängige fachliche Verantwortlichkeiten übernimmt.

Eine hohe Summe der zyklomatischen Komplexität ist dafür allein kein ausreichender Beleg.

Die Klassenkomplexität ist außerdem nicht mit der Komplexität einer einzelnen Methode gleichzusetzen.

Empfehlung:

Die Klasse vorerst nicht aufteilen.

Beobachte bei zukünftigen Änderungen, ob neue unabhängige Verantwortlichkeiten entstehen.

Aufwand: 0 Minuten unmittelbar; 10–20 Minuten für eine gezielte Architekturprüfung
Testrisiko: Keines bei reiner Prüfung
PMD 4 – CyclomaticComplexity von erstelle()
Schweregrad: Kein Handlungsbedarf
if (id == null) ...
if (erstelltVon == null) ...
if (titel == null) ...
if (zeitraum == null) ...

Die Komplexität entsteht überwiegend durch die Überprüfung fachlicher Invarianten.

Jede Prüfung beantwortet eine eigenständige Frage:

Kann mit diesen Eingaben eine gültige Abstimmung erzeugt werden?

Das ist eine zentrale Aufgabe des Aggregate Roots.

Eine Auslagerung dieser Prüfungen würde die fachliche Komplexität nicht beseitigen, sondern lediglich in andere Methoden verschieben.

Empfehlung: Beibehalten.

Eine weitere Aufteilung ist erst sinnvoll, wenn dadurch tatsächlich eine eigenständige fachliche Verantwortlichkeit sichtbar wird.

Aufwand: 0 Minuten
Testrisiko: Keines
PMD 5 – ExcessiveParameterList
Schweregrad: Niedrig
erstelle(
    AbstimmungId id,
    NutzerId erstelltVon,
    Titel titel,
    String beschreibung,
    Abstimmungszeitraum zeitraum,
    List<AbstimmOption> optionen
)

Sechs Parameter sind nicht grundsätzlich problematisch.

Entscheidend ist ihre Bedeutung.

In diesem Fall beschreiben alle sechs Parameter die notwendigen Eigenschaften einer Abstimmung.

Außerdem sind bereits mehrere Parameter durch Value Objects typisiert.

Dadurch wird die Gefahr einer falschen Parameterzuordnung verringert.

Trotzdem entstehen kleinere Wartbarkeitskosten:
- Neue Eigenschaften verlängern möglicherweise die Signatur.
- Methodenaufrufe benötigen mehrere Argumente.
- Überladungen können die API vergrößern.
Empfehlung: Aktuell akzeptieren.

Ein Parameter-Objekt oder Builder ist erst sinnvoll, wenn der Erstellungsprozess tatsächlich umfangreicher wird.

Aufwand: 0 Minuten aktuell; ungefähr 30–60 Minuten für eine spätere Umstellung
Testrisiko: Mittel bei einer Umstellung, weil sämtliche Aufrufstellen betroffen sein können
PMD 6 – CyclomaticComplexity von pruefeOptionen()
Schweregrad: Kein Handlungsbedarf

Die Methode überprüft:
1. Ob eine Optionsliste existiert.
2. Ob mindestens zwei Optionen enthalten sind.
3. Ob einzelne Optionen null sind.
4. Ob doppelte Optionen vorhanden sind.
Anschließend wird eine unveränderliche Kopie erstellt.

Das ist eine klar abgegrenzte Verantwortung.

Besonders sinnvoll ist deine Entscheidung, eine Schleife statt eines Streams einzusetzen.
if (!normalisierteTexte.add(option.normalisierterText())) {
    throw new AbstimmungsregelVerletztException(
        "Doppelte Option gefunden: " + option.text()
    );
}

Die Schleife ermöglicht das sofortige Abbrechen und liefert eine konkrete Fehlermeldung.

Ein Stream würde hier nicht zwangsläufig eine Verbesserung bringen.

Empfehlung: Beibehalten.

Aufwand: 0 Minuten
Testrisiko: Keines
PMD 7 – ExcessiveParameterList der zweiten Überladung
Schweregrad: Kein Handlungsbedarf
public static Abstimmung erstelle(
        AbstimmungId id,
        NutzerId erstelltVon,
        String titel,
        String beschreibung,
        Abstimmungszeitraum zeitraum,
        List<AbstimmOption> optionen) {

    return erstelle(
        id,
        erstelltVon,
        new Titel(titel),
        beschreibung,
        zeitraum,
        optionen
    );
}

Diese Methode enthält keine zusätzliche fachliche Logik.

Sie stellt lediglich eine bequemere Schnittstelle für die Erstellung bereit.

Die sechs Parameter sind daher keine zweite unabhängige Designschwäche.

Empfehlung: Die Überladung im Zusammenhang mit Beobachtung B bewerten, nicht isoliert wegen der PMD-Meldung verändern.

Aufwand: 0 Minuten
Testrisiko: Keines
PMD 8 – MissingSerialVersionUID
Schweregrad: Niedrig

Die Exception erweitert IllegalArgumentException und ist damit serialisierbar.

Deshalb empfiehlt PMD die Deklaration einer serialVersionUID.

Maßnahme:
public class AbstimmungsregelVerletztException
        extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public AbstimmungsregelVerletztException(String meldung) {
        super(meldung);
    }
}

Die Ergänzung verbessert die explizite Versionierung der serialisierbaren Klasse.

Allerdings verwendet dein Projekt vermutlich keine Java-Objektserialisierung für Exceptions.

Daher ist der praktische Nutzen gering.

Aufwand: 2 Minuten
Risiko für bestehende Tests: Praktisch keines

Fazit zu PMD: Von den acht Meldungen würde ich nur zwei unmittelbar korrigieren: SystemPrintln und MissingSerialVersionUID. Die übrigen Befunde würde ich dokumentieren oder beobachten, aber nicht allein zur Verbesserung des PMD-Berichts refaktorieren.
2. Bewertung der Abhängigkeiten und Build-Konfiguration
Bei Abhängigkeiten muss zwischen drei Risiken unterschieden werden:

Sicherheitsrisiko: Eine Version enthält bekannte Schwachstellen.

Wartungsrisiko: Eine Bibliothek wird nicht mehr gepflegt oder verursacht Kompatibilitätsprobleme.

Reproduzierbarkeitsrisiko: Das Projekt lässt sich mit unterschiedlichen Entwicklungsumgebungen nicht zuverlässig identisch bauen.

Für die folgende Bewertung verwende ich die von dir angegebenen Versionsstände. Eine vollständige Sicherheitsbewertung würde zusätzlich einen Dependency- und CVE-Scan benötigen.
Übersicht
Befund	Schweregrad	Maßnahme	Aufwand	Testrisiko
Helidon 27.0.1 → 27.0.2	Niedrig	Patch aktualisieren	10–20 Min.	Gering
H2 2.4.240 → 2.5.252	Niedrig	Abhängigkeit prüfen, ggf. entfernen	5–15 Min.	Gering
hamcrest-all 1.3	Mittel	Ersetzen oder entfernen	10–20 Min.	Gering
JUnit-Milestone / SLF4J-Alpha	Niedrig	Vorabversionen nicht übernehmen	0–5 Min.	Keines
maven-site-plugin ohne Version	Niedrig	Version explizit festlegen	5 Min.	Gering
Kein Maven Enforcer	Mittel	Build-Anforderungen absichern	15–30 Min.	Gering bis mittel

2.1 Helidon – Patch-Update
Schweregrad: Niedrig

Eine veraltete Patch-Version stellt nicht automatisch ein Sicherheitsproblem dar.

Trotzdem können Patch-Releases Fehlerkorrekturen und Sicherheitsverbesserungen enthalten.

Empfehlung: Das angegebene Patch-Update durchführen, sofern es für deine verwendeten Helidon-Artefakte vorgesehen ist.

Anschließend:
mvn clean verify

Zusätzlich würde ich den Server einmal starten und einen HTTP-Smoke-Test durchführen.

Deine Domain-Tests überprüfen schließlich nicht zwangsläufig das Zusammenspiel von HTTP-Server, Routing und Templates.

Aufwand: 10–20 Minuten
Testrisiko: Gering

Bewertung: Normale Abhängigkeitspflege, keine ausgeprägte technische Schuld.
2.2 H2 – Ungenutzte Datenbankabhängigkeit
Schweregrad: Niedrig

H2 ist derzeit noch nicht in Verwendung.

Hier stellt sich eine wichtige Frage:

Warum befindet sich die Abhängigkeit bereits im Projekt?

Wenn H2 in einer unmittelbar geplanten Entwicklungsphase benötigt wird, kannst du sie behalten und zum vorgesehenen Zeitpunkt aktualisieren.

Wenn dagegen noch keine Datenbankanbindung entwickelt wird, würde ich die Abhängigkeit vorerst entfernen.

Das entspricht dem YAGNI-Prinzip (You Aren't Gonna Need It).

Eine nicht verwendete Abhängigkeit verursacht unnötigen Wartungsaufwand und vergrößert den Abhängigkeitsgraphen.

Empfehlung: Verwendung mit mvn dependency:analyze und einer Projektsuche prüfen.

Beachte, dass Maven nicht jede indirekte oder dynamische Verwendung zuverlässig erkennen kann.

Aufwand: 5–15 Minuten
Testrisiko: Gering
2.3 hamcrest-all 1.3
Schweregrad: Mittel

Das ist einer der wenigen Befunde, die ich als tatsächliche Wartungsschuld einstufen würde.

Die Bibliothek ist sehr alt und wird in deinem Projekt ausschließlich für zwei Template-Tests benötigt.

Eine solche Abhängigkeit weiterzuführen, obwohl bereits JUnit 5 verwendet wird, bringt wenig Nutzen.

Empfehlung: Die zwei Tests auf JUnit-Assertions umstellen, sofern sie keine speziellen Hamcrest-Matcher benötigen.

Beispiel:
// Vorher
assertThat(ergebnis, is("Erfolg"));

// Nachher
assertEquals("Erfolg", ergebnis);

Anschließend die nicht mehr benötigte Abhängigkeit aus der pom.xml entfernen.

Wenn komplexe Matcher benötigt werden, wäre alternativ eine aktuelle Hamcrest-Version sinnvoll.

Aufwand: 10–20 Minuten
Testrisiko: Gering

Die Änderung ist überschaubar, reduziert eine veraltete Abhängigkeit und vereinfacht den Testaufbau.
2.4 JUnit 6.2.0-M1 und SLF4J 2.1.0-alpha1
Schweregrad: Niedrig

Hier besteht zunächst kein unmittelbarer Änderungsbedarf.

Dass das versions-maven-plugin neuere Vorabversionen erkennt, bedeutet nicht, dass du diese übernehmen solltest.

Milestone- und Alpha-Versionen können noch Änderungen enthalten, die vor einem stabilen Release angepasst werden.

Empfehlung:
- Bei den bereits verwendeten stabilen Versionen bleiben.
- Vorabversionen bei zukünftigen Updateprüfungen bewusst ignorieren.
- Erst bei einer stabilen Veröffentlichung erneut bewerten.
Aufwand: 0–5 Minuten
Testrisiko: Keines

Die Verwendung stabiler Versionen ist keine technische Schuld.
2.5 maven-site-plugin ohne Versionsangabe
Schweregrad: Niedrig

Nicht explizit festgelegte Plugin-Versionen können die Reproduzierbarkeit von Builds beeinträchtigen.

Je nach Maven-Konfiguration können unterschiedliche Plugin-Versionen verwendet werden.

Empfehlung: Eine geprüfte Plugin-Version in der pom.xml festlegen, sofern das Plugin benötigt wird.
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-site-plugin</artifactId>
    <version><!-- geprüfte stabile Version --></version>
</plugin>

Wird das Plugin nicht verwendet und nur durch das Template mitgeliefert, kannst du stattdessen prüfen, ob es entfernt werden sollte.

Aufwand: 5 Minuten
Testrisiko: Gering
2.6 Fehlender Maven Enforcer
Schweregrad: Mittel

Das ist aus meiner Sicht der wichtigste Befund zur Build-Konfiguration.

Dein Projekt verwendet Java 27 und baut auf einer konkreten Entwicklungsumgebung auf.

Ohne explizite Versionsanforderungen können Teammitglieder versehentlich mit einer ungeeigneten Java- oder Maven-Version arbeiten.

Der Maven Enforcer kann solche Voraussetzungen automatisch überprüfen.

Beispiel für die relevanten Regeln:
<rules>
    <requireJavaVersion>
        <version>[27,28)</version>
    </requireJavaVersion>

    <requireMavenVersion>
        <version>[3.9,)</version>
    </requireMavenVersion>
</rules>

Die Konfiguration gehört in eine entsprechend eingebundene Ausführung des maven-enforcer-plugin.

Die Maven-Untergrenze 3.9 ist hier ein Beispiel. Du solltest die tatsächlich im Projekt getestete Mindestversion festlegen.

Die Regeln werden vom offiziellen Maven Enforcer unterstützt. maven.apache.org

Aufwand: 15–30 Minuten
Testrisiko: Gering bis mittel

Das Risiko besteht hauptsächlich darin, dass ein Build in einer bisher tolerierten Entwicklungsumgebung künftig bewusst fehlschlägt.

Zusätzliche Empfehlung: Falls ihr im Team unterschiedliche Maven-Versionen verwendet, wäre auch der Maven Wrapper sinnvoll. Er sorgt dafür, dass alle dieselbe projektseitig festgelegte Maven-Version verwenden können.
3. Bewertung deiner eigenen Beobachtungen A–C
Diese Befunde sind besonders interessant.

Sie zeigen, dass eine statische Codeanalyse nicht sämtliche Wartbarkeitsprobleme erkennen kann.
A – Uneinheitliche Exception-Typen
Schweregrad: Mittel

Im Konstruktor von Abstimmungszeitraum verwendest du:
throw new AbstimmungsregelVerletztException(...);

In den Methoden enthaelt(), istVorbei() und istInZukunft() dagegen:
throw new IllegalArgumentException(...);

Obwohl beide Exceptions miteinander verwandt sind, unterscheiden sie sich semantisch.

IllegalArgumentException beschreibt allgemein ein ungültiges Methodenargument.

AbstimmungsregelVerletztException signalisiert eine Verletzung einer fachlichen Regel.
Warum ist das problematisch?
Aufrufende Klassen können anhand des Exception-Typs möglicherweise nicht einheitlich unterscheiden, ob ein technischer Aufruffehler oder eine verletzte Domänenregel vorliegt.

Daher sollte die Entscheidung bewusst und konsistent getroffen werden.

Allerdings muss nicht jede ungültige Eingabe automatisch eine fachliche Regelverletzung sein.

Beispielsweise lässt sich argumentieren:
- Ein ungültiger Abstimmungszeitraum verletzt eine fachliche Invariante.
- Ein null-Argument für eine zeitliche Abfrage ist dagegen ein Programmierfehler des Aufrufers.
Deshalb würde ich nicht automatisch alle Exceptions vereinheitlichen.
Empfohlene Maßnahme
Lege einen dokumentierten Exception-Vertrag fest.

Mein Vorschlag:
- Fachlich ungültiger Zustand: AbstimmungsregelVerletztException
- Ungültiges Methodenargument aufgrund eines Programmierfehlers: IllegalArgumentException
Damit kann deine bestehende Implementierung fachlich korrekt sein.

Ergänze aber einen kurzen Kommentar beziehungsweise Javadoc und stelle sicher, dass die Tests den beabsichtigten Vertrag prüfen.

Alternativ kannst du dich bewusst dafür entscheiden, sämtliche ungültigen Domäneneingaben über deine eigene Exception abzubilden.

Aufwand: 15–25 Minuten
Testrisiko: Gering bei Dokumentation, mittel bei einer Änderung der Exception-Typen

Bewertung: Die eigentliche technische Schuld besteht im bisher nicht eindeutig festgelegten Exception-Vertrag, nicht zwangsläufig in der Verwendung zweier Exception-Klassen.
B – Überladene Factory-Methoden und doppelte Titel-Getter
Schweregrad: Mittel

Du hast:
erstelle(..., String titel, ...)
erstelle(..., Titel titel, ...)

Außerdem:
getTitel()
getTitelObjekt()

Beides hat nachvollziehbare Gründe.

Die String-Überladung erleichtert die Verwendung. Der Getter mit String-Rückgabe kann beispielsweise für die Darstellung oder bestehende Aufrufstellen praktisch sein.

Allerdings entstehen zwei unterschiedliche Möglichkeiten, denselben fachlichen Wert zu verwenden.
Problem
Die API ist nicht ganz eindeutig.

Bei einem DDD-Modell sollte nach Möglichkeit erkennbar sein, ob ein Wert als fachliches Value Object oder als einfacher String verwendet werden soll.

Besonders relevant ist das bei zukünftigen Erweiterungen von Titel, beispielsweise wenn weitere Regeln hinzukommen.
Empfohlene Maßnahme
Ich würde langfristig die typisierte API bevorzugen:
public Titel getTitelObjekt()

Die String-Repräsentation kann anschließend über das Value Object abgerufen werden:
abstimmung.getTitelObjekt().wert();

Noch konsistenter wäre beispielsweise eine API, bei der getTitel() direkt Titel liefert.

Das wäre allerdings eine Änderung des bisherigen Rückgabetyps und damit eine inkompatible API-Änderung.

Für den aktuellen Projektstand würde ich deshalb keine Getter umbenennen.

Stattdessen:
1. Die Titel-basierte Factory als bevorzugte Variante festlegen.
2. Die String-Überladung als Komfortmethode akzeptieren.
3. Beide Getter vorerst beibehalten.
4. Bei der nächsten größeren API-Überarbeitung eine eindeutige Konvention einführen.
Ein zusätzlicher Punkt: Ein Aufruf mit einem expliziten null-Literal als Titelargument ist wegen der beiden Überladungen nicht mehrdeutig, da Titel spezifischer als String ist. Er wählt die Titel-Überladung. Dennoch können sich die Fehlermeldungen beider Varianten unterscheiden, weshalb gezielte Tests sinnvoll sind.

Aufwand: 10–20 Minuten für Konvention und Dokumentation; 30–60 Minuten für eine tatsächliche API-Vereinheitlichung
Testrisiko: Gering bei Dokumentation, mittel bei API-Änderungen
C – Exception als Kontrollfluss
Schweregrad: Niedrig

Aktuell:
public static boolean istGueltigerTitel(String t) {
    try {
        new Titel(t);
        return true;
    } catch (IllegalArgumentException e) {
        return false;
    }
}

Die Implementierung hat einen klaren Vorteil:

Die Validierungsregeln befinden sich ausschließlich im Value Object Titel.

Dadurch vermeidest du doppelte Validierungslogik.

Allerdings gibt es auch Nachteile:
- Exceptions werden für erwartbare ungültige Eingaben verwendet.
- Bei häufigen ungültigen Eingaben kann unnötiger Aufwand entstehen.
- Der relativ allgemeine Catch kann auch andere IllegalArgumentException-Fehler aus der Konstruktion als bloß ungültigen Titel behandeln.
Empfohlene Maßnahme
Zunächst würde ich ausschließlich den Catch präzisieren:
try {
    new Titel(t);
    return true;
} catch (AbstimmungsregelVerletztException e) {
    return false;
}

Voraussetzung: Sämtliche erwartbaren Validierungsfehler des Titel-Konstruktors werden tatsächlich durch diese Exception signalisiert.

Damit verhinderst du, dass andere IllegalArgumentException-Fehler versehentlich verschluckt werden.

Langfristig wäre eine explizite Validierungsfunktion denkbar, beispielsweise:
Titel.istGueltig(t)

Diese sollte jedoch dieselben Regeln wie der Konstruktor verwenden und keine unabhängige zweite Regelimplementierung enthalten.

Aufwand: 5–10 Minuten für den präziseren Catch
Testrisiko: Gering

Ein größerer Umbau wäre nur sinnvoll, wenn die Validierungsfunktion sehr häufig aufgerufen wird oder du konkrete Probleme mit dem Exception-basierten Ansatz feststellst.
4. Priorisierung: Welche drei Maßnahmen zuerst?
Ich würde die Maßnahmen nicht nach der PMD-Priorität sortieren, sondern nach ihrem Verhältnis aus Nutzen, Aufwand und Änderungsrisiko.
Meine Top 3
Rang	Maßnahme	Nutzen	Aufwand	Risiko
1	hamcrest-all entfernen	Veraltete Abhängigkeit beseitigen	10–20 Min.	Gering
2	Maven Enforcer einrichten	Reproduzierbarkeit verbessern	15–30 Min.	Gering bis mittel
3	Exception-Vertrag vereinheitlichen bzw. dokumentieren	Domänenverhalten klarer definieren	15–25 Min.	Gering bis mittel

Platz 1: Hamcrest entfernen
Warum zuerst?

Die Abhängigkeit wird nur in zwei Tests verwendet. Sie lässt sich daher mit wenig Aufwand vollständig beseitigen.

Du reduzierst die Anzahl der Abhängigkeiten und vermeidest die weitere Verwendung einer nicht mehr gepflegten Bibliothek.

Die Änderung ist leicht überprüfbar.
Platz 2: Maven Enforcer einführen
Die Build-Umgebung ist eine wichtige Grundlage für die weitere Entwicklung.

Gerade in einem Studienprojekt, an dem möglicherweise mehrere Personen arbeiten, sind einheitliche Entwicklungsbedingungen hilfreich.

Mit dem Enforcer kannst du verhindern, dass unterschiedliche Java- oder Maven-Versionen zu schwer nachvollziehbaren Build-Problemen führen.
Platz 3: Exception-Vertrag klären
Dieser Punkt betrifft unmittelbar deine Domänenarchitektur.

Einheitlich definierte Fehlerverträge erleichtern die spätere Anbindung einer REST-API.

Du kannst dann bewusst unterscheiden, welche Exceptions durch fehlerhafte Benutzereingaben entstehen und welche auf Programmierfehler hinweisen.

Die übrigen Änderungen würde ich nachrangig behandeln.

Besonders wichtig: Ich würde keine größere Refaktorierung von Abstimmung durchführen, nur um die PMD-Komplexität zu reduzieren.
5. Ist die Komplexität von Abstimmung problematisch?
5.1 Zyklomatische Komplexität von 10
Meine Einschätzung: Die beiden Methoden sind für ihren konkreten Zweck noch angemessen komplex.

PMD betrachtet Werte von 8 bis 10 bereits als hohe Komplexität. Diese Einordnung ist aber eine allgemeine Heuristik, kein Beweis für ein schlechtes Design. PMD Code Documentation

Betrachten wir deine beiden Methoden.
erstelle()
Die Methode validiert die Eingaben und erzeugt ein gültiges Aggregat.

Die Bedingungen sind größtenteils unabhängig voneinander.
if (id == null) ...
if (erstelltVon == null) ...
if (titel == null) ...
if (zeitraum == null) ...

Das ist etwas anderes als eine tief verschachtelte Geschäftslogik.

Beispielsweise wäre folgende Struktur deutlich schwieriger zu verstehen:
if (bedingungA) {
    if (bedingungB) {
        if (bedingungC) {
            // Komplexe Geschäftslogik
        }
    }
}

Beide Konstruktionen erhöhen die zyklomatische Komplexität, unterscheiden sich aber erheblich in ihrer Verständlichkeit.

Die erste Variante ist einfacher zu lesen und zu testen.
pruefeOptionen()
Auch diese Methode ist fachlich überschaubar.

Sie prüft eine klar abgegrenzte Invariante: Eine Abstimmung muss eine gültige Liste unterschiedlicher Optionen enthalten.

Hier würde ich besonders auf die fachlichen Testfälle achten:
Testfall	Erwartetes Verhalten
Optionsliste ist null	Exception
Optionsliste ist leer	Exception
Nur eine Option	Exception
Genau zwei gültige Optionen	Erfolgreich
Drei gültige Optionen	Erfolgreich
Option enthält null	Exception
Zwei identische Optionen	Exception
Optionen unterscheiden sich nur durch Groß-/Kleinschreibung	Gemäß Normalisierungsregel
Optionen unterscheiden sich nur durch Leerzeichen	Gemäß Normalisierungsregel
Ursprungsliste wird nach Erstellung verändert	Aggregat bleibt unverändert

Bei den letzten beiden Normalisierungsfällen richtet sich das erwartete Ergebnis nach der konkreten Implementierung von normalisierterText().

Wenn diese Fälle bereits abgedeckt sind, spricht das zusätzlich dafür, die Methode unverändert zu lassen.
5.2 Würde eine Auslagerung die Komplexität verbessern?
Stell dir vor, du würdest die Prüfungen in mehrere Methoden aufteilen:
pruefeId(id);
pruefeErsteller(erstelltVon);
pruefeTitel(titel);
pruefeZeitraum(zeitraum);

Dadurch würde die zyklomatische Komplexität von erstelle() sinken.

Die Anzahl der erforderlichen Entscheidungen bleibt aber identisch.

Außerdem müsste ein Entwickler nun mehrere Methoden öffnen, um sämtliche Erstellungsregeln zu verstehen.

Eine solche Refaktorierung würde möglicherweise den PMD-Wert verbessern, ohne die Wartbarkeit zu erhöhen.

Deshalb würde ich die unmittelbaren Invariantenprüfungen in der Factory belassen.

Anders sieht es aus, wenn die Prüfungen fachlich umfangreicher werden.

Beispielsweise könnte eine komplexe Validierung von Abstimmungsoptionen eine eigene fachliche Komponente rechtfertigen.

Aktuell besteht dafür aber kein ausreichender Grund.
6. Parameter-Objekt oder Builder?
6.1 Parameter-Objekt
Eine mögliche Lösung wäre:
public record AbstimmungErstellen(
    AbstimmungId id,
    NutzerId erstelltVon,
    Titel titel,
    String beschreibung,
    Abstimmungszeitraum zeitraum,
    List<AbstimmOption> optionen
) {}

Die Factory hätte anschließend nur noch einen Parameter:
Abstimmung.erstelle(daten);

Das sieht auf den ersten Blick sauberer aus.

Allerdings verschwindet die Komplexität nicht.

Du führst lediglich eine zusätzliche Klasse ein, die sechs vorhandene Eigenschaften zusammenfasst.
Wann wäre das sinnvoll?
Wenn der Erstellungsprozess umfangreicher wird oder dieselben Eingabedaten an mehreren Stellen benötigt werden.

Beispielsweise:
- REST-Endpunkt
- Anwendungsservice
- Wiederverwendbarer Anwendungs-Command
- Umfangreiche Testdaten
Ein Parameter-Objekt kann dann als Command im Application Layer sinnvoll sein.

Dabei sollte jedoch die Frameworkunabhängigkeit der Domäne erhalten bleiben.
Für dein Projekt
Momentan würde ich darauf verzichten.

Die sechs Parameter sind gut verständlich und repräsentieren unmittelbar die Eigenschaften des Aggregats.

Ein Parameter-Objekt würde aktuell eher zusätzliche Abstraktion als tatsächlichen Nutzen erzeugen.
6.2 Builder
Ein Builder könnte so aussehen:
Abstimmung abstimmung = Abstimmung.builder()
    .id(id)
    .erstelltVon(nutzerId)
    .titel(titel)
    .beschreibung(beschreibung)
    .zeitraum(zeitraum)
    .optionen(optionen)
    .build();

Der Vorteil liegt in der Lesbarkeit der Argumentzuordnung.

Allerdings entstehen zusätzliche Probleme:
- Mehr Implementierungscode.
- Zusätzlicher Testaufwand.
- Ein Builder kann unvollständige Zwischenzustände besitzen.
- Fachliche Invarianten müssen weiterhin beim Erzeugen des Aggregats geprüft werden.
Gerade der letzte Punkt ist wichtig.

Ein Builder ersetzt keine fachliche Validierung.
Meine Entscheidung
Variante	Bewertung für votee
Bestehende Factory	Empfohlen
Parameter-Objekt	Aktuell nicht notwendig
Builder	Aktuell Overengineering
Aufteilung der Factory in viele Hilfsmethoden	Kein erkennbarer Mehrwert

Für ein kleines DDD-Aggregat mit sechs klar definierten Eigenschaften ist deine Factory eine vernünftige Lösung.
7. Warum melden SonarQube und PMD unterschiedlich viele Probleme?
Das ist einer der wichtigsten Punkte deiner gesamten Technical-Debt-Analyse.

Du hast:

SonarQube: 0 Issues

PMD: 8 Meldungen

Trotzdem analysieren beide Werkzeuge weitgehend denselben Quellcode.

Der Unterschied entsteht vor allem durch ihre Regeln, Konfigurationen und Bewertungsverfahren.
7.1 Unterschiedliche Regelwerke
SonarQube verwendet das Quality Profile „Sonar way“.

PMD verwendet dein eigenes Regelwerk mit den Kategorien:
- bestpractices
- errorprone
- design
Außerdem hast du eigene Schwellenwerte definiert.

Damit untersuchen die Werkzeuge zwar ähnliche Qualitätsmerkmale, verwenden aber nicht dieselben Regeln.

Ein Befund kann daher bei PMD erscheinen und bei SonarQube ausbleiben.

Das ist kein Widerspruch.
7.2 Unterschiedliche Komplexitätsbewertung
SonarQube und PMD verwenden nicht zwangsläufig dieselben Komplexitätsmetriken und Grenzwerte.

Besonders wichtig ist die Unterscheidung zwischen:

Zyklomatischer Komplexität: Wie viele unabhängige Kontrollflusspfade existieren?

Kognitiver Komplexität: Wie schwierig ist der Kontrollfluss für Menschen nach einer bestimmten Metrik zu verstehen?

Beispiel:
if (a == null) throw new IllegalArgumentException();
if (b == null) throw new IllegalArgumentException();
if (c == null) throw new IllegalArgumentException();

Die zyklomatische Komplexität steigt durch jede Bedingung.

Die kognitive Schwierigkeit bleibt dagegen relativ gering, weil keine Verschachtelung entsteht.

Deshalb können beide Werkzeuge denselben Code unterschiedlich bewerten.

Das bedeutet nicht, dass eines der Werkzeuge falsch arbeitet.
7.3 Unterschiedliche Interpretationen technischer Schuld
Technical Debt ist nicht einfach die Anzahl der gemeldeten Probleme.

Beispielsweise:
Befund	Tool-Meldung	Tatsächliche Schuld
Ungenutzter Helidon-Parameter	Ja	Nein
Fehlende serialVersionUID	Ja	Sehr gering
Hohe Komplexität durch Invarianten	Ja	Nicht zwangsläufig
Veraltete Testbibliothek	Nicht zwingend	Ja
Uneinheitlicher Exception-Vertrag	Nicht zwingend	Potenziell ja

Dieser Vergleich zeigt:

Ein statisches Analysewerkzeug kann auf mögliche technische Schuld hinweisen. Es kann diese aber nicht vollständig beurteilen.

Eine solche Bewertung benötigt zusätzlich fachliches und architektonisches Verständnis.
7.4 Was bedeutet „0 Issues“ bei SonarQube?
Ein Ergebnis von null Issues bedeutet:
Im analysierten Code wurden unter Berücksichtigung des aktiven Quality Profiles keine aktiven Regelverletzungen festgestellt.

Es bedeutet nicht:
- Der Code ist vollständig wartbar.
- Es existiert keine technische Schuld.
- Die Architektur ist optimal.
- Alle fachlichen Regeln sind richtig umgesetzt.
- Es existieren keine Sicherheitslücken.
Auch ein bestandenes Quality Gate ist zunächst nur die Aussage, dass die konfigurierten Qualitätsbedingungen erfüllt wurden.

Es ist kein allgemeines Qualitätszertifikat.
8. Was sagen Technical-Debt-Kennzahlen tatsächlich aus?
Ich würde die Kennzahlen in deinem Studienprojekt folgendermaßen interpretieren.
Kennzahl	Aussagekraft	Einschränkung
SonarQube-Issues	Erkannte Regelverletzungen	Abhängig vom Quality Profile
PMD-Meldungen	Mögliche Qualitätsprobleme	Enthalten auch Fehlalarme
Cyclomatic Complexity	Komplexität des Kontrollflusses	Berücksichtigt fachliche Notwendigkeit nicht
Cognitive Complexity	Geschätzte Verständlichkeit des Kontrollflusses	Bildet nicht das gesamte Design ab
Code-Duplikation	Erkannte identische oder ähnliche Codeabschnitte	Nicht jede Duplikation ist schlecht
Test-Coverage	Ausgeführter Code	Misst nicht die Qualität der Assertions
Veraltete Abhängigkeiten	Aktualisierungsbedarf	Nicht jede neuere Version ist besser
Geschätzter Behebungsaufwand	Aufwand innerhalb definierter Regeln	Kein vollständiger wirtschaftlicher Schuldenwert

Wichtige Unterscheidung
Eine Codeänderung kann die Metrik verbessern und trotzdem die Architektur verschlechtern.

Beispielsweise:

Vorher:
if (optionen == null) throw ...;
if (optionen.size() < 2) throw ...;

Nachher:
pruefeOptionsliste(optionen);

Die zweite Variante reduziert die lokale Komplexität.

Sie verbessert aber nicht zwangsläufig den Code.

Der tatsächliche Nutzen hängt davon ab, ob durch die Auslagerung eine verständlichere Struktur entsteht.
Technical Debt als wirtschaftliches Konzept
Technische Schuld wird besonders relevant, wenn eine heutige technische Entscheidung zukünftige Änderungen erschwert oder verteuert.

Beispiele aus deinem Projekt:

Veraltete Hamcrest-Abhängigkeit: Eine alte Bibliothek muss langfristig berücksichtigt oder ersetzt werden.

Fehlende Build-Anforderungen: Teammitglieder könnten Zeit mit der Fehlersuche bei ungeeigneten Entwicklungsumgebungen verlieren.

Uneindeutiger Exception-Vertrag: Spätere Implementierungen könnten Fehler unterschiedlich behandeln.

Bei den Komplexitätsmeldungen lässt sich dagegen aus den Zahlen allein keine vergleichbare zusätzliche Wartungslast nachweisen.

Daher wäre es falsch, alle acht PMD-Meldungen gleichwertig als technische Schuld zu zählen.
9. Konkreter Maßnahmenplan
Für dein Studienprojekt würde ich einen kleinen, kontrollierten Maßnahmenplan erstellen.
Reihenfolge	Maßnahme	Aufwand	Priorität
1	Hamcrest entfernen	10–20 Min.	Mittel
2	Maven Enforcer konfigurieren	15–30 Min.	Mittel
3	Exception-Vertrag prüfen und dokumentieren	15–25 Min.	Mittel
4	Logging statt System.out.println()	5–10 Min.	Niedrig
5	serialVersionUID ergänzen	2 Min.	Niedrig
6	Helidon-Patch prüfen und aktualisieren	10–20 Min.	Niedrig
7	H2-Verwendung prüfen	5–15 Min.	Niedrig
8	maven-site-plugin-Version festlegen oder Plugin entfernen	5 Min.	Niedrig
9	Exception-Catch im Titelvalidator präzisieren	5–10 Min.	Niedrig
10	Titel-API-Konvention dokumentieren	10–20 Min.	Niedrig

Gesamtaufwand: Etwa 1,5 bis 3 Stunden für die empfohlenen kleineren Maßnahmen, einschließlich kurzer Überprüfungsschritte.

Die konkrete Dauer hängt davon ab, ob sich bei den Abhängigkeiten oder beim Exception-Vertrag zusätzliche Anpassungen ergeben.
Was ich bewusst nicht verändern würde
Folgende Bestandteile würde ich vorerst unverändert lassen:
- Abstimmung.erstelle() mit sechs Parametern.
- pruefeOptionen() mit der expliziten Schleife.
- Die Verantwortung des Aggregate Roots für seine Invarianten.
- Die beiden Factory-Überladungen, solange sie sinnvoll verwendet werden.
- Die beiden Titel-Getter, solange keine größere API-Überarbeitung geplant ist.
- Den ungenutzten Parameter des Helidon-Handlers.
Eine kleine Korrektur zu Abschnitt 3B: Da String und Titel zwei nicht miteinander verwandte Referenztypen sind, wäre erstelle(id, nutzer, null, ...) tatsächlich mehrdeutig und würde nicht kompilieren. Bei einer expliziten Typumwandlung oder einer entsprechend typisierten Variablen ist die Überladung eindeutig. Das ist ein weiterer kleiner Nachteil der doppelten Factory-Signatur, aber kein ausreichender Grund für einen sofortigen Umbau.
10. Gesamtfazit für die Studienarbeit
Deine Analyse eignet sich gut, um zwischen automatisch erkannten Qualitätsauffälligkeiten und tatsächlich relevanter technischer Schuld zu unterscheiden.

Ich würde die Ergebnisse in deiner Dokumentation folgendermaßen zusammenfassen:
Bewertung der technischen Schuld
Das Projekt eVote „votee“ weist zum Zeitpunkt der Untersuchung eine geringe technische Schuld auf. Die statische Codeanalyse mit SonarQube ergab keine Regelverletzungen und ein bestandenes Quality Gate. PMD identifizierte acht Auffälligkeiten, deren fachliche Überprüfung jedoch zeigte, dass mehrere Meldungen auf bewusste Architekturentscheidungen oder notwendige Schnittstellenvorgaben zurückzuführen sind.

Insbesondere die erhöhte zyklomatische Komplexität des Aggregate Roots Abstimmung entsteht überwiegend durch die Überprüfung fachlicher Invarianten. Eine Refaktorierung allein zur Reduzierung dieser Kennzahl wird daher nicht als sinnvoll erachtet. Auch die Verwendung von sechs Parametern in den Factory-Methoden rechtfertigt beim aktuellen Projektumfang keine zusätzliche Abstraktion.

Tatsächlicher Verbesserungsbedarf besteht vor allem bei der Pflege älterer Abhängigkeiten, der Absicherung reproduzierbarer Builds und der konsistenten Definition von Fehlerverträgen innerhalb der Domäne. Die identifizierten Maßnahmen lassen sich größtenteils mit geringem Aufwand und begrenztem Änderungsrisiko umsetzen.

Die Untersuchung zeigt damit auch die Grenzen automatisierter Qualitätsmetriken. Während SonarQube und PMD wertvolle Hinweise liefern, erfordert die Bewertung technischer Schuld zusätzlich eine Betrachtung der fachlichen Anforderungen, der Architektur und der zukünftigen Wartungskosten.
Abschließende Empfehlung
Ich würde deinen bestehenden Domain-Code weitgehend beibehalten.

Das Projekt ist bereits gut getestet, die fachlichen Verantwortlichkeiten sind nachvollziehbar und die gemeldete Komplexität ist für die Aufgabe des Aggregats vertretbar.

Der größte Mehrwert liegt derzeit nicht in einer umfassenden Refaktorierung, sondern in der gezielten Beseitigung kleiner Wartungs- und Build-Risiken.

Für deine Studienarbeit ist zudem eine wichtige Erkenntnis entstanden:

Codequalität lässt sich nicht allein aus der Anzahl der Tool-Meldungen oder der Höhe einzelner Metriken ableiten. Entscheidend ist, ob die identifizierten Auffälligkeiten zukünftige Änderungen tatsächlich erschweren.

Genau diese Unterscheidung zwischen gemessener Auffälligkeit und begründetem Handlungsbedarf macht deine Technical-Debt-Bewertung nachvollziehbar.
~~~

