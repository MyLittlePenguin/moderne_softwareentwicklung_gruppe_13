package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import java.time.LocalDateTime;

/**
 * Value Object für den Zeitraum einer Abstimmung.
 * Regel 4: Das Ende des Zeitraums liegt nach dem Beginn.
 */
public record Abstimmungszeitraum(LocalDateTime beginn, LocalDateTime ende) {

    public Abstimmungszeitraum {
        if (beginn == null) {
            throw new IllegalArgumentException("Beginn darf nicht null sein.");
        }
        if (ende == null) {
            throw new IllegalArgumentException("Ende darf nicht null sein.");
        }
        if (!ende.isAfter(beginn)) {
            throw new IllegalArgumentException("Das Ende muss nach dem Beginn liegen.");
        }
    }

    /**
     * Prüft, ob ein gegebener Zeitpunkt innerhalb des Abstimmungszeitraums liegt (inklusive Beginn und Ende).
     *
     * @param zeitpunkt der zu prüfende Zeitpunkt
     * @return true, falls der Zeitpunkt im Zeitraum liegt, sonst false
     */
    public boolean enthaelt(LocalDateTime zeitpunkt) {
        if (zeitpunkt == null) {
            throw new IllegalArgumentException("Zeitpunkt darf nicht null sein.");
        }
        return !zeitpunkt.isBefore(beginn) && !zeitpunkt.isAfter(ende);
    }

    /**
     * Prüft, ob der Abstimmungszeitraum zum gegebenen Zeitpunkt bereits abgelaufen ist.
     *
     * @param zeitpunkt der zu prüfende Zeitpunkt
     * @return true, falls der Zeitpunkt nach dem Ende liegt, sonst false
     */
    public boolean istVorbei(LocalDateTime zeitpunkt) {
        if (zeitpunkt == null) {
            throw new IllegalArgumentException("Zeitpunkt darf nicht null sein.");
        }
        return zeitpunkt.isAfter(ende);
    }

    /**
     * Prüft, ob der Abstimmungszeitraum zum gegebenen Zeitpunkt noch in der Zukunft liegt.
     *
     * @param zeitpunkt der zu prüfende Zeitpunkt
     * @return true, falls der Zeitpunkt vor dem Beginn liegt, sonst false
     */
    public boolean istInZukunft(LocalDateTime zeitpunkt) {
        if (zeitpunkt == null) {
            throw new IllegalArgumentException("Zeitpunkt darf nicht null sein.");
        }
        return zeitpunkt.isBefore(beginn);
    }
}
