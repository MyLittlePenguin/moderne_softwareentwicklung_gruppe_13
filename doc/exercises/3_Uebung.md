*Master Medieninformatik (online) // Moderne Softwareentwicklung // Übungen*

# Übung 3: Systemarchitektur Ihres Projektes modellieren

**Ziel:** Sie sollen die Konzepte von Domain-Driven Design (DDD) und Event Storming auf Ihr eigenes Projekt anwenden, um eine klare und gut strukturierte Softwarearchitektur zu entwickeln. Dabei sollen Sie lernen, das Domänenmodell zu definieren, Bounded Contexts zu identifizieren und Strategien für die Implementierung anzuwenden.

**Relevante Lerneinheiten:** UML + OOD (aus den Bachelorunterlagen)

**Bearbeitungszeit:** Zwei Wochen!

## Aufgabenstellung

1. **Event Storming durchführen**
   - Führen Sie als Team eine Event-Storming-Session durch, um die wichtigsten Ereignisse (Events) in Ihrem System zu identifizieren. Sammeln Sie die relevantesten Ereignisse in Bezug auf Ihre Projektidee (mind. zwei) und visualisieren Sie diese auf einem (digitalen) Whiteboard (z. B. Miro, Zoom-Whiteboard).
   - **Ziel:** Ein gemeinsames Verständnis der Domäne entwickeln und die Geschäftsprozesse klar darstellen.
   - **Output:** Eine Sammlung von identifizierten Events und eine visuelle Darstellung der Ereignisse, inklusive ihrer Reihenfolge und Zusammenhänge.

2. **Domänenmodell erstellen**
   - Definieren Sie auf Basis der Event-Storming-Session das Domänenmodell für Ihr Projekt. Identifizieren Sie die wichtigsten Entitäten, Werte und Aggregate in Ihrer Domäne, die aus den identifizierten Events abgeleitet werden.
   - **Ziel:** Eine klare Darstellung der Kernkonzepte und ihrer Beziehungen im System.
   - **Output:** Erstellen Sie ein einfaches Klassendiagramm, das das Domänenmodell darstellt.

3. **Bounded Contexts identifizieren**
   - Analysieren Sie Ihr Domänenmodell und identifizieren Sie die verschiedenen Bounded Contexts. Bestimmen Sie, welche Teile der Anwendung in separaten Kontexten entwickelt werden sollten, basierend auf den identifizierten Events.
   - **Ziel:** Klare Trennung der Verantwortlichkeiten und des Modells, um Komplexität zu reduzieren.
   - **Output:** Erstellen Sie ein Diagramm oder eine Tabelle, die die identifizierten Bounded Contexts und deren Beziehungen zeigt.

4. **Entitäten und Aggregates definieren**
   - Definieren Sie die Entitäten und Aggregates innerhalb Ihrer Bounded Contexts, die aus den identifizierten Events und dem Domänenmodell abgeleitet werden. Bestimmen Sie, welche Attribute und Methoden sie haben sollten.
   - **Ziel:** Ein detailliertes Verständnis der Datenstruktur und der Logik in Ihrer Anwendung.
   - **Output:** Erstellen Sie ein Dokument, das jede Entität und ihr zugehöriges Aggregate beschreibt, einschließlich ihrer Beziehungen.

5. **Domain Services und Repositories**
   - Identifizieren Sie notwendige Domain Services und Repositories, die zur Verwaltung Ihrer Entitäten benötigt werden. Berücksichtigen Sie dabei die aus dem Event Storming gewonnenen Erkenntnisse.
   - **Ziel:** Definition der Logik, die nicht natürlich in eine Entität passt, und der Repositories, die die Persistenz Ihrer Entitäten verwalten.
   - **Output:** Erstellen Sie eine Übersicht der Domain Services und Repositories, die in Ihrem Projekt verwendet werden, einschließlich ihrer Methoden.

6. **Implementierungsstrategie**
   - Entwickeln Sie eine Strategie zur Implementierung Ihrer DDD-Modelle in Code. Berücksichtigen Sie dabei Aspekte wie Testbarkeit und Modularität.
   - **Ziel:** Vorbereitung einer strukturierten Herangehensweise an die Codierung, die die Prinzipien von DDD berücksichtigt.
   - **Output:** Erstellen Sie ein kurzes Dokument, das die Implementierungsstrategie beschreibt. Zeigen Sie auf, wie ein Domain Model in Ihrer Programmiersprache aussieht, sodass Sie darauf aufbauend nächste Woche mit der Implementierung anfangen können.

**Abgabe (optional):** Via Projekt-Repository bis zum 03.11.26 zur Verfügung stellen.

---

*Prof. Dr. S. Ipek-Ugay*
