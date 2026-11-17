# Review der DDD-Prinzipien (Domain-Driven Design)

**Projekt:** eVote („votee“)  
**Untersuchter Bounded Context:** `abstimmungsverwaltung` (Fokus: Event *„Abstimmung erstellt“*, Stand Übung 4)  
**Datum:** 17.11.2026  

---

## 1. Executive Summary

Im Rahmen des vierten Übungsblatts wurde die Domänenlogik für das Event *„Abstimmung erstellt“* im Kontext `abstimmungsverwaltung` testgetrieben (TDD) implementiert. Dieses Review bewertet die Einhaltung der Prinzipien des **Domain-Driven Design (DDD)** im aktuellen Codebestand.

### Gesamtbewertung

| DDD-Kriterium | Status | Zusammenfassung |
| :--- | :---: | :--- |
| **Framework-Unabhängigkeit des Domain-Kerns** | 🟢 Vorbildlich | Das Paket `domain.model` ist vollständig frei von Frameworks und externen Abhängigkeiten (reines Standard-Java). |
| **Ubiquitous Language (Allgegenwärtige Sprache)** | 🟢 Eingehalten | Fachbegriffe (`Abstimmung`, `AbstimmOption`, `Abstimmungszeitraum`, `Frist`, `NutzerId`) werden konsistent und fachlich präzise verwendet. |
| **Value Objects (Unveränderlichkeit & Validierung)** | 🟢 Eingehalten | `NutzerId`, `Abstimmungszeitraum` und `AbstimmOption` sind sauber als immutabler Java `record` mit Selbstvalidierung umgesetzt. |
| **Aggregate Root & Invariantenschutz** | 🟢 Eingehalten | Always-Valid-Zustand wird über statische Factory garantiert; Kapselung über `List.copyOf` ist geschützt. |
| **Entitäts-Identität (Entity / Aggregate Identity)** | 🟢 Erfüllt (Refaktoriert) | `AbstimmungId` als Value Object eingeführt; `Abstimmung` besitzt ID als Pflichtfeld und identitätsbasierte `equals`/`hashCode`. |
| **Rich Domain Model vs. Anämisches Modell** | 🟢 Erfüllt (Refaktoriert) | Domänenverhalten direkt im Aggregat (`istLaufend`, `istBeendet`, `kannAbgestimmtWerden`, `hatOption`) und im Value Object `Abstimmungszeitraum` (`enthaelt`, `istVorbei`, `istInZukunft`) implementiert. |
| **Domain Events** | 🔴 Offen | Das Event `AbstimmungErstellt` wird vom Aggregat noch nicht erzeugt oder publiziert. |
| **Schichtenarchitektur & Validierungstrennung** | 🟡 Gut mit Lücken | `AbstimmungEingabeValidator` liegt korrekt in `application`. Allerdings sind fachliche Kernregeln (z. B. Titellänge) nur im Validator und nicht im Domain Model geschützt. |

---

## 2. Strategisches Design (Strategic Design)

### 2.1 Ubiquitous Language
- **Konsistente Fachterminologie:** Die Begriffe aus dem Domain Model und Event Storming ([doc/3_Uebung.md](3_Uebung.md)) wurden im Code konsistent übernommen (`Abstimmung`, `AbstimmOption`, `Abstimmungszeitraum`, `NutzerId`).
- **Fachliche Aliase:** Die Methode `getFrist()` in `Abstimmung.java`, welche an `zeitraum.ende()` delegiert, stärkt die Ubiquitous Language, da der Begriff „Frist“ ein zentrales Fachkonzept darstellt.

### 2.2 Bounded Contexts & Kontextabgrenzung
- **Strukturierung:** Der Kontext `abstimmungsverwaltung` bildet die Wurzel der fachlichen Paketstruktur (`de.mse.votee_backend.abstimmungsverwaltung`).
- **Lose Kopplung über Aggregat-IDs:** Anstatt Entitäten aus dem Kontext `kontoverwaltung` (wie `Verwaltungsangestellte` oder `User`) direkt zu referenzieren, referenziert `Abstimmung` den Ersteller lose über das Value Object `NutzerId erstelltVon`. Dies entspricht exakt der DDD-Best-Practice *„Reference other Aggregates by Identity“*.

