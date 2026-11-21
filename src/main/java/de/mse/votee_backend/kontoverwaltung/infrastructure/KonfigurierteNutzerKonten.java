package de.mse.votee_backend.kontoverwaltung.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.mse.votee_backend.kontoverwaltung.application.NutzerKontoRepository;
import de.mse.votee_backend.kontoverwaltung.domain.model.NutzerKonto;

/**
 * Konfigurationsbasierte Implementierung von {@link NutzerKontoRepository} für den ersten Durchstich
 * (fester Administrator-Account und ggf. Test-Bürgerkonto, gemäß Implementierungsstrategie).
 */
public class KonfigurierteNutzerKonten implements NutzerKontoRepository {

    private final Map<String, NutzerKonto> nachId;
    private final Map<String, NutzerKonto> nachBenutzername;

    public KonfigurierteNutzerKonten(List<NutzerKonto> konten) {
        Objects.requireNonNull(konten, "Kontenliste darf nicht null sein.");
        Map<String, NutzerKonto> idMap = new HashMap<>();
        Map<String, NutzerKonto> nameMap = new HashMap<>();
        for (NutzerKonto konto : konten) {
            Objects.requireNonNull(konto, "NutzerKonto darf nicht null sein.");
            idMap.put(konto.id(), konto);
            nameMap.put(konto.benutzername().toLowerCase(Locale.ROOT), konto);
        }
        this.nachId = Map.copyOf(idMap);
        this.nachBenutzername = Map.copyOf(nameMap);
    }

    @Override
    public Optional<NutzerKonto> findeNachBenutzername(String benutzername) {
        if (benutzername == null || benutzername.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nachBenutzername.get(benutzername.strip().toLowerCase(Locale.ROOT)));
    }

    @Override
    public Optional<NutzerKonto> findeNachId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nachId.get(id.strip()));
    }
}
