
CREATE TABLE authorization_consent (
  principal_name varchar(255) NOT NULL,
  registered_client_id varchar(255) NOT NULL,
  authorities varchar(255) DEFAULT NULL,
  PRIMARY KEY (principal_name,registered_client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
