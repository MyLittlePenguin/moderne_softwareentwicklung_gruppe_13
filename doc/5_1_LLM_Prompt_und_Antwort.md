# 5.1 LLM-Prompt und Antwort

**LLM:** ChatGPT GPT-6 Medium, 18.11.2026, neuer Chat ohne Vorwissen. Eingefügt: Abstimmung.java, Abstimmungszeitraum.java, Titel.java (Stand main e53be0c).

## Prompt

---

Du bist ein erfahrener Java-Reviewer. Ich analysiere die Codequalität meines Projekts mit Metriken und möchte von dir **Optimierungs- und Vereinfachungsvorschläge**, die sich aus diesen Metriken ableiten. Ändere den Code noch nicht vollständig, sondern schlage gezielte Maßnahmen vor.

**Projekt:** eVote „votee“, Online-Abstimmungssystem. Java 27, Domain-Driven Design, Domänenschicht ohne Framework. Bounded Context „Abstimmungsverwaltung“, Aggregate Root `Abstimmung`, Value Objects als Java records.

**Metriken (SonarQube Community, 18.11.2026):**

- Gesamt: 464 Lines of Code, 11 Klassen, 43 Methoden, Kommentaranteil 16,7 %, Cyclomatic Complexity 83, Cognitive Complexity 43.
- Cyclomatic Complexity pro Datei: `Abstimmung.java` 35, `Abstimmungszeitraum.java` 11, `Titel.java` 11, `AbstimmungEingabeValidator.java` 5, alle anderen ≤ 4.
- `Abstimmung.java`: 136 LOC, 21 der 43 Methoden; Konstruktor und zwei überladene Factory-Methoden `erstelle(...)` mit je **6 Parametern** (alle anderen Methoden im Projekt ≤ 2 Parameter).

**Fragen:**

1. Welche Stellen sind laut diesen Metriken am auffälligsten, und warum?
2. Schlage für jede auffällige Stelle konkrete Optimierungen oder Vereinfachungen vor (z. B. Methoden modularisieren, Parameter bündeln, Duplikate zusammenführen). Zeige kurze Code-Skizzen.
3. Bewerte jeden Vorschlag: Nutzen, Risiko, Aufwand, und ob er zu DDD passt (Fachregeln bleiben in der Aggregate Root bzw. im Value Object).
4. Welche Werte sind für ein Projekt dieser Größe **unproblematisch** und brauchen keine Änderung?

**Code:**

```java
// Abstimmung.java
<DATEI EINFÜGT>
```

```java
// Abstimmungszeitraum.java
<DATEI EINFÜGT>
```

```java
// Titel.java
<DATEI EINFÜGT>
```

_(Beim Prompt wurden an den drei Platzhaltern die Dateien aus `main` @ `e53be0c` eingefügt.)_

## Antwort (ungekürzt, ChatGPT GPT-6 Medium, 18.11.2026)

