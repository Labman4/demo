CREATE TABLE `oauth2_client_registered` (
  `registration_id` varchar(255) NOT NULL,
  `authorization_grant_type` varbinary(255) DEFAULT NULL,
  `client_authentication_method` varbinary(255) DEFAULT NULL,
  `client_id` varchar(255) DEFAULT NULL,
  `client_name` varchar(255) DEFAULT NULL,
  `client_secret` varchar(255) DEFAULT NULL,
  `provider_details` varbinary(2000) DEFAULT NULL,
  `redirect_uri` varchar(255) DEFAULT NULL,
  `scopes` varbinary(1000) DEFAULT NULL,
  PRIMARY KEY (`registration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;