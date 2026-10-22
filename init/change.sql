
alter table branch
    modify founding_day date null;

alter table chat
    modify group_chat_image text null;

CREATE TABLE `module` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(300) DEFAULT NULL,
  `code` varchar(100) DEFAULT NULL,
  `description` varchar(300) DEFAULT NULL,
  `active` int DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `module_code_uindex` (`code`),
  KEY `module_active_index` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `resource`
  ADD COLUMN `module_id` int NULL,
  ADD KEY `resource_module_id_index` (`module_id`),
  ADD CONSTRAINT `resource_module_fk` FOREIGN KEY (`module_id`) REFERENCES `module` (`id`);

ALTER TABLE `order`
    ADD COLUMN buyer_name varchar(255) NULL,
    ADD COLUMN buyer_phone varchar(30) NULL,
    ADD COLUMN buyer_email varchar(255) NULL,
    ADD COLUMN receiver_name varchar(255) NULL,
    ADD COLUMN receiver_phone varchar(30) NULL,
    ADD COLUMN shipping_fee double NOT NULL DEFAULT 0,
    ADD COLUMN voucher_id int NULL,
    ADD COLUMN voucher_code varchar(50) NULL,
    ADD COLUMN branch_id int NULL,
    ADD COLUMN customer_id int NULL;

ALTER TABLE invoice
    ADD COLUMN order_id int NULL,
    ADD COLUMN shipping_fee double DEFAULT 0,
    ADD COLUMN payment_method varchar(20) NULL,
    ADD UNIQUE KEY invoice_order_id_uindex (order_id),
    ADD KEY invoice_status_receipt_index (status, receipt_date),
    ADD KEY invoice_created_time_index (created_time);

-- Must return no rows before adding the unique key below:
-- SELECT order_code, COUNT(*) FROM payment WHERE order_code IS NOT NULL GROUP BY order_code HAVING COUNT(*)>1;
ALTER TABLE payment
    ADD COLUMN order_id int NULL,
    ADD KEY payment_invoice_id_index (invoice_id),
    ADD UNIQUE KEY payment_order_code_uindex (order_code);

ALTER TABLE invoice
    MODIFY created_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY updated_time timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `order`
    MODIFY updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

UPDATE invoice i
JOIN `order` o ON o.id = i.order_id
SET i.created_time = o.created_at,
    i.updated_time = COALESCE(i.receipt_date, o.created_at)
WHERE i.created_time IS NULL;

-- Loyalty phase 1 schema
CREATE TABLE `loyalty_tier` (
    `id` int NOT NULL AUTO_INCREMENT,
    `name` varchar(100) NOT NULL,
    `min_rank_points` bigint NOT NULL,
    `earn_multiplier` decimal(6,2) NOT NULL DEFAULT 1.00,
    `status` int DEFAULT 1,
    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `loyalty_tier_min_rank_points_uindex` (`min_rank_points`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `loyalty_tier` (`name`, `min_rank_points`, `earn_multiplier`)
VALUES ('Hạng gốc', 0, 1.00);

CREATE TABLE `loyalty_config` (
    `id` int NOT NULL,
    `earn_rate` int NOT NULL DEFAULT 10000,
    `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `loyalty_config` (`id`) VALUES (1);

CREATE TABLE `loyalty_account` (
    `id` int NOT NULL AUTO_INCREMENT,
    `customer_id` int NOT NULL,
    `balance` bigint NOT NULL DEFAULT 0,
    `rank_points` bigint NOT NULL DEFAULT 0,
    `tier_id` int NULL,
    `version` int NOT NULL DEFAULT 0,
    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `loyalty_account_customer_id_uindex` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `loyalty_transaction` (
    `id` int NOT NULL AUTO_INCREMENT,
    `customer_id` int NOT NULL,
    `type` varchar(20) NOT NULL,
    `points` bigint NOT NULL,
    `rank_points_delta` bigint DEFAULT 0,
    `balance_after` bigint NOT NULL,
    `rank_points_after` bigint NOT NULL,
    `ref_type` varchar(20) NULL,
    `ref_id` varchar(64) NULL,
    `note` varchar(500) NULL,
    `created_by` int NULL,
    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `loyalty_transaction_ref_uindex` (`type`, `ref_type`, `ref_id`),
    KEY `loyalty_transaction_customer_id_id_index` (`customer_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `customer_voucher` (
    `id` int NOT NULL AUTO_INCREMENT,
    `customer_id` int NOT NULL,
    `voucher_id` int NOT NULL,
    `status` varchar(12) NOT NULL,
    `points_spent` bigint NOT NULL,
    `invoice_id` int NULL,
    `expires_at` timestamp NULL,
    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `used_at` timestamp NULL,
    PRIMARY KEY (`id`),
    KEY `customer_voucher_customer_id_status_index` (`customer_id`, `status`),
    KEY `customer_voucher_invoice_id_index` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `voucher`
    ADD COLUMN `points_cost` bigint NULL,
    ADD COLUMN `redeem_stock` int NULL,
    ADD COLUMN `min_tier_id` int NULL,
    ADD COLUMN `redeemed_count` int NOT NULL DEFAULT 0;

-- Must return no rows before adding the unique key below:
-- SELECT account_user_id FROM customer GROUP BY 1 HAVING COUNT(*)>1 AND account_user_id IS NOT NULL;
ALTER TABLE `customer`
    ADD COLUMN `account_user_id` int NULL,
    ADD UNIQUE KEY `customer_account_user_uindex` (`account_user_id`);

ALTER TABLE `order`
    ADD COLUMN `customer_voucher_id` int NULL,
    ADD COLUMN `loyalty_voucher_discount` double NOT NULL DEFAULT 0;

-- Rollback (manual only; do not execute as part of the migration):
-- ALTER TABLE `order` DROP COLUMN `customer_voucher_id`, DROP COLUMN `loyalty_voucher_discount`;
-- ALTER TABLE `customer` DROP INDEX `customer_account_user_uindex`, DROP COLUMN `account_user_id`;
-- ALTER TABLE `voucher` DROP COLUMN `points_cost`, DROP COLUMN `redeem_stock`, DROP COLUMN `min_tier_id`, DROP COLUMN `redeemed_count`;
-- DROP TABLE `customer_voucher`;
-- DROP TABLE `loyalty_transaction`;
-- DROP TABLE `loyalty_account`;
-- DROP TABLE `loyalty_config`;
-- DROP TABLE `loyalty_tier`;

INSERT INTO `resource` (`name`, `description`, `code`, `uri`, `actions`, `active`, `module_id`)
VALUES ('Quản lý loyalty', 'Quản lý hạng, điểm và tài khoản loyalty', 'LOYALTY', '/loyalty/',
        '["VIEW", "ADD", "UPDATE", "DELETE"]', 1, 1);