```text
Code-Review: eVote „votee“ – Auswertung der SonarQube-Metriken
Projekt: eVote „votee“
Technologie: Java 27, Domain-Driven Design (DDD)
Bounded Context: Abstimmungsverwaltung
Analysegrundlage: SonarQube Community, 18.11.2026, sowie die drei bereitgestellten Java-Dateien.

1. Gesamtbewertung
Mein Fazit: Die Domänenschicht wirkt bereits gut strukturiert. Ein grundlegendes Refactoring ist nicht erforderlich. Es gibt jedoch einige Stellen, an denen sich die Lesbarkeit, Wartbarkeit und Konsistenz verbessern lassen.

Besonders wichtig ist, dass die Optimierungen nicht nur auf eine Reduzierung der SonarQube-Werte abzielen. Eine niedrigere Komplexität ist nicht automatisch gleichbedeutend mit besserem Code.

Bei DDD müssen vor allem die fachlichen Verantwortlichkeiten erhalten bleiben.

Bewertung der Projektmetriken
Metrik                     | Wert   | Bewertung                 | Handlungsbedarf
Lines of Code              | 464    | Kleine Codebasis          | Keiner
Klassen                    | 11     | Überschaubare Struktur    | Keiner
Methoden                   | 43     | Unauffällig               | Keiner
Kommentaranteil            | 16,7 % | Unproblematisch           | Gering
Cyclomatic Complexity      | 83     | Insgesamt moderat         | Gezielt prüfen
Cognitive Complexity       | 43     | Insgesamt unauffällig     | Gering
Höchste Datei-Komplexität  | 35     | Auffällig, aber erklärbar | Mittel
Höchste Parameteranzahl    | 6      | Verbesserungsmöglichkeit  | Mittel

Die gesamte Cyclomatic Complexity von 83 verteilt sich auf 43 Methoden. Rechnerisch ergibt sich ein Durchschnitt von etwa 1,93.
Das ist zunächst ein guter Wert.
Allerdings können Durchschnittswerte einzelne komplexe Methoden verdecken. Deshalb ist die Betrachtung der einzelnen Dateien und Methoden entscheidend.

2. Abstimmung.java – höchste Priorität
Metriken:
- 136 LOC
- 21 Methoden
- Cyclomatic Complexity: 35
- Konstruktor mit 6 Parametern
- Zwei überladene Factory-Methoden mit jeweils 6 Parametern
Abstimmung.java verursacht rund 42 % der gesamten Cyclomatic Complexity.
Das macht die Klasse zum wichtigsten Kandidaten für eine genauere Untersuchung.
Allerdings ist sie die Aggregate Root. Sie soll zentrale Fachregeln kontrollieren und ist damit erwartungsgemäß komplexer als einfache Value Objects.

2.1 Factory-Methoden mit sechs Parametern
Ist-Zustand
public static Abstimmung erstelle(
        AbstimmungId id,
        NutzerId erstelltVon,
        Titel titel,
        String beschreibung,
        Abstimmungszeitraum zeitraum,
        List<AbstimmOption> optionen) {
    // Validierung
}
Es existiert zusätzlich eine überladene Methode, die einen String statt eines Titel-Objekts annimmt.
Diese delegiert bereits an die zentrale Factory-Methode.
Wichtig: Hier liegt keine nennenswerte Duplikation der Validierungslogik vor. Die Überladung ist sauber umgesetzt.

Problem
Sechs Parameter sind nicht automatisch falsch.
Sie erschweren aber die Verwendung der Factory-Methode und machen zukünftige Erweiterungen aufwendiger.
Bei der Abstimmung gehören mehrere dieser Werte fachlich zusammen.

Vorschlag A: Parameter in einem Record bündeln
Java 27 bietet mit Records eine kompakte Möglichkeit, zusammengehörige Daten zu bündeln.
Beispiel:
public record AbstimmungErstellung(
        AbstimmungId id,
        NutzerId erstelltVon,
        Titel titel,
        String beschreibung,
        Abstimmungszeitraum zeitraum,
        List<AbstimmOption> optionen
) {}
Die Factory-Methode erhält anschließend nur noch einen Parameter.
public static Abstimmung erstelle(
        AbstimmungErstellung eingabe) {
    // Bestehende Fachregeln bleiben erhalten.
    return new Abstimmung(
            eingabe.id(),
            eingabe.erstelltVon(),
            eingabe.titel(),
            eingabe.beschreibung(),
            eingabe.zeitraum(),
            eingabe.optionen()
    );
}
Diese Skizze zeigt nur die Parameterbündelung. Vor dem Aufruf des Konstruktors müssen weiterhin alle bisherigen Fachregeln geprüft werden. Insbesondere dürfen Beschreibung und Optionsliste nicht ungeprüft übernommen werden.

Bewertung
Nutzen: Bessere Lesbarkeit und einfachere Erweiterbarkeit
Risiko: Gering bis mittel
Aufwand: Gering bis mittel
DDD-Konformität: Ja, wenn das Record nur Eingabedaten bündelt
Priorität: Mittel

Meine Empfehlung:
Diese Änderung ist sinnvoll, wenn die Abstimmung künftig weitere Eigenschaften erhalten soll oder zahlreiche Aufrufstellen existieren.
Für eine kleine und stabile Domänenschicht sind sechs Parameter allerdings noch vertretbar.
Wichtig ist außerdem, das Record nicht als Ersatz für die Aggregate Root zu verwenden. Die fachliche Validierung bleibt bei Abstimmung.

2.2 Factory-Methode modularisieren
Die zentrale Factory-Methode enthält mehrere unabhängige Prüfungen:
1. AbstimmungId vorhanden?
2. NutzerId vorhanden?
3. Titel vorhanden?
4. Beschreibung normalisieren.
5. Abstimmungszeitraum vorhanden?
6. Abstimmungsoptionen prüfen.
Die Validierung der Optionen ist bereits in eine separate Methode pruefeOptionen() ausgelagert.
Das ist positiv.

Verbesserungsvorschlag
Die Prüfung notwendiger Referenzen könnte in eine eigene Methode ausgelagert werden.
private static void pruefePflichtfelder(
        AbstimmungId id,
        NutzerId erstelltVon,
        Titel titel,
        Abstimmungszeitraum zeitraum) {
    if (id == null) {
        throw new AbstimmungsregelVerletztException("AbstimmungId muss angegeben werden.");
    }
    if (erstelltVon == null) {
        throw new AbstimmungsregelVerletztException("Ersteller muss angegeben werden.");
    }
    if (titel == null) {
        throw new AbstimmungsregelVerletztException("Titel darf nicht leer sein.");
    }
    if (zeitraum == null) {
        throw new AbstimmungsregelVerletztException("Abstimmungszeitraum muss angegeben werden.");
    }
}
Anschließend:
public static Abstimmung erstelle(
        AbstimmungId id,
        NutzerId erstelltVon,
        Titel titel,
        String beschreibung,
        Abstimmungszeitraum zeitraum,
        List<AbstimmOption> optionen) {
    pruefePflichtfelder(id, erstelltVon, titel, zeitraum);
    String bereinigteBeschreibung =
            beschreibung == null ? "" : beschreibung.strip();
    List<AbstimmOption> gepruefteOptionen =
            pruefeOptionen(optionen);
    return new Abstimmung(id, erstelltVon, titel, bereinigteBeschreibung, zeitraum, gepruefteOptionen);
}

Bewertung
Nutzen: Kürzere Factory-Methode, klarere Verantwortlichkeiten
Risiko: Gering
Aufwand: Gering
DDD-Konformität: Vollständig
Priorität: Niedrig bis mittel

Einschränkung: Diese Änderung reduziert nicht zwingend die gesamte Cyclomatic Complexity.
Die Verzweigungen werden hauptsächlich auf zwei Methoden verteilt.
Das ist kein Problem, denn das Ziel sollte bessere Verständlichkeit sein und nicht die künstliche Verbesserung einer Metrik.
Außerdem entsteht eine weitere Methode mit vier Parametern. Deshalb würde ich diese Änderung nur durchführen, wenn die Lesbarkeit tatsächlich gewinnt.

2.3 Duplikate bei den Abstimmungsoptionen prüfen
Die Methode pruefeOptionen() kontrolliert mehrere Fachregeln.
Set<String> normalisierteTexte = new HashSet<>();
for (AbstimmOption option : optionen) {
    if (option == null) {
        throw new AbstimmungsregelVerletztException("Option darf nicht null sein.");
    }
    if (!normalisierteTexte.add(option.normalisierterText())) {
        throw new AbstimmungsregelVerletztException("Doppelte Option gefunden: " + option.text());
    }
}

Bewertung des bestehenden Codes
Hier sehe ich keinen unmittelbaren Optimierungsbedarf.
Die Implementierung besitzt mehrere Stärken:
- Duplikate werden anhand normalisierter Texte erkannt.
- Die Prüfung wird beim ersten Duplikat beendet.
- Die Fehlermeldung enthält die betroffene Option.
- Mit List.copyOf() wird eine unveränderliche Kopie erstellt.
- Die Fachregel bleibt innerhalb der Aggregate Root.
Die Cyclomatic Complexity entsteht hier durch nachvollziehbare fachliche Bedingungen.
Eine Umstellung auf Streams würde den Code nicht automatisch verbessern.
Empfehlung: Beibehalten.
Allenfalls könnte zukünftig geprüft werden, ob die Behandlung von Duplikaten in einem eigenen domänenspezifischen Typ sinnvoll wäre.
Für das aktuelle Projekt wäre das aber vermutlich unnötige zusätzliche Architektur.

2.4 Die Methode hatOption() vereinfachen
Momentan gibt es zwei überladene Methoden.
public boolean hatOption(AbstimmOption option) {
    if (option == null) {
        return false;
    }
    String gesucht = option.normalisierterText();
    return optionen.stream()
            .map(AbstimmOption::normalisierterText)
            .anyMatch(gesucht::equals);
}
Zusätzlich:
public boolean hatOption(String text) {
    if (text == null || text.isBlank()) {
        return false;
    }
    return hatOption(new AbstimmOption(text));
}
Die Überladung ist grundsätzlich sinnvoll.
Allerdings könnte die erste Methode etwas direkter formuliert werden.
Vorschlag
public boolean hatOption(AbstimmOption option) {
    if (option == null) {
        return false;
    }
    return optionen.stream()
            .anyMatch(vorhanden ->
                    vorhanden.normalisierterText()
                            .equals(option.normalisierterText()));
}
Dadurch entfällt die zusätzliche map()-Operation.

Bewertung
Nutzen: Etwas kompakterer Code
Risiko: Gering
Aufwand: Sehr gering
DDD-Konformität: Ja
Priorität: Niedrig

Allerdings ist die bestehende Implementierung ebenfalls gut lesbar.
Die vorgeschlagene Änderung ist daher optional.
Wichtiger wäre ein Test, der sicherstellt, dass sich die beiden Methoden bei gültigen Eingaben identisch verhalten.
Zusätzlicher Prüfpunkt: Die String-Überladung erzeugt ein neues AbstimmOption-Objekt. Falls ein nicht leerer Text gegen weitere Regeln dieses Value Objects verstößt, kann die Methode eine Exception auslösen, obwohl ihr Rückgabetyp boolean möglicherweise ein einfaches false erwarten lässt.
Ob dies geändert werden sollte, hängt vom gewünschten fachlichen Vertrag ab. Dafür müsste auch AbstimmOption.java untersucht werden.

3. Abstimmungszeitraum.java – mittlere Priorität
Metriken:
- Cyclomatic Complexity: 11
- Ein Record
- Drei öffentliche Prüfmethoden
- Validierung im Konstruktor
Die Klasse ist klein, besitzt aber mehrere Bedingungen.
Das liegt hauptsächlich daran, dass jede Methode den übergebenen Zeitpunkt auf null überprüft.

3.1 Wiederholte Null-Prüfungen
Aktuell:
public boolean enthaelt(LocalDateTime zeitpunkt) {
    if (zeitpunkt == null) {
        throw new IllegalArgumentException("Zeitpunkt darf nicht null sein.");
    }
    return !zeitpunkt.isBefore(beginn) && !zeitpunkt.isAfter(ende);
}
Die gleiche Prüfung befindet sich in:
istVorbei(...)
istInZukunft(...)
Hier liegt echte Duplikation vor.

Vorschlag: Gemeinsame Prüfung
private static void pruefeZeitpunkt(LocalDateTime zeitpunkt) {
    if (zeitpunkt == null) {
        throw new IllegalArgumentException("Zeitpunkt darf nicht null sein.");
    }
}
Anschließend:
public boolean enthaelt(LocalDateTime zeitpunkt) {
    pruefeZeitpunkt(zeitpunkt);
    return !zeitpunkt.isBefore(beginn) && !zeitpunkt.isAfter(ende);
}
public boolean istVorbei(LocalDateTime zeitpunkt) {
    pruefeZeitpunkt(zeitpunkt);
    return zeitpunkt.isAfter(ende);
}
public boolean istInZukunft(LocalDateTime zeitpunkt) {
    pruefeZeitpunkt(zeitpunkt);
    return zeitpunkt.isBefore(beginn);
}

Bewertung
Nutzen: Weniger Duplikation
Risiko: Sehr gering
Aufwand: Sehr gering
DDD-Konformität: Vollständig
Priorität: Mittel

Die Fachregeln bleiben im Value Object.
Eine Alternative wäre Objects.requireNonNull(). Das würde allerdings standardmäßig eine NullPointerException statt der bisherigen IllegalArgumentException auslösen.
Ich würde deshalb die gemeinsame Hilfsmethode bevorzugen, wenn die bestehende Exception-Semantik erhalten bleiben soll.

3.2 Fachliche Randbedingung überprüfen
In Abstimmungszeitraum gibt es einen Punkt, der wichtiger ist als die reine Komplexität.
Die Methode enthaelt() berücksichtigt Beginn und Ende einschließlich.
return !zeitpunkt.isBefore(beginn) && !zeitpunkt.isAfter(ende);
Eine Abstimmung ist also exakt zum Endzeitpunkt noch aktiv.
istVorbei() liefert dagegen erst nach dem Ende true.
return zeitpunkt.isAfter(ende);
Das ist logisch konsistent.

Fachliche Frage
Soll eine Stimme exakt um 18:00:00 Uhr noch abgegeben werden dürfen, wenn die Abstimmung um 18:00:00 Uhr endet?
Wenn das fachlich gewollt ist, sollte die Implementierung unverändert bleiben.
Falls die Frist exklusiv sein soll, müsste die Grenzbedingung angepasst werden.
Ich würde hier kein Refactoring durchführen, ohne diese Fachregel ausdrücklich festzulegen.
Gerade bei Abstimmungssystemen ist die exakte Behandlung von Fristen wichtiger als eine geringfügige Verbesserung der Komplexitätsmetriken.

4. Titel.java – mittlere Priorität
Metriken:
- Cyclomatic Complexity: 11
- Ein Record
- Validierung und Normalisierung
- Private Hilfsmethode stripSpaces()
Die Komplexität entsteht insbesondere durch die Prüfung der Zeichenlänge, das reguläre Ausdrucksmuster und die Schleifen zur Entfernung führender und abschließender Leerzeichen.

4.1 Die Methode stripSpaces() untersuchen
Aktuelle Implementierung:
private static String stripSpaces(String str) {
    int start = 0;
    while (start < str.length() && str.charAt(start) == ' ') {
        start++;
    }
    int end = str.length();
    while (end > start && str.charAt(end - 1) == ' ') {
        end--;
    }
    return str.substring(start, end);
}
Die Methode entfernt ausdrücklich nur normale Leerzeichen am Anfang und Ende.
Andere Zeichen wie Tabs oder Zeilenumbrüche bleiben erhalten und werden anschließend durch das reguläre Ausdrucksmuster abgelehnt.

Vereinfachungsmöglichkeit
Wenn die Fachregel lediglich vorsieht, Leerzeichen an den Rändern zu entfernen, könnte man eine Standardmethode verwenden.
String bereinigt = normalisiert.strip();
Dadurch könnte stripSpaces() vollständig entfallen.
Aber: Achtung!
strip() entfernt mehr Arten von Leerraum als die bestehende Methode.
Damit würde sich das Verhalten verändern.
Beispielsweise würden Tabs am Anfang eines Titels entfernt, statt anschließend eine Validierungsausnahme auszulösen.

Bewertung
Nutzen: Weniger eigener Code, geringere Komplexität
Risiko: Mittel, weil sich die Validierung verändert
Aufwand: Sehr gering
DDD-Konformität: Ja, wenn die Fachregel angepasst werden darf
Priorität: Niedrig

Meine Empfehlung:
Die bestehende Methode zunächst beibehalten.
Sie ist nicht besonders lang und dokumentiert ein bewusst gewähltes Verhalten.
Eine Reduzierung der Cyclomatic Complexity rechtfertigt keine unbeabsichtigte Veränderung einer Fachregel.

4.2 Validierung im Record beibehalten
Der Konstruktor von Titel übernimmt folgende Aufgaben:
1. Null- und Leerprüfung
2. Unicode-Normalisierung nach NFC
3. Entfernung äußerer Leerzeichen
4. Mindestlänge prüfen
5. Maximallänge prüfen
6. Erlaubte Zeichen prüfen
Diese Verantwortung gehört in das Value Object.
Empfehlung: Keine Auslagerung in einen allgemeinen Validator.
Die Validierung ist zentraler Bestandteil des Fachmodells.
Wenn diese Prüfungen in eine separate technische Utility-Klasse verschoben würden, könnte das Value Object seine eigenen Invarianten nicht mehr unmittelbar gewährleisten.
Das widerspräche dem Ziel eines eigenständigen, frameworkfreien Domänenmodells.
Eine Aufteilung in kleine private Methoden wäre technisch möglich, bringt bei dieser Klassengröße aber wenig Mehrwert.

5. AbstimmungEingabeValidator.java – geringe Priorität
Cyclomatic Complexity: 5
Dieser Wert ist niedrig.
Außerdem liegt die Implementierung dieser Klasse nicht unter den bereitgestellten Dateien.
Deshalb lässt sich nicht beurteilen, ob darin tatsächlich unnötige Verzweigungen oder redundante Prüfungen existieren.
Ein wichtiger architektonischer Punkt sollte dennoch überprüft werden.

Mögliche doppelte Validierung
Es gibt zwei unterschiedliche Arten von Validierung.
Eingabevalidierung: Prüft beispielsweise, ob eine HTTP-Anfrage syntaktisch vollständig und korrekt aufgebaut ist.
Domänenvalidierung: Prüft, ob eine Abstimmung die fachlichen Regeln erfüllt.
Beispiel:
// Eingabevalidierung
if (eingabe == null) {
    // Ungültige Anfrage behandeln
}
// Domänenvalidierung
if (optionen.size() < 2) {
    throw new AbstimmungsregelVerletztException("Mindestens zwei Optionen erforderlich.");
}
Die zweite Prüfung muss in der Domäne abgesichert bleiben, unabhängig davon, ob bereits eine Eingabeprüfung stattgefunden hat.

Bewertung
Nutzen: Klare Trennung technischer und fachlicher Prüfungen
Risiko: Mittel, falls Regeln versehentlich entfernt werden
Aufwand: Gering bis mittel
DDD-Konformität: Ja
Priorität: Niedrig

Empfehlung: Den Validator vorerst nicht ändern, sondern prüfen, ob er fachliche Regeln dupliziert und welche Schicht ihn verwendet.

6. Methodenanzahl und vermeintliche Duplikation
Ein weiterer auffälliger Wert sind die 21 Methoden in Abstimmung.java.
Das sind knapp 49 % aller Projektmethoden.
Auf den ersten Blick könnte man vermuten, dass die Klasse zu viele Verantwortlichkeiten übernimmt.
Der tatsächliche Code zeigt jedoch etwas anderes.
Viele Methoden sind einfache Abfragen.
Beispiel:
public boolean istLaufend(LocalDateTime zeitpunkt) {
    return zeitraum.enthaelt(zeitpunkt);
}
public boolean kannAbgestimmtWerden(LocalDateTime zeitpunkt) {
    return istLaufend(zeitpunkt);
}
Beide Methoden liefern aktuell denselben Wert.
Trotzdem würde ich sie nicht automatisch zusammenführen.

Warum?
Die Methoden drücken unterschiedliche fachliche Bedeutungen aus:
- istLaufend() beschreibt den zeitlichen Zustand.
- kannAbgestimmtWerden() beschreibt eine fachliche Berechtigung.
- kannErgebnisEingesehenWerden() beschreibt eine andere fachliche Berechtigung.
Das ist bei DDD sinnvoll.
Später könnte beispielsweise gelten:
public boolean kannAbgestimmtWerden(LocalDateTime zeitpunkt) {
    return istLaufend(zeitpunkt) && !istGesperrt();
}
istGesperrt() ist hier lediglich ein hypothetisches Beispiel, keine vorhandene Projektmethode.
Die fachliche Bedeutung wäre dann nicht mehr identisch mit istLaufend().
Empfehlung: Fachlich sinnvolle Methoden beibehalten, auch wenn sie momentan denselben Wert zurückgeben.
Die aktuelle Delegation ist außerdem bereits gut gelöst.

7. Priorisierte Refactoring-Maßnahmen
Priorität | Datei               | Maßnahme                                                     | Nutzen | Risiko       | Aufwand      | DDD
1         | Abstimmungszeitraum | Wiederholte Null-Prüfungen zusammenführen                    | Mittel | Sehr gering  | Sehr gering  | Ja
2         | Abstimmung          | Factory-Methode auf Lesbarkeit prüfen und ggf. modularisieren| Mittel | Gering       | Gering       | Ja
3         | Abstimmung          | Sechs Factory-Parameter ggf. in Record bündeln               | Mittel | Gering–mittel| Mittel       | Ja
4         | Abstimmung          | hatOption() vereinfachen                                     | Gering | Gering       | Sehr gering  | Ja
5         | Titel               | stripSpaces() nur bei fachlich gewünschter Änderung ersetzen | Gering | Mittel       | Sehr gering  | Bedingt
6         | EingabeValidator    | Verantwortlichkeiten und mögliche Doppelvalidierung prüfen   | Mittel | Mittel       | Gering–mittel| Ja
Wichtig: Diese Priorisierung betrifft vor allem die Codepflege. Die fachliche Klärung der Fristgrenze in Abstimmungszeitraum sollte unabhängig davon früh erfolgen.

8. Welche Metriken sind unproblematisch?
Nicht jede auffällige Kennzahl bedeutet automatisch schlechte Codequalität.

8.1 Cyclomatic Complexity: 83
Der Gesamtwert ist für 43 Methoden zunächst unproblematisch.
Entscheidend ist die Komplexität einzelner Methoden.
Ein Gesamtwert von 83 bedeutet nicht, dass eine einzelne Methode 83 Entscheidungswege besitzt.
Ebenso bedeutet die Datei-Komplexität von 35 nicht automatisch, dass Abstimmung.java eine übermäßig komplexe Methode enthält.
Keine pauschale Änderung erforderlich.

8.2 Cognitive Complexity: 43
Der Gesamtwert deutet nicht unmittelbar auf schwer verständlichen Code hin.
Die Cognitive Complexity bewertet unter anderem verschachtelte Kontrollstrukturen und komplexe Bedingungen.
Relevant wäre insbesondere, ob einzelne Methoden hohe Werte erreichen.
Diese Detailwerte liegen nicht vor.
Keine Änderung allein aufgrund des Gesamtwertes erforderlich.

8.3 Kommentaranteil: 16,7 %
Dieser Wert ist unproblematisch.
Die bereitgestellten Dateien enthalten zahlreiche JavaDoc-Kommentare.
Bei einer Domänenschicht sind erklärende Kommentare über Fachregeln durchaus sinnvoll.
Allerdings sollten Kommentare nicht lediglich offensichtliche Programmabläufe wiederholen.
Beispiel:
// Regel 0: Id ist gesetzt
if (id == null) {
    // ...
}
Der Kommentar vermittelt kaum zusätzliche Informationen.
Ein Kommentar über die fachliche Bedeutung einer Regel wäre wertvoller.
Empfehlung: Kommentare bei Gelegenheit qualitativ überprüfen, aber keinen bestimmten Prozentwert anstreben.

8.4 Klassen- und Methodenanzahl
11 Klassen und 43 Methoden sind für die beschriebene Domänenschicht nicht auffällig.
Insbesondere bei DDD ist eine größere Anzahl kleiner Value Objects sinnvoll.
Eine Reduzierung der Klassenanzahl wäre kein Qualitätsziel.

8.5 Sechs Parameter
Sechs Parameter sind etwas unhandlich, aber kein zwingender Code-Smell.
Eine separate Eingabestruktur lohnt sich vor allem, wenn sie fachlich sinnvoll ist oder mehrere Aufrufstellen vereinfacht.
Die bloße Reduktion von sechs Parametern auf einen Record-Parameter ist noch keine wesentliche Architekturverbesserung.

9. Zusätzliche Beobachtungen außerhalb der Metriken
Bei der Durchsicht sind mir drei Punkte aufgefallen, die SonarQube-Metriken allein nicht ausreichend abbilden.

9.1 Unveränderlichkeit der Abstimmungsoptionen
Die Aggregate Root erstellt bereits eine unveränderliche Kopie:
return List.copyOf(optionen);
Das ist eine gute Entscheidung.
Dadurch kann die Liste nach der Erstellung nicht von außen verändert werden.
Vorausgesetzt, AbstimmOption selbst ist unveränderlich, unterstützt dies die Integrität des Aggregats.
Beibehalten.

9.2 Identitätsbasierter Vergleich
Die Methode equals() vergleicht Abstimmungen ausschließlich über ihre ID.
@Override
public boolean equals(Object o) {
    return o instanceof Abstimmung andere && id.equals(andere.id);
}
Das ist für eine DDD-Entität angemessen.
Im Gegensatz zu einem Value Object wird die Identität nicht über sämtliche Attribute bestimmt.
Beibehalten.

9.3 Zeitliche Fachregeln sind an einer Stelle konzentriert
Die Aggregate Root delegiert ihre zeitlichen Prüfungen an Abstimmungszeitraum.
Beispiel:
public boolean istBeendet(LocalDateTime zeitpunkt) {
    return zeitraum.istVorbei(zeitpunkt);
}
Das ist eine sinnvolle Trennung.
Der Zeitraum kennt seine zeitlichen Grenzen, während die Aggregate Root daraus fachliche Zustände und Berechtigungen ableitet.
Diese Struktur sollte ausdrücklich erhalten bleiben.

10. Abschließende Empfehlung
Für das Projekt „votee“ würde ich kein umfangreiches Refactoring empfehlen.
Die bereitgestellten Dateien zeigen bereits mehrere positive Eigenschaften:
- Fachregeln werden in der Aggregate Root beziehungsweise in den Value Objects geprüft.
- Value Objects verwenden Java Records.
- Die Domänenschicht benötigt für ihre Validierung kein Framework.
- Die Factory-Methoden vermeiden doppelte Validierungslogik.
- Die Optionsliste wird unveränderlich gespeichert.
- Die Implementierung verwendet fachlich aussagekräftige Methodennamen.
Die größten Verbesserungsmöglichkeiten sehe ich bei der Wartbarkeit der Factory-Methode und bei den wiederholten Prüfungen im Abstimmungszeitraum.
Eine Reduktion der Metriken sollte dabei nicht zum Selbstzweck werden.

Meine konkrete Entscheidung als Reviewer
Maßnahme                                          | Entscheidung
pruefeOptionen() umschreiben                      | Nicht erforderlich
Abstimmung in mehrere Klassen zerlegen            | Nicht erforderlich
Sechs Parameter durch Record ersetzen             | Optional, bei wachsender Schnittstelle sinnvoll
Factory-Methode modularisieren                    | Optional
Null-Prüfungen im Zeitraum zusammenführen         | Empfehlenswert
stripSpaces() durch strip() ersetzen              | Ohne fachliche Klärung nicht empfehlenswert
Fachlich benannte Delegationsmethoden entfernen   | Nicht empfehlenswert
Kommentare pauschal reduzieren                    | Nicht erforderlich
Fachregeln in technische Validatoren verschieben  | Nicht empfehlenswert

Gesamturteil
Die Codequalität ist anhand der vorliegenden Metriken und drei untersuchten Dateien überwiegend positiv zu bewerten.
Die erhöhte Cyclomatic Complexity von Abstimmung.java ist durch ihre zentrale Rolle als Aggregate Root zumindest teilweise erklärbar. Eine abschließende Beurteilung erfordert allerdings die Komplexitätswerte der einzelnen Methoden.
Die sinnvollsten Optimierungen sind klein, gezielt und risikoarm. Sie sollten die fachlichen Regeln nicht verändern.
Für ein studentisches DDD-Projekt mit 464 LOC ist es wichtiger, die Domänenlogik nachvollziehbar und testbar zu halten, als die Cyclomatic Complexity um einige Punkte zu reduzieren.
Eine wesentliche Einschränkung bleibt: Die SonarQube-Werte allein liefern keine Aussage über Testabdeckung, Bugs, Security Hotspots oder tatsächliche Wartungsprobleme. Zudem wurden nur drei der elf Klassen im Quellcode untersucht. Das Gesamturteil ist daher eine begründete Ersteinschätzung und keine vollständige Codequalitätsprüfung.
```
