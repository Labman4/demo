CREATE TABLE user_account (
  id BIGINT NOT NULL,
   username VARCHAR(255),
   email VARCHAR(255),
   password VARCHAR(60),
   enabled BOOLEAN NOT NULL,
   is_using2fa BOOLEAN NOT NULL,
   CONSTRAINT pk_user_account PRIMARY KEY (id)
);