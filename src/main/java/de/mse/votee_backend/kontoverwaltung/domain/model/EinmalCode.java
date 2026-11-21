package de.mse.votee_backend.kontoverwaltung.domain.model;

import java.util.regex.Pattern;

/**
 * Value Object für einen 2FA-Einmalcode (exakt 5 ASCII-Ziffern).
 */
public record EinmalCode(String wert) {

    private static final Pattern FUENF_ZIFFERN = Pattern.compile("^[0-9]{5}$");

    public EinmalCode {
        if (wert == null || !FUENF_ZIFFERN.matcher(wert).matches()) {
            throw new IllegalArgumentException("Einmalcode muss aus genau 5 Ziffern bestehen.");
        }
    }
}
