package de.mse.votee_backend.abstimmungsverwaltung.infrastructure;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbTransaction;

import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmOption;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Abstimmung;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmungId;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Abstimmungszeitraum;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.NutzerId;
import de.mse.votee_backend.abstimmungsverwaltung.domain.repository.AbstimmungRepository;

/**
 * H2-/JDBC-Adapter für den Repository-Port {@link AbstimmungRepository} auf Basis des Helidon DbClient.
 */
public class H2AbstimmungRepository implements AbstimmungRepository {

    private static final String INSERT_ABSTIMMUNG = """
            INSERT INTO abstimmungen (id, erstellt_von, titel, beschreibung, beginn, ende)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_OPTION = """
            INSERT INTO abstimm_optionen (abstimmung_id, position, text)
            VALUES (?, ?, ?)
            """;

    private static final String SELECT_NACH_ID = """
            SELECT id, erstellt_von, titel, beschreibung, beginn, ende
            FROM abstimmungen
            WHERE id = ?
            """;

    private static final String SELECT_ALLE = """
            SELECT id, erstellt_von, titel, beschreibung, beginn, ende
            FROM abstimmungen
            ORDER BY ende ASC, id ASC
            """;

    private static final String SELECT_OPTIONEN_NACH_ID = """
            SELECT text
            FROM abstimm_optionen
            WHERE abstimmung_id = ?
            ORDER BY position ASC
            """;

    private static final String SELECT_ALLE_OPTIONEN = """
            SELECT abstimmung_id, text
            FROM abstimm_optionen
            ORDER BY abstimmung_id ASC, position ASC
            """;

    private final DbClient dbClient;

    public H2AbstimmungRepository(DbClient dbClient) {
        this.dbClient = Objects.requireNonNull(dbClient, "DbClient darf nicht null sein.");
    }

    @Override
    public void speichere(Abstimmung abstimmung) {
        Objects.requireNonNull(abstimmung, "Abstimmung darf nicht null sein.");

        DbTransaction tx = dbClient.transaction();
        try {
            tx.insert(INSERT_ABSTIMMUNG,
                    abstimmung.getId().wert(),
                    abstimmung.getErstelltVon().wert(),
                    abstimmung.getTitel(),
                    abstimmung.getBeschreibung(),
                    Timestamp.valueOf(abstimmung.getZeitraum().beginn()),
                    Timestamp.valueOf(abstimmung.getZeitraum().ende()));

            List<AbstimmOption> optionen = abstimmung.getOptionen();
            for (int position = 0; position < optionen.size(); position++) {
                tx.insert(INSERT_OPTION,
                        abstimmung.getId().wert(),
                        position,
                        optionen.get(position).text());
            }

            tx.commit();
        } catch (RuntimeException e) {
            try {
                tx.rollback();
            } catch (RuntimeException rollbackFehler) {
                e.addSuppressed(rollbackFehler);
            }
            throw e;
        }
    }

    @Override
    public Optional<Abstimmung> findeNachId(AbstimmungId id) {
        Objects.requireNonNull(id, "AbstimmungId darf nicht null sein.");

        Optional<AbstimmungZeile> kopf = dbClient.execute()
                .get(SELECT_NACH_ID, id.wert())
                .map(H2AbstimmungRepository::leseKopfdaten);

        if (kopf.isEmpty()) {
            return Optional.empty();
        }

        List<AbstimmOption> optionen = ladeOptionenFuer(id.wert());
        return Optional.of(baueAbstimmung(kopf.get(), optionen));
    }

    @Override
    public List<Abstimmung> findeAlle() {
        List<AbstimmungZeile> koepfe;
        try (Stream<DbRow> zeilen = dbClient.execute().query(SELECT_ALLE)) {
            koepfe = zeilen.map(H2AbstimmungRepository::leseKopfdaten).toList();
        }

        if (koepfe.isEmpty()) {
            return List.of();
        }

        Map<String, List<AbstimmOption>> optionenJeAbstimmung = ladeAlleOptionen();
        return koepfe.stream()
                .map(kopf -> baueAbstimmung(kopf, optionenJeAbstimmung.getOrDefault(kopf.id(), List.of())))
                .toList();
    }

    private List<AbstimmOption> ladeOptionenFuer(String abstimmungId) {
        try (Stream<DbRow> zeilen = dbClient.execute().query(SELECT_OPTIONEN_NACH_ID, abstimmungId)) {
            return zeilen
                    .map(zeile -> new AbstimmOption(zeile.column("text").asString().get()))
                    .toList();
        }
    }

    private Map<String, List<AbstimmOption>> ladeAlleOptionen() {
        Map<String, List<AbstimmOption>> gruppiert = new HashMap<>();
        try (Stream<DbRow> zeilen = dbClient.execute().query(SELECT_ALLE_OPTIONEN)) {
            zeilen.forEach(zeile -> {
                String abstimmungId = zeile.column("abstimmung_id").asString().get();
                String text = zeile.column("text").asString().get();
                gruppiert.computeIfAbsent(abstimmungId, schluessel -> new ArrayList<>())
                        .add(new AbstimmOption(text));
            });
        }
        return gruppiert;
    }

    private static AbstimmungZeile leseKopfdaten(DbRow zeile) {
        return new AbstimmungZeile(
                zeile.column("id").asString().get(),
                zeile.column("erstellt_von").asString().get(),
                zeile.column("titel").asString().get(),
                zeile.column("beschreibung").asString().get(),
                leseZeitpunkt(zeile, "beginn"),
                leseZeitpunkt(zeile, "ende"));
    }

    private static LocalDateTime leseZeitpunkt(DbRow zeile, String spalte) {
        Object wert = zeile.column(spalte).get();
        if (wert instanceof LocalDateTime zeitpunkt) {
            return zeitpunkt;
        }
        if (wert instanceof Timestamp zeitstempel) {
            return zeitstempel.toLocalDateTime();
        }
        throw new IllegalStateException("Unerwarteter Zeitstempel-Typ in Spalte " + spalte + ": " + wert);
    }

    private static Abstimmung baueAbstimmung(AbstimmungZeile kopf, List<AbstimmOption> optionen) {
        return Abstimmung.erstelle(
                new AbstimmungId(kopf.id()),
                new NutzerId(kopf.erstelltVon()),
                kopf.titel(),
                kopf.beschreibung(),
                new Abstimmungszeitraum(kopf.beginn(), kopf.ende()),
                optionen);
    }

    private record AbstimmungZeile(
            String id,
            String erstelltVon,
            String titel,
            String beschreibung,
            LocalDateTime beginn,
            LocalDateTime ende) {
    }
}
