package de.mse.votee_backend.abstimmungsverwaltung.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.helidon.dbclient.DbClient;

/**
 * Initialisiert das H2-Datenbankschema aus {@code src/main/resources/schema.sql}.
 */
public final class DatenbankInitialisierung {

    private static final String SCHEMA_PFAD = "/schema.sql";

    private DatenbankInitialisierung() {
    }

    /**
     * Führt alle DDL-Anweisungen aus {@code schema.sql} idempotent aus.
     *
     * @param dbClient der konfigurierte Helidon-DbClient
     */
    public static void initialisiere(DbClient dbClient) {
        Objects.requireNonNull(dbClient, "DbClient darf nicht null sein.");
        String sql = ladeSchemaSql();
        for (String anweisung : sql.split(";")) {
            String bereinigt = anweisung.strip();
            if (!bereinigt.isEmpty()) {
                dbClient.execute().dml(bereinigt);
            }
        }
    }

    private static String ladeSchemaSql() {
        try (InputStream eingabe = DatenbankInitialisierung.class.getResourceAsStream(SCHEMA_PFAD)) {
            if (eingabe == null) {
                throw new IllegalStateException("Schema-Datei nicht im Classpath gefunden: " + SCHEMA_PFAD);
            }
            return new String(eingabe.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Fehler beim Lesen der Schema-Datei: " + SCHEMA_PFAD, e);
        }
    }
}
