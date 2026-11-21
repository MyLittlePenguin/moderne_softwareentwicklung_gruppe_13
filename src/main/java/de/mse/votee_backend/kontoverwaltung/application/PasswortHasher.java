package de.mse.votee_backend.kontoverwaltung.application;

/**
 * Port zum sicheren Hashen und Verifizieren von Passwörtern.
 */
public interface PasswortHasher {

    /**
     * Erzeugt einen gesalzenen kryptographischen Hash für das gegebene Passwort.
     *
     * @param klartextPasswort das zu hashende Passwort
     * @return der kodierte Hash inkl. Salt und Parametern
     */
    String hashe(String klartextPasswort);

    /**
     * Prüft ein Klartext-Passwort in konstanter Zeit gegen einen gespeicherten Hash.
     *
     * @param klartextPasswort das eingegebene Passwort
     * @param gespeicherterHash der gespeicherte Hash
     * @return {@code true}, falls das Passwort übereinstimmt, sonst {@code false}
     */
    boolean pruefe(String klartextPasswort, String gespeicherterHash);
}
