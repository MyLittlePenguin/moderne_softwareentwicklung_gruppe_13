package de.mse.votee_backend.kontoverwaltung.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.mse.votee_backend.kontoverwaltung.domain.model.NutzerKonto;
import de.mse.votee_backend.kontoverwaltung.domain.model.Rolle;

/**
 * Tests für die serverseitige Session- und CSRF-Token-Verwaltung.
 */
@DisplayName("SessionStore")
class SessionStoreTest {

    private static final NutzerKonto ADMIN = new NutzerKonto("admin-1", "admin", "hash", Rolle.ADMIN);

    @Test
    @DisplayName("Erzeugt eindeutige Sessions mit CSRF-Token und findet gültige Sessions")
    void erzeugtUndFindetSession() {
        Clock clock = Clock.fixed(Instant.parse("2027-01-15T10:00:00Z"), ZoneId.of("UTC"));
        SessionStore store = new SessionStore(clock, Duration.ofMinutes(30));

        Session session1 = store.erzeugeSession(ADMIN);
        Session session2 = store.erzeugeSession(ADMIN);

        assertNotNull(session1.sessionId());
        assertNotNull(session1.csrfToken());
        assertNotEquals(session1.sessionId(), session2.sessionId());
        assertNotEquals(session1.csrfToken(), session2.csrfToken());

        Optional<Session> gefunden = store.findeGueltigeSession(session1.sessionId());
        assertTrue(gefunden.isPresent());
        assertEquals("admin-1", gefunden.get().nutzerId());
        assertTrue(gefunden.get().istAdministrator());
    }

    @Test
    @DisplayName("Abgelaufene Session wird nicht mehr akzeptiert")
    void abgelaufeneSessionWirdUngueltig() {
        AtomicReference<Instant> jetzt = new AtomicReference<>(Instant.parse("2027-01-15T10:00:00Z"));
        Clock beweglicheClock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return jetzt.get();
            }
        };

        SessionStore store = new SessionStore(beweglicheClock, Duration.ofMinutes(30));
        Session session = store.erzeugeSession(ADMIN);

        jetzt.set(Instant.parse("2027-01-15T10:29:59Z"));
        assertTrue(store.findeGueltigeSession(session.sessionId()).isPresent());

        jetzt.set(Instant.parse("2027-01-15T10:30:01Z"));
        assertTrue(store.findeGueltigeSession(session.sessionId()).isEmpty());
    }

    @Test
    @DisplayName("Logout entfernt die Session dauerhaft")
    void entferneSessionBeendetSitzung() {
        Clock clock = Clock.fixed(Instant.parse("2027-01-15T10:00:00Z"), ZoneId.of("UTC"));
        SessionStore store = new SessionStore(clock, Duration.ofMinutes(30));
        Session session = store.erzeugeSession(ADMIN);

        store.entferneSession(session.sessionId());

        assertTrue(store.findeGueltigeSession(session.sessionId()).isEmpty());
        assertTrue(store.findeGueltigeSession(null).isEmpty());
    }

    @Test
    @DisplayName("CSRF-Token-Prüfung akzeptiert nur das passende Token der Session")
    void prueftCsrfToken() {
        Clock clock = Clock.fixed(Instant.parse("2027-01-15T10:00:00Z"), ZoneId.of("UTC"));
        SessionStore store = new SessionStore(clock, Duration.ofMinutes(30));
        Session session = store.erzeugeSession(ADMIN);

        assertTrue(store.pruefeCsrfToken(session, session.csrfToken()));
        assertFalse(store.pruefeCsrfToken(session, "falsches-token"));
        assertFalse(store.pruefeCsrfToken(session, ""));
        assertFalse(store.pruefeCsrfToken(session, null));
        assertFalse(store.pruefeCsrfToken(null, session.csrfToken()));
    }
}
