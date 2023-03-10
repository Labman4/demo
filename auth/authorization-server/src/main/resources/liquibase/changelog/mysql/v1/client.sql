CREATE TABLE client (
  `id` varchar(255) NOT NULL,
  `authorization_grant_types` varchar(255) DEFAULT NULL,
  `client_authentication_methods` varchar(255) DEFAULT NULL,
  `client_id` varchar(255) DEFAULT NULL,
  `client_id_issued_at` datetime(6) DEFAULT NULL,
  `client_name` varchar(255) DEFAULT NULL,
  `client_secret` varchar(255) DEFAULT NULL,
  `client_secret_expires_at` datetime(6) DEFAULT NULL,
  `client_settings` varchar(255) DEFAULT NULL,
  `redirect_uris` varchar(255) DEFAULT NULL,
  `scopes` varchar(255) DEFAULT NULL,
  `token_settings` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;