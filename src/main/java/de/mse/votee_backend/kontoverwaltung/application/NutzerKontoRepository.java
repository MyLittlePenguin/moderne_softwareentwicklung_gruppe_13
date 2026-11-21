package de.mse.votee_backend.kontoverwaltung.application;

import java.util.Optional;

import de.mse.votee_backend.kontoverwaltung.domain.model.NutzerKonto;

/**
 * Repository-Port für Benutzerkonten im Bounded Context {@code kontoverwaltung}.
 */
public interface NutzerKontoRepository {

    /**
     * Sucht ein Benutzerkonto anhand des Benutzernamens.
     *
     * @param benutzername der Anmeldename
     * @return das gefundene Konto oder {@link Optional#empty()}
     */
    Optional<NutzerKonto> findeNachBenutzername(String benutzername);

    /**
     * Sucht ein Benutzerkonto anhand seiner eindeutigen ID.
     *
     * @param id die Konto-ID
     * @return das gefundene Konto oder {@link Optional#empty()}
     */
    Optional<NutzerKonto> findeNachId(String id);
}
