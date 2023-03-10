CREATE TABLE authorization_consent (
   registered_client_id VARCHAR(255) NOT NULL,
   principal_name VARCHAR(255) NOT NULL,
   authorities VARCHAR(1000),
   CONSTRAINT pk_authorizationconsent PRIMARY KEY (registered_client_id, principal_name)
);
