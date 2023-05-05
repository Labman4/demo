-- liquibase formatted sql

--changeset auth:init_client

INSERT INTO client
(id, authorization_grant_types, client_authentication_methods, client_id, client_id_issued_at, client_name, client_secret, client_secret_expires_at, client_settings, redirect_uris, scopes, token_settings)
VALUES(gen_random_uuid(), 'client_credentials', 'client_secret_basic', 'init', now(), 'init', '{bcrypt}$2a$10$NXjhNl9sRfH0kjWXolHyh.WDPggqEKX2y/aYicEhSeFoL4sN/1.bS', now() + INTERVAL '1 DAY', '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.token-endpoint-authentication-signing-algorithm":"RS256"}', '', '', '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",300]}');

--changeset auth:init_pkce

INSERT INTO client
(client_id, id, client_id_issued_at, client_secret, client_secret_expires_at, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings)
VALUES('pkce', gen_random_uuid(), now(), '', now() + INTERVAL '1 YEAR', 'pkce', 'none', 'authorization_code,refresh_token', 'https://oauth2-proxy.elpsykongroo.com/oauth2/callback', 'https://elpsykongroo.com', 'openid,policy,email,group,permission', '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.token-endpoint-authentication-signing-algorithm":"RS256","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":true}', '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.refresh-token-time-to-live":["java.time.Duration",18000],"settings.token.reuse-refresh-tokens":true,"settings.token.access-token-time-to-live":["java.time.Duration",1200],"settings.token.authorization-code-time-to-live":["java.time.Duration",7200]}');
