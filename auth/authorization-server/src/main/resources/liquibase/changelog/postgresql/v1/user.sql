-- liquibase formatted sql

--changeset auth:user

CREATE TABLE user_account (
	id int8 NOT NULL,
	username varchar(255) NULL,
	email varchar(255) NULL,
	"password" varchar(60) NULL,
	enabled bool NOT NULL,
	is_using2fa bool NOT NULL,
	CONSTRAINT pk_user_account PRIMARY KEY (id),
	CONSTRAINT user_email_un UNIQUE (email),
	CONSTRAINT user_username_un UNIQUE (username)
);

--changeset auth:initUser

INSERT INTO user_account
(id, username, email, "password", enabled, is_using2fa)
VALUES(0, 'lab', 'w', '{noop}man', true, false);


--changeset auth:role

CREATE TABLE role (
  id BIGINT NOT NULL,
   name VARCHAR(255),
   CONSTRAINT pk_role PRIMARY KEY (id)
);


--changeset auth:privilege

CREATE TABLE privilege (
  id BIGINT NOT NULL,
   name VARCHAR(255),
   CONSTRAINT pk_privilege PRIMARY KEY (id)
);

--changeset auth:users_roles

CREATE TABLE users_roles (
	user_id int8 NOT NULL,
	role_id int8 NOT NULL
);

ALTER TABLE public.users_roles ADD CONSTRAINT fkci4mdvg1fmo9eqmwno1y9o0fa FOREIGN KEY (user_id) REFERENCES user_account(id);
ALTER TABLE public.users_roles ADD CONSTRAINT fkt4v0rrweyk393bdgt107vdx0x FOREIGN KEY (role_id) REFERENCES "role"(id);

--changeset auth:roles_privileges

CREATE TABLE roles_privileges (
	role_id int8 NOT NULL,
	privilege_id int8 NOT NULL
);

ALTER TABLE public.roles_privileges ADD CONSTRAINT fk5yjwxw2gvfyu76j3rgqwo685u FOREIGN KEY (privilege_id) REFERENCES privilege(id);
ALTER TABLE public.roles_privileges ADD CONSTRAINT fk9h2vewsqh8luhfq71xokh4who FOREIGN KEY (role_id) REFERENCES "role"(id);