-- liquibase formatted sql

--changeset auth:authorization

CREATE TABLE "authorization" (
   id VARCHAR(255) NOT NULL,
   registered_client_id VARCHAR(255),
   principal_name VARCHAR(255),
   authorization_grant_type VARCHAR(255),
   authorized_scopes VARCHAR(1000),
   attributes VARCHAR(4000),
   state VARCHAR(500),
   authorization_code_value VARCHAR(1000),
   authorization_code_issued_at TIMESTAMP WITHOUT TIME ZONE,
   authorization_code_expires_at TIMESTAMP WITHOUT TIME ZONE,
   authorization_code_metadata VARCHAR(255),
   access_token_value VARCHAR(1000),
   access_token_issued_at TIMESTAMP WITHOUT TIME ZONE,
   access_token_expires_at TIMESTAMP WITHOUT TIME ZONE,
   access_token_metadata VARCHAR(1000),
   access_token_type VARCHAR(255),
   access_token_scopes VARCHAR(1000),
   refresh_token_value VARCHAR(1000),
   refresh_token_issued_at TIMESTAMP WITHOUT TIME ZONE,
   refresh_token_expires_at TIMESTAMP WITHOUT TIME ZONE,
   refresh_token_metadata VARCHAR(1000),
   oidc_id_token_value VARCHAR(1000),
   oidc_id_token_issued_at TIMESTAMP WITHOUT TIME ZONE,
   oidc_id_token_expires_at TIMESTAMP WITHOUT TIME ZONE,
   oidc_id_token_metadata VARCHAR(1000),
   oidc_id_token_claims VARCHAR(1000),
   CONSTRAINT pk_authorization PRIMARY KEY (id)
);
--changeset auth:authorization_consent

CREATE TABLE authorization_consent (
   registered_client_id VARCHAR(255) NOT NULL,
   principal_name VARCHAR(255) NOT NULL,
   authorities VARCHAR(1000),
   CONSTRAINT pk_authorizationconsent PRIMARY KEY (registered_client_id, principal_name)
);

--changeset auth:client

CREATE TABLE client (
   client_id VARCHAR(255) NOT NULL,
   id VARCHAR(255) NOT NULL,
   client_id_issued_at TIMESTAMP WITHOUT TIME ZONE,
   client_secret VARCHAR(255),
   client_secret_expires_at TIMESTAMP WITHOUT TIME ZONE,
   client_name VARCHAR(255),
   client_authentication_methods VARCHAR(1000),
   authorization_grant_types VARCHAR(1000),
   redirect_uris VARCHAR(1000),
   scopes VARCHAR(1000),
   client_settings VARCHAR(2000),
   token_settings VARCHAR(2000),
   CONSTRAINT client_un UNIQUE (client_id),
   CONSTRAINT client_pk PRIMARY KEY (id)
);

--changeset auth:oauth2_authorized_client

CREATE TABLE oauth2_authorized_client
(
    client_registration_id  varchar(100)                            NOT NULL,
    principal_name          varchar(200)                            NOT NULL,
    access_token_type       varchar(100)                            NOT NULL,
    access_token_value      varchar(1000)                                  NOT NULL,
    access_token_issued_at  timestamp                               NOT NULL,
    access_token_expires_at timestamp                               NOT NULL,
    access_token_scopes     varchar(1000) DEFAULT NULL,
    refresh_token_value     varchar(1000)         DEFAULT NULL,
    refresh_token_issued_at timestamp     DEFAULT NULL,
    created_at              timestamp     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (client_registration_id, principal_name)
);

--changeset auth:oauth2_client_registered

CREATE TABLE "oauth2_client_registered"
(
    registration_id                 varchar(255)  NOT NULL,
    client_id                       varchar(255)  NOT NULL,
    client_secret                   varchar(255)  DEFAULT NULL,
    client_authentication_method    varchar(1000) DEFAULT NULL,
    authorization_grant_type        varchar(1000) DEFAULT NULL,
    client_name                     varchar(255)  DEFAULT NULL,
    redirect_uri                    varchar(2000) DEFAULT NULL,
    scopes                          _varchar DEFAULT NULL,
    provider_details                bytea DEFAULT NULL,
    PRIMARY KEY (registration_id)
)