-- Executado uma unica vez, na criacao do volume do PostgreSQL.
-- Cada microservico tem seu PROPRIO banco e usuario (database per service):
-- um servico nao tem permissao para acessar o banco do outro.

CREATE USER auth_user WITH PASSWORD 'auth_pass';
CREATE DATABASE auth_db OWNER auth_user;
REVOKE ALL ON DATABASE auth_db FROM PUBLIC;

CREATE USER produtos_user WITH PASSWORD 'produtos_pass';
CREATE DATABASE produtos_db OWNER produtos_user;
REVOKE ALL ON DATABASE produtos_db FROM PUBLIC;

CREATE USER vendas_user WITH PASSWORD 'vendas_pass';
CREATE DATABASE vendas_db OWNER vendas_user;
REVOKE ALL ON DATABASE vendas_db FROM PUBLIC;
