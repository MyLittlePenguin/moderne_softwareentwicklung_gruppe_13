package de.mse.votee_backend.kontoverwaltung.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import de.mse.votee_backend.kontoverwaltung.domain.model.NutzerKonto;

/**
 * Serverseitiger In-Memory-Sitzungsspeicher mit kryptographisch sicheren Session-IDs und CSRF-Tokens.
 */
public class SessionStore {

    private static final int TOKEN_BYTES = 32;

    private final Clock clock;
    private final Duration lebensdauer;
    private final SecureRandom zufall = new SecureRandom();
    private final Map<String, Session> sitzungen = new ConcurrentHashMap<>();

    public SessionStore(Clock clock, Duration lebensdauer) {
        this.clock = Objects.requireNonNull(clock, "Clock darf nicht null sein.");
        this.lebensdauer = Objects.requireNonNull(lebensdauer, "Lebensdauer darf nicht null sein.");
        if (lebensdauer.isZero() || lebensdauer.isNegative()) {
            throw new IllegalArgumentException("Lebensdauer muss positiv sein.");
        }
    }

    /**
     * Erzeugt und speichert eine neue Sitzung für ein erfolgreich authentifiziertes Konto.
     *
     * @param konto das angemeldete Konto
     * @return die neu erzeugte {@link Session}
     */
    public Session erzeugeSession(NutzerKonto konto) {
        Objects.requireNonNull(konto, "NutzerKonto darf nicht null sein.");
        String sessionId = generiereToken();
        String csrfToken = generiereToken();
        Instant gueltigBis = clock.instant().plus(lebensdauer);

        Session session = new Session(
                sessionId, konto.id(), konto.benutzername(), konto.rolle(), csrfToken, gueltigBis);
        sitzungen.put(sessionId, session);
        return session;
    }

    /**
     * Liefert die gültige Sitzung zur gegebenen Session-ID. Abgelaufene Sitzungen werden verworfen.
     *
     * @param sessionId die Session-ID aus dem Cookie
     * @return die gültige Sitzung oder {@link Optional#empty()}
     */
    public Optional<Session> findeGueltigeSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        Session session = sitzungen.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        if (session.istAbgelaufen(clock.instant())) {
            sitzungen.remove(sessionId, session);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    /**
     * Beendet eine Sitzung (Logout).
     *
     * @param sessionId die zu entfernende Session-ID
     */
    public void entferneSession(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            sitzungen.remove(sessionId);
        }
    }

    /**
     * Prüft ein übermitteltes CSRF-Token in konstanter Zeit gegen das Token der Sitzung.
     *
     * @param session die aktive Sitzung
     * @param token   das übermittelte CSRF-Token
     * @return {@code true}, falls das Token exakt übereinstimmt
     */
    public boolean pruefeCsrfToken(Session session, String token) {
        if (session == null || token == null || token.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                session.csrfToken().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private String generiereToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        zufall.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
