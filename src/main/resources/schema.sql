CREATE TABLE IF NOT EXISTS abstimmungen (
    id VARCHAR(64) PRIMARY KEY,
    erstellt_von VARCHAR(100) NOT NULL,
    titel VARCHAR(100) NOT NULL,
    beschreibung TEXT NOT NULL,
    beginn TIMESTAMP NOT NULL,
    ende TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS abstimm_optionen (
    abstimmung_id VARCHAR(64) NOT NULL,
    position INT NOT NULL,
    text VARCHAR(500) NOT NULL,
    PRIMARY KEY (abstimmung_id, position),
    FOREIGN KEY (abstimmung_id) REFERENCES abstimmungen(id) ON DELETE CASCADE
);
