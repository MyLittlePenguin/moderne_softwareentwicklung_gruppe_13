# 5.2 LLM-Prompt und Antwort

**LLM:** ChatGPT GPT-6 Medium, 18.11.2026, neuer Chat ohne Vorwissen. Eingefügt: Abstimmung.java, Titel.java (Stand main e53be0c).

## Prompt

---

Du hilfst mir, die Testabdeckung meines Java-Projekts gezielt zu verbessern. Schreibe **Testideen und Edge Cases** mit kurzen JUnit-5-Skizzen. Ändere den Produktionscode nicht.

**Projekt:** eVote „votee“, Online-Abstimmungssystem. Java 27, JUnit 5, Domain-Driven Design, Domänenschicht ohne Framework. `Abstimmung` ist Aggregate Root, `Titel` ein Value Object (record). Fachliche Fehler werfen `AbstimmungsregelVerletztException` (Unterklasse von `IllegalArgumentException`).

**Coverage (JaCoCo 0.8.16, 190 Tests):** gesamt 94 % Instructions, 95 % Branches. Im Paket `domain.model` fehlen nur noch:
1. `Abstimmung.erstelle(AbstimmungId, NutzerId, Titel, String, Abstimmungszeitraum, List)`: Zweig `titel == null` nie wahr (alle bisherigen Tests nutzen die `String`-Überladung).
2. `Abstimmung.getTitelObjekt()`: nie aufgerufen.
3. `Abstimmung.equals(Object)`: Zweig „`o` ist keine `Abstimmung`“ nie ausgeführt, obwohl ein Test `assertNotEquals(null, a)` und `assertNotEquals("keine Abstimmung", a)` enthält.
4. `Titel.stripSpaces(String)`: 2 von 8 Zweigen fehlen (`start < str.length()` falsch, `end > start` falsch).

**Fragen:**
1. Welche Tests schließen die Lücken 1–3? Zeige je eine JUnit-5-Skizze.
2. Warum wird bei Lücke 3 der Zweig trotz des vorhandenen Tests nicht ausgeführt?
3. Sind die Zweige bei Lücke 4 über die öffentliche Schnittstelle überhaupt erreichbar? Begründe. Was würdest du stattdessen tun?
4. Welche **besonders komplexen Methoden** (`Titel`-Konstruktor, `pruefeOptionen`) haben Edge Cases, die zwar abgedeckt aussehen, aber fachlich noch fehlen könnten? Nenne max. 5 konkrete Edge Cases.
5. Ist 100 % Coverage hier ein sinnvolles Ziel? Kurze Begründung.

**Code:**

```java
// Abstimmung.java
<HIER EINFÜGEN>
```

```java
// Titel.java
<HIER EINFÜGEN>
```

_(Beim Prompt wurden an den beiden Platzhaltern die Dateien aus `main` @ `e53be0c` eingefügt.)_

## Antwort (ungekürzt)

