package de.mse.votee_backend.kontoverwaltung.domain.model;

/**
 * Benutzerkonto im Bounded Context {@code kontoverwaltung}.
 *
 * @param id           eindeutige Konto-ID
 * @param benutzername eindeutiger Anmeldename
 * @param passwortHash gesalzener Passwort-Hash (niemals Klartext)
 * @param rolle        die Rolle des Kontos
 */
public record NutzerKonto(String id, String benutzername, String passwortHash, Rolle rolle) {

    public NutzerKonto {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Konto-ID darf nicht leer sein.");
        }
        if (benutzername == null || benutzername.isBlank()) {
            throw new IllegalArgumentException("Benutzername darf nicht leer sein.");
        }
        if (passwortHash == null || passwortHash.isBlank()) {
            throw new IllegalArgumentException("Passwort-Hash darf nicht leer sein.");
        }
        if (rolle == null) {
            throw new IllegalArgumentException("Rolle muss angegeben werden.");
        }
        id = id.strip();
        benutzername = benutzername.strip();
    }

    /**
     * Prüft, ob dieses Konto Administrator-Rechte (Verwaltungsangestellte) besitzt.
     *
     * @return {@code true}, falls die Rolle {@link Rolle#ADMIN} ist
     */
    public boolean istAdministrator() {
        return rolle == Rolle.ADMIN;
    }
}