---

## 3. Taktisches Design (Tactical Design)

### 3.1 Entities & Aggregate Roots (`Abstimmung`)

#### 🟢 Entitäts-Identität (`AbstimmungId`) & Gleichheit (Refaktoriert)
Eine Entität ist im DDD primär über eine unveränderliche Identität definiert, die über die gesamte Lebensdauer fortbesteht:
- Das Value Object `AbstimmungId` (`record(String wert)`) wurde mit Selbstvalidierung und der Factory-Methode `AbstimmungId.neu()` implementiert.
- `Abstimmung.erstelle(AbstimmungId id, ...)` fordert die ID als ersten Pflichtparameter (Regel 0: `id != null`).
- `equals()` und `hashCode()` wurden strikt auf Basis von `id` implementiert. Zwei Instanzen mit identischer ID gelten als identisch (unabhängig von weiteren Attributen); Instanzen mit unterschiedlicher ID gelten als verschieden.

#### 🟢 Fachliches Verhalten & Rich Domain Model (Refaktoriert)
Das Aggregat `Abstimmung` und das Value Object `Abstimmungszeitraum` verfügen über aussagekräftiges fachliches Verhalten:
- **Zeitraumbezogene Zustandsabfragen:** `istLaufend(zeitpunkt)`, `istBeendet(zeitpunkt)`, `istNochNichtGestartet(zeitpunkt)`.
- **Geschäftsregeln der Ubiquitous Language:** `kannAbgestimmtWerden(zeitpunkt)` (aktiv während des Zeitraums) und `kannErgebnisEingesehenWerden(zeitpunkt)` (erst nach Fristablauf gemäß Event-Storming-Policy).
- **Kapselung der Aggregate-Interna:** `hatOption(AbstimmOption)` und `hatOption(String)` erlauben die Prüfung, ob eine Antwortoption existiert, ohne dass Aufrufer von außen in die Optionsliste greifen müssen.
- **Verhalten im Value Object:** `Abstimmungszeitraum` kapselt `enthaelt(zeitpunkt)`, `istVorbei(zeitpunkt)` und `istInZukunft(zeitpunkt)`.

#### 🟢 Invariantenschutz & Kapselung
- **Always-Valid Pattern:** Durch den privaten Konstruktor und die Factory-Methode `Abstimmung.erstelle(...)` kann das Aggregat niemals in einem invaliden Zustand instanziiert werden.
- **Defensive Kopie:** Die Liste der Optionen wird mit `List.copyOf(optionen)` unveränderlich gekapselt.
- **Normalisierung:** Duplikate bei Optionen werden unter Berücksichtigung von Rand-Leerzeichen und Groß-/Kleinschreibung (`strip().toLowerCase(Locale.ROOT)`) zuverlässig abgefangen.

### 3.2 Value Objects

- **`Abstimmungszeitraum`:** Exzellent modelliert als `record(LocalDateTime beginn, LocalDateTime ende)`. Es kapselt Beginn und Ende und stellt sicher, dass `ende.isAfter(beginn)` gilt.
- **`NutzerId`:** Schützt vor typischer *Primitive Obsession* (Verhindert, dass beliebige Strings fälschlich als ID genutzt werden) und validiert auf `null` und Blank.
- **`AbstimmOption`:** Sauber als unveränderlicher Record modelliert.

