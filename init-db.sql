-- Create databases and users for each service
CREATE DATABASE IF NOT EXISTS users_db;
CREATE DATABASE IF NOT EXISTS products_db;
CREATE DATABASE IF NOT EXISTS orders_db;

CREATE USER IF NOT EXISTS 'user_service'@'%' IDENTIFIED BY 'user123';
CREATE USER IF NOT EXISTS 'product_service'@'%' IDENTIFIED BY 'product123';
CREATE USER IF NOT EXISTS 'order_service'@'%' IDENTIFIED BY 'order123';

GRANT ALL PRIVILEGES ON users_db.* TO 'user_service'@'%';
GRANT ALL PRIVILEGES ON products_db.* TO 'product_service'@'%';
GRANT ALL PRIVILEGES ON orders_db.* TO 'order_service'@'%';

FLUSH PRIVILEGES;