```text
Testanalyse – eVote „votee“
Ausgangslage: 190 Tests, 94 % Instruction Coverage und 95 % Branch Coverage.

Die beiden Java-Dateien zeigen, dass sich die ersten drei Lücken gezielt schließen lassen. Bei Titel.stripSpaces() sind dagegen zwei Zweige über die öffentliche Schnittstelle nicht erreichbar.

1. Fehlender Zweig: Abstimmung.erstelle() mit Titel == null
Ursache: Die bisherigen Tests verwenden die String-Überladung. Diese erzeugt zuerst ein Titel-Objekt:
return erstelle(id, erstelltVon, new Titel(titel),
                beschreibung, zeitraum, optionen);
Dadurch wird ein ungültiger Titel bereits im Konstruktor von Titel abgefangen. Die Null-Prüfung der anderen Factory-Methode wird nicht erreicht.
Die betreffende Prüfung steht in Abstimmung.java, Zeile 62.
JUnit-5-Test
@Test
void erstelleMitNullTitelObjektWirftException() {
    assertThrows(
        AbstimmungsregelVerletztException.class,
        () -> Abstimmung.erstelle(
            gueltigeId,
            gueltigerNutzer,
            (Titel) null,
            null,
            gueltigerZeitraum,
            gueltigeOptionen
        )
    );
}
Die Variablen gueltigeId, gueltigerNutzer, gueltigerZeitraum und gueltigeOptionen stehen hier für gültige Test-Fixtures.
Wichtig: (Titel) null erzwingt die richtige Überladung. Ohne Cast wäre der Methodenaufruf wegen der beiden Überladungen mehrdeutig.
Ergebnis: Der bisher fehlende true-Zweig von titel == null wird ausgeführt.

2. Fehlender Aufruf: Abstimmung.getTitelObjekt()
Die Methode gibt das gespeicherte Value Object zurück:
public Titel getTitelObjekt() {
    return titel;
}
Sie wurde bisher nicht aufgerufen.
JUnit-5-Test
@Test
void getTitelObjektLiefertGespeichertenTitel() {
    Titel titel = new Titel("Meine Abstimmung");
    Abstimmung abstimmung = Abstimmung.erstelle(
        gueltigeId,
        gueltigerNutzer,
        titel,
        null,
        gueltigerZeitraum,
        gueltigeOptionen
    );
    assertSame(titel, abstimmung.getTitelObjekt());
}
Warum assertSame()?
Es gibt einen Unterschied zwischen:
- assertEquals(): Prüft die Wertgleichheit.
- assertSame(): Prüft, ob beide Referenzen auf dasselbe Objekt zeigen.
Da getTitelObjekt() das gespeicherte Objekt unverändert zurückgibt, ist assertSame() hier passend.
Ergebnis: Die Methode wird vollständig abgedeckt.

3. Fehlender Zweig: Abstimmung.equals(Object)
Deine Implementierung lautet:
@Override
public boolean equals(Object o) {
    return o instanceof Abstimmung andere
            && id.equals(andere.id);
}
Die Gleichheit hängt ausschließlich von der Identität der Abstimmung ab.
Warum reichen die vorhandenen Tests nicht?
Du verwendest bisher:
assertNotEquals(null, a);
assertNotEquals("keine Abstimmung", a);
Das Problem ist die Reihenfolge der Argumente.
JUnit verwendet sinngemäß:
Objects.equals(unexpected, actual)
Dadurch ergeben sich folgende Aufrufe:
Test                                      | Tatsächlich geprüfte Methode
assertNotEquals(null, a)                  | Objects.equals(null, a)
assertNotEquals("keine Abstimmung", a)    | "keine Abstimmung".equals(a)
assertNotEquals(a, "keine Abstimmung")    | a.equals("keine Abstimmung")
Bei null beendet Objects.equals() den Vergleich, ohne a.equals() aufzurufen.
Beim String wird die equals()-Methode von String ausgeführt, nicht die von Abstimmung.
JUnit-5-Test
@Test
void equalsMitFremdemObjektLiefertFalse() {
    Abstimmung abstimmung = erstelleGueltigeAbstimmung();
    assertNotEquals(abstimmung, "keine Abstimmung");
}
Dieser Test führt tatsächlich Abstimmung.equals() aus.
Noch eindeutiger wäre:
@Test
void equalsMitFremdemObjektLiefertFalse() {
    Abstimmung abstimmung = erstelleGueltigeAbstimmung();
    assertFalse(abstimmung.equals("keine Abstimmung"));
}
Hier ist unmittelbar erkennbar, welche Methode getestet wird.
Für null wäre entsprechend assertFalse(abstimmung.equals(null)) möglich.
Ergebnis: Der fehlende instanceof == false-Zweig wird ausgeführt.

4. Fehlende Zweige in Titel.stripSpaces()
Die Methode lautet:
private static String stripSpaces(String str) {
    int start = 0;
    while (start < str.length()
            && str.charAt(start) == ' ') {
        start++;
    }
    int end = str.length();
    while (end > start
            && str.charAt(end - 1) == ' ') {
        end--;
    }
    return str.substring(start, end);
}
Sind die fehlenden Zweige erreichbar?
Nein, nicht über den öffentlichen Konstruktor von Titel.
Der Konstruktor prüft zunächst:
if (wert == null || wert.isBlank()) {
    throw new AbstimmungsregelVerletztException(...);
}
Erst danach wird stripSpaces() aufgerufen.
Zweig A: start < str.length() ist false
Dieser Fall tritt ein, wenn start das Ende des Strings erreicht.
Beispiel:
stripSpaces("   ");
Alle Zeichen sind Leerzeichen. Die Schleife würde bis zum Ende laufen.
Allerdings wird "   " bereits durch isBlank() zurückgewiesen.
Ein String mit gültigen Zeichen kann diesen Zustand nicht erreichen.
Zweig B: end > start ist false
Dieser Zweig tritt ein, wenn end und start identisch sind.
Beispiel:
stripSpaces("   ");
Nach dem Entfernen aller führenden Leerzeichen gilt:
start = 3;
end = 3;
Die zweite Schleife wird nicht betreten.
Auch dieser Zustand ist über den Konstruktor nicht erreichbar.
Was würde ich stattdessen tun?
Ich würde keine Reflection-Tests für die private Methode schreiben, nur um JaCoCo zufriedenzustellen.
Stattdessen:
1. Die fachlich erreichbaren Fälle über new Titel(...) testen.
2. Die beiden nicht erreichbaren Zweige dokumentieren.
3. Bei der Coverage-Bewertung zwischen fehlenden Tests und nicht erreichbaren Zweigen unterscheiden.
4. Den Produktionscode unverändert lassen.
Sinnvolle Tests wären:
@Test
void titelEntferntFuehrendeUndFolgendeLeerzeichen() {
    Titel titel = new Titel("  Abstimmung  ");
    assertEquals("Abstimmung", titel.wert());
}
Und:
@Test
void titelMitNurLeerzeichenWirdAbgelehnt() {
    assertThrows(
        AbstimmungsregelVerletztException.class,
        () -> new Titel("   ")
    );
}
Der zweite Test deckt die Validierung durch isBlank() ab, nicht die unerreichbaren Zweige in stripSpaces().
Fazit: Die fehlenden Zweige sind technisch nachvollziehbar, aber über die öffentliche API nicht erreichbar. Eine Coverage von 100 % wäre hier ohne Eingriff in die Implementierung oder Tests privater Methoden nicht sinnvoll erzwingbar.

5. Fünf fachliche Edge Cases
Hohe Branch Coverage bedeutet nicht automatisch, dass alle fachlich relevanten Eingaben getestet wurden.
Besonders interessant sind der Titel-Konstruktor und Abstimmung.pruefeOptionen().

Edge Case 1: Unicode-Normalisierung verändert die Länge
Der Titel wird vor der Längenprüfung nach NFC normalisiert.
Beispiel:
@Test
void titelNormalisiertZerlegtenUmlaut() {
    Titel titel = new Titel("Müller");
    assertEquals("Müller", titel.wert());
}
Warum relevant?
Der zerlegte Umlaut besteht zunächst aus zwei Unicode-Codepoints. Nach NFC wird daraus ein zusammengesetztes Zeichen.
Damit wird geprüft, ob die Normalisierung tatsächlich vor der Validierung greift.

Edge Case 2: Unicode-Zeichen und Längengrenze
Der Konstruktor verwendet:
bereinigt.length()
In Java zählt String.length() UTF-16-Codeeinheiten und nicht Unicode-Codepoints.
Ein ergänzendes Zeichen wie 𐐀 benötigt zwei UTF-16-Codeeinheiten.
@Test
void titelMitUnicodeZeichenAnLaengengrenze() {
    String text = "𐐀".repeat(51);
    assertThrows(
        AbstimmungsregelVerletztException.class,
        () -> new Titel(text)
    );
}
Warum relevant?
Der Titel enthält 51 Unicode-Codepoints, aber 102 UTF-16-Codeeinheiten.
Der Test dokumentiert das aktuelle Verhalten. Fachlich sollte zusätzlich geklärt werden, was „maximal 100 Zeichen“ bedeutet.
Hinweis: Java zählt mit length() ebenfalls nicht automatisch für Nutzer wahrnehmbare Zeichen, sogenannte Graphemcluster.

Edge Case 3: Unsichtbare Unicode-Zeichen
Der Titel erlaubt bestimmte Unicode-Buchstaben und Sonderzeichen, aber keine beliebigen Steuer- oder Formatierungszeichen.
@Test
void titelMitZeroWidthSpaceWirdAbgelehnt() {
    assertThrows(
        AbstimmungsregelVerletztException.class,
        () -> new Titel("Meine​Abstimmung")
    );
}
Warum relevant?
Das Zero Width Space (U+200B) ist unsichtbar.
Solche Zeichen können durch Kopieren aus Webseiten oder Dokumenten entstehen.
Der Test prüft, ob das definierte Zeichenmuster sie zuverlässig zurückweist.

Edge Case 4: Duplikate nach Normalisierung
pruefeOptionen() vergleicht nicht die ursprünglichen Texte, sondern deren normalisierte Varianten.
@Test
void optionenMitNormalisiertGleichemTextWerdenAbgelehnt() {
    List<AbstimmOption> optionen = List.of(
        new AbstimmOption("Ja"),
        new AbstimmOption(" ja ")
    );
    assertThrows(
        AbstimmungsregelVerletztException.class,
        () -> erstelleMitOptionen(optionen)
    );
}
Warum relevant?
Der Test prüft, ob unterschiedliche Schreibweisen einer Antwortoption als Duplikate erkannt werden.
Voraussetzung: AbstimmOption.normalisierterText() entfernt Rand-Leerzeichen und berücksichtigt Groß-/Kleinschreibung entsprechend. Diese Implementierung liegt mir nicht vor.
Der Test ist deshalb ein gezielter Testkandidat, dessen erwartetes Ergebnis mit der tatsächlichen Normalisierungsregel abgeglichen werden muss.

Edge Case 5: Die Optionsliste wird nach Erstellung verändert
Die Methode gibt eine unveränderliche Kopie zurück:
return List.copyOf(optionen);
Daraus ergeben sich zwei fachlich wichtige Eigenschaften:
- Nachträgliche Änderungen der ursprünglichen Liste dürfen die Abstimmung nicht beeinflussen.
- Die über getOptionen() zurückgegebene Liste darf nicht veränderbar sein.
@Test
void abstimmungSchuetztOptionenVorAenderungen() {
    List<AbstimmOption> optionen = new ArrayList<>();
    optionen.add(new AbstimmOption("Ja"));
    optionen.add(new AbstimmOption("Nein"));
    Abstimmung abstimmung = erstelleMitOptionen(optionen);
    optionen.add(new AbstimmOption("Vielleicht"));
    assertEquals(2, abstimmung.getOptionen().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> abstimmung.getOptionen().clear()
    );
}
Warum relevant?
Das Aggregate Root muss seine Invarianten nach der Erstellung bewahren.
Ein Test auf eine unveränderliche Liste ist deshalb fachlich wertvoller als das bloße Ausführen einer weiteren Verzweigung.

6. Ist 100 % Coverage ein sinnvolles Ziel?
Nein, nicht als Selbstzweck.
Mit 190 Tests sowie 94 % Instruction Coverage und 95 % Branch Coverage besitzt das Projekt bereits eine hohe strukturelle Testabdeckung.
Dabei gilt:
Metrik                  | Aussage
Instruction Coverage    | Wie viel ausführbarer Code wurde ausgeführt?
Branch Coverage         | Welche Verzweigungsalternativen wurden ausgeführt?
Fachliche Testqualität  | Werden die Geschäftsregeln einschließlich ihrer Grenzfälle zuverlässig geprüft?
Die dritte Frage beantworten die ersten beiden Metriken nicht.
Gerade bei deinem DDD-Projekt sind die fachlichen Invarianten wichtiger als die letzten Prozentpunkte.

Meine Priorisierung
Priorität  | Maßnahme                             | Begründung
1 – Hoch   | Null-Test für Titel-Überladung       | Fehlenden Validierungspfad prüfen
2 – Hoch   | equals() mit Fremdobjekt             | Identitätsvergleich vollständig testen
3 – Mittel | getTitelObjekt()                     | Öffentliche Methode absichern
4 – Hoch   | Unicode- und Normalisierungsfälle    | Fachliche Grenzfälle prüfen
5 – Hoch   | Unveränderlichkeit der Optionsliste  | Aggregate-Invarianten schützen
6 – Niedrig| Private stripSpaces()-Zweige         | Über öffentliche API nicht erreichbar

Abschließende Empfehlung
Ich würde zunächst die drei gezielten Tests ergänzen und anschließend die fünf Edge Cases mit dem bestehenden Testbestand abgleichen, damit keine unnötigen Doppelungen entstehen.
Mein Qualitätsziel für „votee“ wäre nicht 100 % Branch Coverage, sondern eine möglichst vollständige Abdeckung der fachlichen Regeln, ihrer Grenzwerte und der Aggregate-Invarianten.
Die verbleibenden, nachweislich unerreichbaren Zweige in stripSpaces() würde ich bewusst akzeptieren und dokumentieren.
```

_Hinweis: Tabellen aus der Chat-Ansicht wurden beim Kopieren als Tab-getrennter Text übernommen und hier als `|`-Spalten dargestellt; der Inhalt ist unverändert._