#### 💡 Verbesserungspotenzial (Primitive Obsession & Titel-Invariante):
- `titel` und `beschreibung` sind noch primitive `java.lang.String`.
- Die Regel zur Mindest- und Maximallänge eines Titels (3 bis 100 Zeichen) sowie erlaubte Zeichensätze befinden sich derzeit ausschließlich im `AbstimmungEingabeValidator` der Application-Schicht.
- *DDD-Grundsatz:* Eine Domäneninvariante gehört in die Domänenschicht (z. B. als Value Object `Titel`), damit sie nicht durch alternative Aufrufer der Factory umgangen werden kann.

### 3.3 Domain Events
- Im Event Storming ist *„Abstimmung erstellt“* (`AbstimmungErstellt`) das zentrale Startereignis.
- Derzeit erzeugt oder publiziert das Aggregat `Abstimmung` noch keine Domain Events. Die vom LLM im Testentwurf vorgeschlagene Methode `domainEvents()` wurde vorerst entfernt.
- Nach DDD sollten wesentliche Zustandsänderungen als Domain Events festgehalten werden, um andere Bounded Contexts (z. B. `stimmabgabe`) lose zu benachrichtigen.

---

## 4. Architektur- und Schichtenprinzipien

### 4.1 Unabhängigkeit der Domänenschicht
- **100 % eingehalten:** Das Paket `domain.model` hat keinerlei Abhängigkeiten zu Frameworks (weder Helidon noch Datenbankbibliotheken wie H2/JDBC). Es werden ausschließlich Standard-Java-Klassen verwendet.

### 4.2 Schichtentrennung (Application vs. Domain)
- `AbstimmungEingabeValidator` liegt im Paket `application`. Er dient der syntaktischen Validierung von Formulareingaben (Parsing von Datumsstrings mit striktem `ResolverStyle`, Regex-Prüfungen vor Konvertierung in Value Objects).
- Repositories und Services: Gemäß der Implementierungsstrategie („von innen nach außen“) existieren `AbstimmungRepository` (Domain Port) und `AbstimmungsService` noch nicht und folgen in den nächsten Schritten.

### 4.3 Domänenspezifische Exceptions
- Aktuell wird bei allen Regelverletzungen eine allgemeine `IllegalArgumentException` geworfen.
- Im DDD ist die Verwendung fachlicher Ausnahmen (z. B. `UngueltigerAbstimmungszeitraumException`, `ZuWenigOptionenException`, `DoppelteOptionException`) vorzuziehen, um fachliche Fehler semantisch klar von Programmierfehlern zu trennen.

---

## 5. Konkrete Handlungsempfehlungen (Refactoring Roadmap)

1. **`AbstimmungId` einführen (✅ Erledigt):**
   - Value Object `record AbstimmungId(String wert)` mit Selbstvalidierung und Factory `AbstimmungId.neu()` umgesetzt.
   - Dem Aggregat `Abstimmung` als Pflichtfeld zugewiesen und `equals()` / `hashCode()` basierend auf dieser ID implementiert.
2. **Fachliches Verhalten im Aggregat implementieren (Rich Domain Model) (✅ Erledigt):**
   - Methoden zur zeitlichen Abfrage (`istLaufend`, `istBeendet`, `istNochNichtGestartet`, `kannAbgestimmtWerden`, `kannErgebnisEingesehenWerden`) und Kapselung der Optionen (`hatOption`) ergänzt.
   - Value Object `Abstimmungszeitraum` um `enthaelt`, `istVorbei`, `istInZukunft` erweitert.
3. **Domänen-Invariante für Titel verankern:**
   - Entweder ein Value Object `record Titel(String wert)` einführen, welches die Längenprüfung (3–100 Zeichen) kapselt, oder die Prüfung direkt in `Abstimmung.erstelle(...)` aufnehmen.
4. **Domain Event `AbstimmungErstellt` reaktivieren:**
   - Event-Record `record AbstimmungErstellt(AbstimmungId id, LocalDateTime zeitstempel)` erstellen und im Aggregat registrieren.
5. **Ports & Repository-Schnittstellen anlegen:**
   - Interface `AbstimmungRepository` im Paket `abstimmungsverwaltung.domain.repository` definieren.
