-- =====================================================================
-- Tabelas do JdbcUserDetailsManager (schema fixo do Spring Security)
-- Controlam as ROLES de quem acessa os endpoints protegidos
-- =====================================================================
DROP TABLE IF EXISTS authorities;
DROP TABLE IF EXISTS users;

create table users(username varchar(50) not null primary key,password varchar(500) not null, enabled boolean not null);
create table authorities (username varchar(50) not null,authority varchar_ignorecase(50) not null,constraint fk_authorities_users foreign key(username) references users(username));
create unique index ix_auth_username on authorities (username,authority);

-- =====================================================================
-- Tabelas do enunciado
-- (removida a vírgula depois de updated_at, que causava erro de sintaxe)
-- =====================================================================
DROP TABLE IF EXISTS login_attempts;
DROP TABLE IF EXISTS usuarios;

CREATE TABLE IF NOT EXISTS login_attempts (
    username VARCHAR(100) PRIMARY KEY,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    last_failed_at TIMESTAMP NULL,
    locked_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS usuarios (
    username VARCHAR(100) PRIMARY KEY,
    senha VARCHAR(10)
);

-- =====================================================================
-- Usuários iniciais
--   admin      / admin123  -> ROLE_ADMIN       (pode desbloquear usuários)
--   manutencao / manut123  -> ROLE_MANUTENCAO  (pode cadastrar e listar)
--   joao       / joao123   -> ROLE_USER
--   maria      / maria123  -> ROLE_USER
--   carlos     / carlos123 -> ROLE_USER (já começa BLOQUEADO, para testar o unlock)
-- =====================================================================
INSERT INTO usuarios (username, senha) VALUES ('admin', 'admin123');
INSERT INTO usuarios (username, senha) VALUES ('manutencao', 'manut123');
INSERT INTO usuarios (username, senha) VALUES ('joao', 'joao123');
INSERT INTO usuarios (username, senha) VALUES ('maria', 'maria123');
INSERT INTO usuarios (username, senha) VALUES ('carlos', 'carlos123');

insert into users (username, password, enabled) values ('admin', '{noop}admin123', true);
insert into users (username, password, enabled) values ('manutencao', '{noop}manut123', true);
insert into users (username, password, enabled) values ('joao', '{noop}joao123', true);
insert into users (username, password, enabled) values ('maria', '{noop}maria123', true);
insert into users (username, password, enabled) values ('carlos', '{noop}carlos123', false);

insert into authorities (username, authority) values ('admin', 'ROLE_ADMIN');
insert into authorities (username, authority) values ('manutencao', 'ROLE_MANUTENCAO');
insert into authorities (username, authority) values ('joao', 'ROLE_USER');
insert into authorities (username, authority) values ('maria', 'ROLE_USER');
insert into authorities (username, authority) values ('carlos', 'ROLE_USER');

INSERT INTO login_attempts (username, failed_attempts, locked) VALUES ('admin', 0, FALSE);
INSERT INTO login_attempts (username, failed_attempts, locked) VALUES ('manutencao', 0, FALSE);
INSERT INTO login_attempts (username, failed_attempts, locked) VALUES ('joao', 0, FALSE);
INSERT INTO login_attempts (username, failed_attempts, locked) VALUES ('maria', 0, FALSE);
INSERT INTO login_attempts (username, failed_attempts, locked, last_failed_at, locked_at) VALUES ('carlos', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
