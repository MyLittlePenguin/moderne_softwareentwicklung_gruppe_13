# Funktionale Anforderungen zu vorgeschlagenen Themen

Mögliche funktionale Anforderungen an die 5 Projektthemen, von denen Sie im Rahmen des Projektes mindestens 2 (gerne auch mehr) umsetzen sollten. (Sinnvolle weitere funktionale Anforderungen wären ebenfalls akzeptabel!)

## 1. MediTrack: Patientendatenverwaltung und -überwachung

Ein System zur Verwaltung und Überwachung von Patientendaten, einschließlich Vitaldaten und Behandlungshistorie, mit automatischen Benachrichtigungen bei kritischen Werten.

**Funktionale Anforderungen:**

- **Patientenregistrierung und Authentifizierung:** Patienten und medizinisches Personal sollten sich registrieren und anmelden können.
- **Erfassung von Patientendaten:** Eingabe und Verwaltung von grundlegenden Gesundheitsinformationen (Name, Geburtsdatum, Krankheitsgeschichte).
- **Eingabe von Vitaldaten:** Patienten oder medizinisches Personal können Vitaldaten (z. B. Blutdruck, Puls, Temperatur) eingeben.
- **Benachrichtigungssystem:** Automatische Benachrichtigungen an Ärzte oder Pflegepersonal, wenn Vitalwerte kritische Schwellen überschreiten.
- **Behandlungsübersicht:** Zugriff auf eine Historie der bisherigen Behandlungen und medizinischen Eingriffe.

## 2. PatientCare: Kommunikation und Gesundheitsdatenverwaltung

Ein System zur Kommunikation zwischen Patienten und Ärzten sowie zur Verwaltung und Einsicht von Gesundheitsdaten. Patienten können ihre Vitaldaten eingeben, und Ärzte können diese Daten einsehen, kommentieren und Empfehlungen geben.

**Funktionale Anforderungen:**

- **Nutzerregistrierung und Authentifizierung:** Registrierung für Patienten und Ärzte mit entsprechenden Rollen.
- **Eingabe von Vitaldaten:** Patienten können regelmäßig ihre Gesundheitsdaten (z. B. Blutdruck, Puls) eingeben.
- **Ärztlicher Zugriff:** Ärzte können auf die Gesundheitsdaten der Patienten zugreifen und sie einsehen.
- **Kommunikationssystem:** Nachrichtenfunktion zwischen Arzt und Patient zur Beratung oder Terminvereinbarung.
- **Ärztliche Empfehlungen:** Ärzte können Kommentare oder Empfehlungen zu den eingereichten Vitaldaten abgeben.

## 3. eConsult: Online-Beratungsplattform für Ärzte und Patienten

Ein System, das eine virtuelle Konsultation ermöglicht, in dem Patienten online mit Ärzten in Kontakt treten und Beratungen anfordern können. Ärzte können auf Basis von geteilten Gesundheitsdaten Diagnosen stellen oder Empfehlungen geben.

**Funktionale Anforderungen:**

- **Nutzerregistrierung und Authentifizierung:** Registrierung für Patienten und Ärzte mit unterschiedlichen Berechtigungen.
- **Beratungsanforderung:** Patienten können über die Plattform eine medizinische Beratung anfordern, die dann einem Arzt zugewiesen wird.
- **Gesundheitsdateneingabe:** Patienten können Gesundheitsdaten und Symptome über ein Formular eingeben, um diese mit dem Arzt zu teilen.
- **Arzt-Beratungsfunktion:** Ärzte können die eingereichten Daten einsehen und dem Patienten eine schriftliche oder mündliche Beratung anbieten.
- **Kommunikationssystem:** Echtzeit-Chat oder Nachrichtenfunktion für den Austausch zwischen Patienten und Ärzten.

## 4. CityFeedback: Bürgerbeteiligungs- und Beschwerdesystem

Eine Plattform, auf der Bürger Anliegen oder Beschwerden an die Stadtverwaltung richten und deren Bearbeitungsfortschritt verfolgen können.

**Funktionale Anforderungen:**

