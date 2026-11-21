package de.mse.votee_backend.kontoverwaltung.infrastructure;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import de.mse.votee_backend.kontoverwaltung.application.PasswortHasher;

/**
 * Passwort-Hasher auf Basis von Standard-Java {@code PBKDF2WithHmacSHA256} mit zufälligem Salt.
 * Format: {@code pbkdf2$<iterationen>$<base64Salt>$<base64Hash>}.
 */
public class Pbkdf2PasswortHasher implements PasswortHasher {

    private static final String ALGORITHMUS = "PBKDF2WithHmacSHA256";
    private static final String PRAEFIX = "pbkdf2";
    private static final int STANDARD_ITERATIONEN = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int SCHLUESSEL_BITS = 256;

    private final SecureRandom zufall = new SecureRandom();
    private final int iterationen;

    public Pbkdf2PasswortHasher() {
        this(STANDARD_ITERATIONEN);
    }

    Pbkdf2PasswortHasher(int iterationen) {
        if (iterationen <= 0) {
            throw new IllegalArgumentException("Iterationszahl muss positiv sein.");
        }
        this.iterationen = iterationen;
    }

    @Override
    public String hashe(String klartextPasswort) {
        if (klartextPasswort == null || klartextPasswort.isEmpty()) {
            throw new IllegalArgumentException("Passwort darf nicht leer sein.");
        }
        byte[] salt = new byte[SALT_BYTES];
        zufall.nextBytes(salt);
        byte[] hash = berechneHash(klartextPasswort.toCharArray(), salt, iterationen);
        Base64.Encoder encoder = Base64.getEncoder();
        return PRAEFIX + "$" + iterationen + "$"
                + encoder.encodeToString(salt) + "$"
                + encoder.encodeToString(hash);
    }

    @Override
    public boolean pruefe(String klartextPasswort, String gespeicherterHash) {
        if (klartextPasswort == null || klartextPasswort.isEmpty()
                || gespeicherterHash == null || gespeicherterHash.isBlank()) {
            return false;
        }
        String[] teile = gespeicherterHash.split("\\$");
        if (teile.length != 4 || !PRAEFIX.equals(teile[0])) {
            return false;
        }
        try {
            int gespeicherteIterationen = Integer.parseInt(teile[1]);
            if (gespeicherteIterationen <= 0) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(teile[2]);
            byte[] erwartet = Base64.getDecoder().decode(teile[3]);
            byte[] berechnet = berechneHash(klartextPasswort.toCharArray(), salt, gespeicherteIterationen);
            return MessageDigest.isEqual(erwartet, berechnet);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] berechneHash(char[] passwort, byte[] salt, int iterationen) {
        PBEKeySpec spec = new PBEKeySpec(passwort, salt, iterationen, SCHLUESSEL_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHMUS);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2-Hashing nicht verfügbar.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
