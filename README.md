# TP2 - Synchronisation des bases avec RabbitMQ

Projet Java simple base sur l'enonce:
- 2 bases agence (`sales_bo1`, `sales_bo2`)
- 1 base siege (`sales_ho`)
- JDBC MySQL + RabbitMQ

## 1) Creer les bases et les tables

```sql
CREATE DATABASE IF NOT EXISTS sales_bo1;
CREATE DATABASE IF NOT EXISTS sales_bo2;
CREATE DATABASE IF NOT EXISTS sales_ho;

CREATE TABLE IF NOT EXISTS sales_bo1.sales (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    sold_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    office VARCHAR(10) NOT NULL DEFAULT 'bo1',
    synced BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS sales_bo2.sales (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    sold_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    office VARCHAR(10) NOT NULL DEFAULT 'bo2',
    synced BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS sales_ho.sales_aggregated (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_office VARCHAR(10) NOT NULL,
    source_sale_id BIGINT NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    sold_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_source_sale (source_office, source_sale_id)
);
```

Exemples de donnees:

```sql
INSERT INTO sales_bo1.sales (product_code, quantity, unit_price, office)
VALUES ('P-100', 2, 25.00, 'bo1'), ('P-200', 1, 199.99, 'bo1');

INSERT INTO sales_bo2.sales (product_code, quantity, unit_price, office)
VALUES ('P-300', 3, 15.50, 'bo2'), ('P-400', 4, 11.20, 'bo2');
```

## 2) Configurer `db.properties`

Mettre le mot de passe MySQL pour `db.bo1.password`, `db.bo2.password`, `db.ho.password`.

## 3) Lancer la synchronisation

Terminal 1 (siege):

```bash
mvn -q exec:java "-Dexec.mainClass=com.tp2.sync.HeadOfficeConsumer"
```

Terminal 2 (agence 1):

```bash
mvn -q exec:java "-Dexec.mainClass=com.tp2.sync.BranchPublisher" "-Dexec.args=bo1"
```

Terminal 3 (agence 2):

```bash
mvn -q exec:java "-Dexec.mainClass=com.tp2.sync.BranchPublisher" "-Dexec.args=bo2"
```
