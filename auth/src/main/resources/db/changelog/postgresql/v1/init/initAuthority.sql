-- liquibase formatted sql

--changeset auth:init_authority

INSERT INTO authority
(id, authority)
VALUES(gen_random_uuid(), 'admin');

--changeset auth:init_authority_group

INSERT INTO authority
(id, authority)
VALUES(gen_random_uuid(), 'group');

--changeset auth:init_authority_permission

INSERT INTO authority
(id, authority)
VALUES(gen_random_uuid(), 'permission');
