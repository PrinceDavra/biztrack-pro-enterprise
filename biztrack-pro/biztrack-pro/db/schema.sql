-- ============================================================
--  BizTrack Pro — MySQL 8.0 schema
--  Optional: Hibernate (hibernate.hbm2ddl.auto=update) will create/upgrade
--  these tables automatically. Run this script only if you prefer to manage
--  the schema explicitly (set hbm2ddl.auto=validate or none in that case).
-- ============================================================

CREATE DATABASE IF NOT EXISTS biztrackpro
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE biztrackpro;

-- ---------- tenants ----------
CREATE TABLE IF NOT EXISTS tenants (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(255),
  email         VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255),
  created_at    TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tenant_email (email)
) ENGINE=InnoDB;

-- ---------- orders ----------
CREATE TABLE IF NOT EXISTS orders (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id         BIGINT       NOT NULL,
  order_id          VARCHAR(50),
  date              DATE,
  product           TEXT,
  sku               VARCHAR(100),
  qty               INT,
  unit_price        DECIMAL(10,2) DEFAULT 0.00,
  cogs_per_unit     DECIMAL(10,2) DEFAULT 0.00,
  revenue           DECIMAL(10,2) DEFAULT 0.00,
  refund            DECIMAL(10,2) DEFAULT 0.00,
  profit            DECIMAL(10,2) DEFAULT 0.00,
  status            VARCHAR(50),
  shipping_city     VARCHAR(100),
  shipping_province VARCHAR(100),
  source            VARCHAR(50),
  free_items        INT DEFAULT 0,
  item_count        INT DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_order_tenant (order_id, tenant_id),
  KEY idx_orders_tenant (tenant_id),
  KEY idx_orders_date (date),
  CONSTRAINT fk_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB;

-- ---------- expenses ----------
CREATE TABLE IF NOT EXISTS expenses (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id      BIGINT       NOT NULL,
  date           DATE,
  description    TEXT,
  amount         DECIMAL(10,2) DEFAULT 0.00,
  category       VARCHAR(100),
  payment_method VARCHAR(50),
  txn_id         VARCHAR(100),
  source         VARCHAR(50),
  PRIMARY KEY (id),
  KEY idx_expenses_tenant (tenant_id),
  KEY idx_expenses_date (date),
  CONSTRAINT fk_expenses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB;

-- ---------- ad_campaigns ----------
CREATE TABLE IF NOT EXISTS ad_campaigns (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id       BIGINT       NOT NULL,
  date            DATE,
  name            VARCHAR(255),
  platform        VARCHAR(100),
  spend           DECIMAL(10,2) DEFAULT 0.00,
  revenue         DECIMAL(10,2) DEFAULT 0.00,
  roas            DECIMAL(8,4),
  clicks          INT DEFAULT 0,
  conversions     INT DEFAULT 0,
  cpc             DECIMAL(8,2),
  cpl             DECIMAL(8,2),
  impressions     INT DEFAULT 0,
  reach           INT DEFAULT 0,
  cpm             DECIMAL(8,2),
  optimised_for   VARCHAR(50),
  roas_available  BOOLEAN DEFAULT FALSE,
  delivery_status VARCHAR(50),
  source          VARCHAR(50),
  PRIMARY KEY (id),
  KEY idx_ads_tenant (tenant_id),
  KEY idx_ads_date (date),
  CONSTRAINT fk_ads_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB;

-- ---------- product_costs ----------
CREATE TABLE IF NOT EXISTS product_costs (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id    BIGINT       NOT NULL,
  product_name VARCHAR(255),
  sku          VARCHAR(100),
  cost         DECIMAL(10,2) DEFAULT 0.00,
  notes        TEXT,
  PRIMARY KEY (id),
  KEY idx_costs_tenant (tenant_id),
  CONSTRAINT fk_costs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB;

-- ---------- business_profile ----------
CREATE TABLE IF NOT EXISTS business_profile (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id      BIGINT       NOT NULL,
  business_name  VARCHAR(255),
  gstin          VARCHAR(20),
  financial_year VARCHAR(20),
  ca_name        VARCHAR(255),
  PRIMARY KEY (id),
  UNIQUE KEY uq_profile_tenant (tenant_id),
  CONSTRAINT fk_profile_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB;

-- ---------- system_properties (AI Advisor thresholds) ----------
CREATE TABLE IF NOT EXISTS system_properties (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  prop_key    VARCHAR(100) NOT NULL,
  prop_value  VARCHAR(255),
  description VARCHAR(255),
  PRIMARY KEY (id),
  UNIQUE KEY uq_sysprop_key (prop_key)
) ENGINE=InnoDB;

INSERT INTO system_properties (prop_key, prop_value, description) VALUES
  ('advisor.margin.low',        '20', 'Net margin % below which a price/COGS action is suggested'),
  ('advisor.aov.low',          '300', 'AOV (INR) below which bundle/free-shipping is suggested'),
  ('advisor.adspend.danger.pct','50', 'Ad spend as % of revenue considered dangerous'),
  ('advisor.roas.scale',         '3', 'ROAS at/above which campaigns should be scaled'),
  ('advisor.roas.review',        '1', 'ROAS floor (break-even) for review vs pause'),
  ('advisor.orders.loyalty',    '50', 'Order count above which a loyalty campaign is suggested'),
  ('advisor.seasonality.pct',   '50', 'Worst month < this %% of best month flags seasonality')
ON DUPLICATE KEY UPDATE prop_value = VALUES(prop_value);
