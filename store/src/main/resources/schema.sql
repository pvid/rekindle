CREATE TABLE application_infos(
    id BIGSERIAL NOT NULL PRIMARY KEY,
    application_id VARCHAR(255) NOT NULL,
    attempt_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    start BIGINT NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    UNIQUE(application_id, attempt_id, name, identifier)
);
CREATE TABLE observations(
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    kind VARCHAR(255) NOT NULL,
    UNIQUE(name)
);
CREATE TABLE metrics(
    app_id BIGINT NOT NULL,
    observation_id INT NOT NULL,
    metric_value BIGINT NOT NULL,
    PRIMARY KEY (app_id, observation_id),
    FOREIGN KEY (app_id) REFERENCES application_infos(id) ON DELETE CASCADE,
    FOREIGN KEY (observation_id) REFERENCES observations(id) ON DELETE CASCADE
);
CREATE TABLE indexed_metrics(
    app_id BIGINT NOT NULL,
    observation_id INT NOT NULL,
    index INT NOT NULL,
    metric_value BIGINT NOT NULL,
    PRIMARY KEY (app_id, observation_id, index),
    FOREIGN KEY (app_id) REFERENCES application_infos(id) ON DELETE CASCADE,
    FOREIGN KEY (observation_id) REFERENCES observations(id) ON DELETE CASCADE
);
CREATE TABLE messages(
    app_id BIGINT NOT NULL,
    observation_id INT NOT NULL,
    contents TEXT NOT NULL,
    PRIMARY KEY (app_id, observation_id),
    FOREIGN KEY (app_id) REFERENCES application_infos(id) ON DELETE CASCADE,
    FOREIGN KEY (observation_id) REFERENCES observations(id) ON DELETE CASCADE
);