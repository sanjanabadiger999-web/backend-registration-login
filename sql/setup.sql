-- ============================================================================
-- reglogin database setup for the Registration & Login web application
-- Run as a MySQL user with admin rights, e.g.:
--   mysql -u root -p < sql\setup.sql
--
-- Creates:
--   1) database reglogin
--   2) dedicated application user (reglogin/reglogin123) -- CHANGE IN PRODUCTION
--   3) tables user and jwt_token
-- ============================================================================

CREATE DATABASE IF NOT EXISTS reglogin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Dedicated application user.
CREATE USER IF NOT EXISTS 'reglogin'@'localhost' IDENTIFIED BY 'frontend';
CREATE USER IF NOT EXISTS 'reglogin'@'%' IDENTIFIED BY 'frontend';
GRANT ALL PRIVILEGES ON reglogin.* TO 'reglogin'@'localhost';
GRANT ALL PRIVILEGES ON reglogin.* TO 'reglogin'@'%';
FLUSH PRIVILEGES;

USE reglogin;

-- This table holds a BCrypt hash only. Plain-text passwords must never be stored.
CREATE TABLE IF NOT EXISTS user (
  id       BIGINT       NOT NULL AUTO_INCREMENT,
  name     VARCHAR(50)  NOT NULL,
  password VARCHAR(100) NOT NULL,               -- BCrypt hash ($2a$10$...)
  email    VARCHAR(100) NOT NULL,
  phone    VARCHAR(20)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_user_name  (name),
  UNIQUE KEY uq_user_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS jwt_token (
  tid           BIGINT       NOT NULL AUTO_INCREMENT,
  uid           BIGINT       NOT NULL,
  token         VARCHAR(512) NOT NULL,
  creation_time DATETIME(6)  NOT NULL,
  expiry_time   DATETIME(6)  NOT NULL,
  PRIMARY KEY (tid),
  KEY ix_jwt_uid (uid),
  CONSTRAINT fk_jwt_user FOREIGN KEY (uid) REFERENCES user (id)
) ENGINE=InnoDB;