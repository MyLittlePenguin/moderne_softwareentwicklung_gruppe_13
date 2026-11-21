package de.mse.votee_backend.abstimmungsverwaltung.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmOption;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Abstimmung;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmungId;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Abstimmungszeitraum;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.NutzerId;
import de.mse.votee_backend.abstimmungsverwaltung.domain.repository.AbstimmungRepository;

/**
 * Anwendungsservice für die Verwaltung von Abstimmungen im Bounded Context {@code abstimmungsverwaltung}.
 */
public class AbstimmungService {

    private final AbstimmungRepository repository;
    private final BerechtigungsPruefung berechtigungsPruefung;
    private final Clock clock;

    public AbstimmungService(AbstimmungRepository repository,
                             BerechtigungsPruefung berechtigungsPruefung,
                             Clock clock) {
        this.repository = Objects.requireNonNull(repository, "AbstimmungRepository darf nicht null sein.");
        this.berechtigungsPruefung = Objects.requireNonNull(
                berechtigungsPruefung, "BerechtigungsPruefung darf nicht null sein.");
        this.clock = Objects.requireNonNull(clock, "Clock darf nicht null sein.");
    }

    /**
     * Erstellt eine neue Abstimmung, sofern das anfragende Konto Administrator-Rechte besitzt
     * und alle Fachregeln erfüllt sind.
     *
     * @param erstelltVon die serverseitig ermittelte NutzerId des anfragenden Kontos
     * @param befehl      die Eingabedaten für die neue Abstimmung
     * @return die erzeugte eindeutige {@link AbstimmungId}
     * @throws KeineBerechtigungException falls das Konto keine Administrator-Rechte hat
     */
    public AbstimmungId erstelleAbstimmung(NutzerId erstelltVon, ErstelleAbstimmungBefehl befehl) {
        if (erstelltVon == null || !berechtigungsPruefung.istAdministrator(erstelltVon)) {
            throw new KeineBerechtigungException("Nur Administrator:innen dürfen Abstimmungen erstellen.");
        }
        if (befehl == null) {
            throw new IllegalArgumentException("ErstelleAbstimmungBefehl darf nicht null sein.");
        }

        Abstimmungszeitraum zeitraum = new Abstimmungszeitraum(befehl.beginn(), befehl.ende());
        zeitraum.pruefeBeginnNichtInVergangenheit(LocalDateTime.now(clock));

        List<AbstimmOption> optionen = befehl.optionen() == null
                ? null
                : befehl.optionen().stream().map(AbstimmOption::new).toList();

        Abstimmung abstimmung = Abstimmung.erstelle(
                AbstimmungId.neu(),
                erstelltVon,
                befehl.titel(),
                befehl.beschreibung(),
                zeitraum,
                optionen);

        repository.speichere(abstimmung);
        return abstimmung.getId();
    }

    /**
     * Liefert alle gespeicherten Abstimmungen.
     *
     * @return Liste aller Abstimmungen
     */
    public List<Abstimmung> findeAlle() {
        return repository.findeAlle();
    }

    /**
     * Sucht eine Abstimmung anhand ihrer ID.
     *
     * @param id die ID der Abstimmung
     * @return die gefundene Abstimmung oder {@link Optional#empty()}
     */
    public Optional<Abstimmung> findeNachId(AbstimmungId id) {
        Objects.requireNonNull(id, "AbstimmungId darf nicht null sein.");
        return repository.findeNachId(id);
    }
}
