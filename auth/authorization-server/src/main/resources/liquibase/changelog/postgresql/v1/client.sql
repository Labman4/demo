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