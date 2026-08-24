-- Cada microservice tem seu próprio database lógico e usuário, na mesma instância
-- PostgreSQL (permitido nesta entrega, contanto que a separação seja explícita).

CREATE USER product_user WITH PASSWORD 'product_pass';
CREATE DATABASE product_db OWNER product_user;

CREATE USER order_user WITH PASSWORD 'order_pass';
CREATE DATABASE order_db OWNER order_user;
