#!/bin/bash
set -eu
# Passwords are generated as hexadecimal strings by scripts/local.py.
for service in account driver ride fare_payment; do
  variable="${service^^}_DB_PASSWORD"
  password="${!variable}"
  [[ "$password" =~ ^[a-f0-9]{32,}$ ]] || { echo 'Invalid generated database password'; exit 1; }
  MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS ridelink_${service}_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'ridelink_${service}'@'%' IDENTIFIED BY '${password}';
GRANT ALL PRIVILEGES ON ridelink_${service}_db.* TO 'ridelink_${service}'@'%';
SQL
done
