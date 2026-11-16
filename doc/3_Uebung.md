## Domainmodel

```mermaid
classDiagram
    class LoginData {
        <<Value Object>>
        UserName
        Passwort
    }

    class Adresse {
        <<Value Object>>
        strasse
        hausnummer
        plz
        ort
    }

    class User {
        <<Entity>>
        login()
    }

    class VerwaltungsAngestellte {
        <<Entity>>
    }

    class Bürgerin {
        <<Entity>>
        name
        vorname
    }

    class Abstimmung {
        <<Entity>>
        titel
        beschreibung
        frist
    }

    class AbstimmOption {
        <<Value Object>>
        text
    }

    class Stimmabgabe {
        <<Entity>>
    }
    
    class Abstimmungszeitraum {
        <<Value Object>>
        begin
        ende
    }

    User *-- LoginData
    Bürgerin --|> User
    Bürgerin *-- Adresse
    VerwaltungsAngestellte --|> User

    Abstimmung "1" *-- "1..n" AbstimmOption
    Abstimmung *-- Abstimmungszeitraum
    VerwaltungsAngestellte --> Abstimmung: erstellt

    Bürgerin "0..n" -- "0..n" Abstimmung: öffnet
    Bürgerin "0..n" -- "0..n" Stimmabgabe: gibt ab
    Stimmabgabe "0..n" -- "1" Abstimmung
    Stimmabgabe "1" -- "1" AbstimmOption
```

## Domain Services und Repositories

### Services

- NutzerService
    - Art: Anwendungsservice
    - Aufgabe:
        - Steuert anmeldung von Verwaltungsangestelltinnen und Bürgerinnen
    - Methoden
        - registriereBürgerin(name, vorname, adresse)
        - generiere2FACodes()
        - login(nutzername, passwort)
        - isVA(): bool
- AbstimmungsService
    - Art: Domain Service
    - Aufgabe:
        - Erstellt Abstimmungen
        - lädt Informationen zu Abstimmungen
        - lädt Ergebnisse zu Abstimmungen
    - Methoden
        - erstelleAbstimmung(titel, beschreibung, frist, abstimmOptionen): Optional\<int\>
        - getAbstimmung(abstimmId): Optional\<Abstimmung\>
        - getAbstimmungsErgebnis(abstimmId): Optional\<AbstimmErgebnis\>
- StimmabgabeService
    - Art: Anwendungsservice Service
    - Aufgabe: Steuert die Stimmabgabe durch Bürgerinnen
    - Methoden
        - stimmeAb(abstimmungsId, abstimmOption, buergerinnenId)
- ErgebnisService
    - Art: Anwendungsservice Service
    - Aufgabe: Steuert die Anzeige von Abstimmergebnissen
    - Methoden
        - getAbstimmErgebnis(abstimmungsId): 

### Repositories

- NutzerRepo
    - Aggregate: Nutzer (Verwaltungsangestellte, Bürgerin)
    - Methoden:
        - save(Nutzer): Optional\<NutzerId\>
        - findById(userId): Optional\<Nutzer\>
- AbstimmungsRepo
    - Aggregate: Abstimmung
    - Methoden:
        - save(Abstimmung): Optional\<AbstimmungId\>
        - findById(abstimmungId): Optional\<Abstimmung\>
        - findOpen(): List\<Abstimmung\>
        - findClosed(): List\<Abstimmung\>
        - getAll(): List\<Abstimmung\>
- StimmabgabeRepo
    - Aggregate: Stimmabgabe
    - Methoden:
        - save(Stimmabgabe)

# Implementationsstrategie

## Vorgehen

1. Reihenfolge der Events: 
   Wir wählen die Ereignisse
   "Bürgerin erstellt" 
   "Abstimmung erstellt" 
   "Stimme abgegeben" 
   "Abstimmung abgeschlossen" 
   aus. Die Umsetzung beginnen wir mit "Abstimmung erstellt" im Context der
   Abstimmungsverwaltung.
2. Von innen nach außen: Pro Context zuerst Value Objects und Entities mit
   ihren, dann das Repository-Interface, die H2 Datenbank, das zugehörige
   Repository für dem umgang mit der DB und den Anwendungsservice. Die
   Oberfläche folgt in einer späteren Übung.
3. Test zuerst (TDD): Vor jeder Implementierung entsteht der Test (Übung 4).
4. Modularität: Jeder Bounded Context ist ein eigener Paketbaum. Contexts kennen
   einander nur über IDs und definierte Schnittstellen.
5. Testbarkeit: Die Domänenschicht kommt ohne Framework aus.
   Helidon als Webserver und für die Infrastruktur um JDBC zu verwenden.
   Wir nutzen Dependency Injection. Organisieren unser Projekt in der
   Hexagonalen Architektur und definieren für die Repositories Interfaces.
   Die Domänenlogik testen wir mit reinen Unit-Tests, die Anwendungsservices
   mit einem In-Memory-Repository und die Adapter mit wenigen Integrationstests.


## Paketstruktur

```
de.mse.votee_backend
+-- abstimmungsverwaltung
+-- domain
|   +-- model abstimmungId, titel, beschreibung, abstimmungsOptionen, abstimmungszeitraum
|   +-- repository AbstimmungRepository (Interface, Port)
+-- application AbstimmungService
+-- infrastructure H2AbstimmungRepository (Adapter)
```
