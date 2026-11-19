package de.mse.votee_backend.kontoverwaltung.application;

import de.mse.votee_backend.kontoverwaltung.domain.model.EinmalCode;

/**
 * Port für die Prüfung des zweiten Faktors (5-stelliger Einmalcode).
 * Ermöglicht den späteren nahtlosen Austausch des Start-Stubs durch generierte Code-Listen in H2.
 */
public interface ZweiFaktorPruefung {

    /**
     * Prüft, ob der angegebene 5-stellige Einmalcode für das Konto gültig ist.
     *
     * @param kontoId die ID des Kontos
     * @param code    der 5-stellige Einmalcode
     * @return {@code true}, falls der Code gültig ist, sonst {@code false}
     */
    boolean pruefeCode(String kontoId, EinmalCode code);
}
