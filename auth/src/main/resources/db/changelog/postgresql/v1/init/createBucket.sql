-- liquibase formatted sql

--changeset bucket:createBucket

CREATE TABLE public.bucket (
	id int8 NULL,
	state bytea NULL
);