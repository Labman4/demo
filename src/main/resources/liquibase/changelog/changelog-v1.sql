-- liquibase formatted sql

-- changeset liquibase:1
CREATE TABLE test (
    id INT,
    data VARCHAR(255),
    PRIMARY KEY (id)
);