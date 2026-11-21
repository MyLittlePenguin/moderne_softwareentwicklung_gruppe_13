package de.mse.votee_backend.abstimmungsverwaltung.application;

/**
 * Fachliche Exception, wenn ein Konto eine Aktion ohne erforderliche Rolle
 * (z. B. fehlende Administrator-Berechtigung beim Erstellen einer Abstimmung) ausführen möchte.
 */
public class KeineBerechtigungException extends RuntimeException {

    public KeineBerechtigungException(String meldung) {
        super(meldung);
    }
}
