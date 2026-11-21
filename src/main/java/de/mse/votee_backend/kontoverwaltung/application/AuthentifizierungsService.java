package de.mse.votee_backend.kontoverwaltung.application;

import java.util.Objects;
import java.util.Optional;

import de.mse.votee_backend.kontoverwaltung.domain.model.EinmalCode;
import de.mse.votee_backend.kontoverwaltung.domain.model.NutzerKonto;

/**
 * Anwendungsservice für die Anmeldung mit Benutzername, Passwort und 5-stelligem 2FA-Einmalcode.
 */
public class AuthentifizierungsService {

    private final NutzerKontoRepository kontoRepository;
    private final PasswortHasher passwortHasher;
    private final ZweiFaktorPruefung zweiFaktorPruefung;

    public AuthentifizierungsService(NutzerKontoRepository kontoRepository,
                                     PasswortHasher passwortHasher,
                                     ZweiFaktorPruefung zweiFaktorPruefung) {
        this.kontoRepository = Objects.requireNonNull(
                kontoRepository, "NutzerKontoRepository darf nicht null sein.");
        this.passwortHasher = Objects.requireNonNull(
                passwortHasher, "PasswortHasher darf nicht null sein.");
        this.zweiFaktorPruefung = Objects.requireNonNull(
                zweiFaktorPruefung, "ZweiFaktorPruefung darf nicht null sein.");
    }

    /**
     * Authentifiziert ein Konto anhand von Benutzername, Passwort und 5-stelligem Einmalcode.
     *
     * @param benutzername der Anmeldename
     * @param passwort     das eingegebene Klartextpasswort
     * @param einmalCode   der eingegebene 5-stellige 2FA-Einmalcode
     * @return das authentifizierte {@link NutzerKonto} oder {@link Optional#empty()} bei ungültigen Angaben
     */
    public Optional<NutzerKonto> anmelden(String benutzername, String passwort, String einmalCode) {
        if (benutzername == null || benutzername.isBlank() || passwort == null || passwort.isEmpty()) {
            return Optional.empty();
        }

        EinmalCode gepruefterCode;
        try {
            gepruefterCode = new EinmalCode(einmalCode);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        return kontoRepository.findeNachBenutzername(benutzername.strip())
                .filter(konto -> passwortHasher.pruefe(passwort, konto.passwortHash()))
                .filter(konto -> zweiFaktorPruefung.pruefeCode(konto.id(), gepruefterCode));
    }
}
