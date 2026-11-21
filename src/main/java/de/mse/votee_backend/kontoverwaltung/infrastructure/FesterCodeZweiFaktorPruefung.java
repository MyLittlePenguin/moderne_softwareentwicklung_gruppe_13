package de.mse.votee_backend.kontoverwaltung.infrastructure;

import de.mse.votee_backend.kontoverwaltung.application.ZweiFaktorPruefung;
import de.mse.votee_backend.kontoverwaltung.domain.model.EinmalCode;

/**
 * Vorläufiger Adapter für {@link ZweiFaktorPruefung}, der den festen 5-stelligen Code {@code 00000} akzeptiert.
 * Wird später durch eine Datenbank-gestützte Einmalcode-Liste ersetzt.
 */
public class FesterCodeZweiFaktorPruefung implements ZweiFaktorPruefung {

    private static final EinmalCode AKZEPTIERTER_CODE = new EinmalCode("00000");

    @Override
    public boolean pruefeCode(String kontoId, EinmalCode code) {
        if (kontoId == null || kontoId.isBlank() || code == null) {
            return false;
        }
        return AKZEPTIERTER_CODE.equals(code);
    }
}
