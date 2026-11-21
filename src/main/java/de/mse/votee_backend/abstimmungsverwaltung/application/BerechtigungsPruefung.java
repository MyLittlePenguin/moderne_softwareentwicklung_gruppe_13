package de.mse.votee_backend.abstimmungsverwaltung.application;

import de.mse.votee_backend.abstimmungsverwaltung.domain.model.NutzerId;

/**
 * Port zur Rollenprüfung gegenüber dem Bounded Context Kontoverwaltung.
 * Entkoppelt die Abstimmungsverwaltung von der konkreten Benutzer- und Rollenverwaltung.
 */
public interface BerechtigungsPruefung {

    /**
     * Prüft, ob die gegebene Nutzer-ID über Administrator-Rechte (Verwaltungsangestellte) verfügt.
     *
     * @param nutzerId die ID des zu prüfenden Kontos
     * @return {@code true}, falls das Konto Administrator-Rechte hat, sonst {@code false}
     */
    boolean istAdministrator(NutzerId nutzerId);
}
