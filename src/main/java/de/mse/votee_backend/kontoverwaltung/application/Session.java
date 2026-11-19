package de.mse.votee_backend.kontoverwaltung.application;

import java.time.Instant;
import java.util.Objects;

import de.mse.votee_backend.kontoverwaltung.domain.model.Rolle;

/**
 * Serverseitige Benutzersitzung inklusive CSRF-Token.
 *
 * @param sessionId    kryptographisch zufällige Sitzungs-ID (für HttpOnly-Cookie)
 * @param nutzerId     ID des angemeldeten Kontos
 * @param benutzername Anmeldename des Kontos
 * @param rolle        Rolle des Kontos
 * @param csrfToken    kryptographisch zufälliges CSRF-Token für Formular-/HTMX-Anfragen
 * @param gueltigBis   Ablaufzeitpunkt der Sitzung
 */
public record Session(
        String sessionId,
        String nutzerId,
        String benutzername,
        Rolle rolle,
        String csrfToken,
        Instant gueltigBis) {

    public Session {
        Objects.requireNonNull(sessionId, "Session-ID darf nicht null sein.");
        Objects.requireNonNull(nutzerId, "Nutzer-ID darf nicht null sein.");
        Objects.requireNonNull(benutzername, "Benutzername darf nicht null sein.");
        Objects.requireNonNull(rolle, "Rolle darf nicht null sein.");
        Objects.requireNonNull(csrfToken, "CSRF-Token darf nicht null sein.");
        Objects.requireNonNull(gueltigBis, "Ablaufzeitpunkt darf nicht null sein.");
    }

    /**
     * Prüft, ob die angemeldete Person Administrator-Rechte besitzt.
     *
     * @return {@code true}, falls Rolle {@link Rolle#ADMIN} ist
     */
    public boolean istAdministrator() {
        return rolle == Rolle.ADMIN;
    }

    /**
     * Prüft, ob die Sitzung zum gegebenen Zeitpunkt abgelaufen ist.
     *
     * @param jetzt der aktuelle Zeitpunkt
     * @return {@code true}, falls die Sitzung abgelaufen ist
     */
    public boolean istAbgelaufen(Instant jetzt) {
        Objects.requireNonNull(jetzt, "Zeitpunkt darf nicht null sein.");
        return jetzt.isAfter(gueltigBis);
    }
}
