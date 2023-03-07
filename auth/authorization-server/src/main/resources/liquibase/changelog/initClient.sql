INSERT INTO public.client
(id, authorization_grant_types, client_authentication_methods, client_id, client_id_issued_at, client_name, client_secret, client_secret_expires_at, client_settings, redirect_uris, scopes, token_settings)
VALUES(gen_random_uuid(), 'client_credentials', 'client_secret_basic', 'init', now(), 'init', '{bcrypt}$2a$10$NXjhNl9sRfH0kjWXolHyh.WDPggqEKX2y/aYicEhSeFoL4sN/1.bS', now() + INTERVAL '1 DAY', '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.token-endpoint-authentication-signing-algorithm":"RS256"}', '', '', '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",300]}');