- **Nutzerregistrierung und Authentifizierung:** Registrierung für Bürger und Stadtmitarbeiter.
- **Feedback-Erstellung:** Bürger können Anliegen oder Beschwerden über ein Formular einreichen, optional mit Kategorisierung (z. B. Verkehr, Umwelt).
- **Verwaltung von Feedback:** Stadtverwaltung kann eingereichte Beschwerden einsehen, den Bearbeitungsstatus ändern und Kommentare hinzufügen.
- **Statusanzeige:** Bürger können den Bearbeitungsstatus ihrer Anliegen in einem Dashboard einsehen.
- **Benachrichtigungen:** Bürger werden informiert, wenn sich der Status ihrer Beschwerde ändert.

## 5. eVote: Online-Abstimmungssystem für Bürgerbefragungen

Eine sichere Plattform für Online-Abstimmungen, bei der Bürger über städtische Entscheidungen oder Projekte abstimmen können.

**Funktionale Anforderungen:**

- **Nutzerregistrierung und Authentifizierung:** Registrierung für Bürger mit gesicherter Authentifizierung (z. B. durch E-Mail-Bestätigung oder 2-Faktor-Authentifizierung).
- **Abstimmungsübersicht:** Bürger können sich eine Liste der aktuellen Abstimmungen ansehen, inklusive Beschreibung und Frist.
- **Abstimmungsmechanismus:** Bürger können an den Abstimmungen teilnehmen und ihre Stimme abgeben (einfacher Voting-Mechanismus).
- **Ergebnisübersicht:** Nach Abschluss der Abstimmung können Bürger die Ergebnisse einsehen.
- **Abstimmungssicherheit:** Sicherstellen, dass jeder Bürger nur 1x abstimmt (z. B. durch eindeutige Kennung).

---

## Beispiel: Patientenmanagementsystem (PMS)

Ein weiteres Beispiel (als Muster), zu dem ich bei jedem Übungsblatt eine Musterlösung bereitstellen werde. Ein paar funktionale Anforderungen für ein Patientenmanagementsystem (PMS):

- **Nutzerregistrierung und Authentifizierung:** Patienten, Ärzte und Verwaltungsmitarbeiter sollen sich registrieren und mit entsprechenden Berechtigungen authentifizieren können.
  - **Rollenspezifischer Zugriff:** Ärzte können Patientendaten einsehen und bearbeiten, Patienten haben eingeschränkten Zugriff auf ihre eigenen Daten und Verwaltungsmitarbeiter verwalten administrative Funktionen.
- **Patientenstammdatenverwaltung:**
  - **Erfassung von Patientendaten:** Speicherung von grundlegenden Daten wie Name, Geburtsdatum, Adresse, Kontaktdaten, Versicherungsstatus.
  - **Aktualisierung von Patientendaten:** Benutzer (z. B. Verwaltungsmitarbeiter) sollten Patientendaten aktualisieren können, falls Änderungen notwendig sind.
  - **Datenhistorie:** Änderungen an Patientendaten sollten versioniert und nachvollziehbar gespeichert werden.
- **Terminverwaltung:**
  - **Terminbuchung:** Patienten können über die Plattform Arzttermine anfragen. Verwaltungsmitarbeiter oder Ärzte bestätigen diese.
  - **Terminübersicht:** Eine Übersicht aller anstehenden und vergangenen Arzttermine für Patienten und Ärzte.
  - **Benachrichtigungen:** Automatische Benachrichtigungen (z. B. per E-Mail oder SMS) an Patienten über bevorstehende Termine oder Terminänderungen.
- **Kommunikationssystem:**
  - **Interne Nachrichten:** Ärzte und Patienten können über das System miteinander kommunizieren, z. B. für Nachfragen oder Updates zu Behandlungsplänen.
  - **Dokumenten-Upload:** Patienten können Dokumente (wie Röntgenbilder, Laborbefunde) hochladen, die Ärzte einsehen können.
- **Berichte und Statistiken:**
  - **Erstellung von Berichten:** Ärzte und medizinisches Personal sollten einfache Berichte über Behandlungen oder Medikationshistorien erstellen können.
  - **Übersicht über Gesundheitsdaten:** Patienten können ihre Vitaldaten in einem grafischen Dashboard einsehen (z. B. Blutdruckkurve über Zeit).
- **Benachrichtigungssystem:**
  - **Terminerinnerungen:** Automatische Erinnerungen für anstehende Arzttermine oder notwendige Nachuntersuchungen.
  - **Updates zu Behandlungsplänen:** Patienten werden benachrichtigt, wenn sich ihre Behandlungspläne oder Medikamente ändern.
