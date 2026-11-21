package de.mse.votee_backend.abstimmungsverwaltung.domain.repository;

import java.util.List;
import java.util.Optional;

import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Abstimmung;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmungId;

/**
 * Repository-Port (Hexagonale Architektur) für das Aggregat {@link Abstimmung}.
 */
public interface AbstimmungRepository {

    /**
     * Speichert eine Abstimmung dauerhaft.
     *
     * @param abstimmung die zu speichernde Abstimmung
     */
    void speichere(Abstimmung abstimmung);

    /**
     * Sucht eine Abstimmung anhand ihrer eindeutigen Identität.
     *
     * @param id die ID der Abstimmung
     * @return die gefundene Abstimmung oder {@link Optional#empty()}
     */
    Optional<Abstimmung> findeNachId(AbstimmungId id);

    /**
     * Liefert alle gespeicherten Abstimmungen.
     *
     * @return Liste aller Abstimmungen
     */
    List<Abstimmung> findeAlle();
}
