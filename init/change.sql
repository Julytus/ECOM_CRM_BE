
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

