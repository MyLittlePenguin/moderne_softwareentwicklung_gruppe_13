package de.mse.votee_backend.kontoverwaltung.infrastructure;

import java.util.Objects;

import de.mse.votee_backend.abstimmungsverwaltung.application.BerechtigungsPruefung;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.NutzerId;
import de.mse.votee_backend.kontoverwaltung.application.NutzerKontoRepository;
import de.mse.votee_backend.kontoverwaltung.domain.model.NutzerKonto;

/**
 * Kontext-Adapter zwischen {@code kontoverwaltung} und {@code abstimmungsverwaltung}:
 * Implementiert den Port {@link BerechtigungsPruefung} auf Basis von {@link NutzerKontoRepository}.
 */
public class KontoBerechtigungsAdapter implements BerechtigungsPruefung {

    private final NutzerKontoRepository kontoRepository;

    public KontoBerechtigungsAdapter(NutzerKontoRepository kontoRepository) {
        this.kontoRepository = Objects.requireNonNull(
                kontoRepository, "NutzerKontoRepository darf nicht null sein.");
    }

    @Override
    public boolean istAdministrator(NutzerId nutzerId) {
        if (nutzerId == null) {
            return false;
        }
        return kontoRepository.findeNachId(nutzerId.wert())
                .map(NutzerKonto::istAdministrator)
                .orElse(false);
    }
}
