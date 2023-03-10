CREATE TABLE "oauth2_client_registered"
(
    registration_id                 varchar(255)  NOT NULL,
    client_id                       varchar(255)  NOT NULL,
    client_secret                   varchar(255)  DEFAULT NULL,
    client_authentication_method    bytea NOT NULL,
    authorization_grant_type        bytea NOT NULL,
    client_name                     varchar(255)  DEFAULT NULL,
    redirect_uri                    varchar(2000) NOT NULL,
    scopes                          _varchar NOT NULL,
    provider_details                bytea DEFAULT NULL,
    PRIMARY KEY (registration_id)
)