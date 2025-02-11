CREATE TABLE `cart`
(
    `id`         BIGINT NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `product_id` BIGINT   NOT NULL,
    `stock`      INT      NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL
);

CREATE TABLE `coupon`
(
    `id`            BIGINT NOT NULL,
    `seller_id`     BIGINT   NOT NULL,
    `discount_cost` INT      NOT NULL,
    `quantity`      INT      NOT NULL,
    `expired_at`    DATETIME NOT NULL,
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NOT NULL,
    `deleted_at`    DATETIME NULL
);


CREATE TABLE `user_received_coupon`
(
    `id`         BIGINT   NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `coupon_id`  BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL,
    `deleted_at` DATETIME NULL
);


ALTER TABLE `cart`
    ADD CONSTRAINT `PK_CART` PRIMARY KEY (`id`);

ALTER TABLE `coupon`
    ADD CONSTRAINT `PK_COUPON` PRIMARY KEY (`id`);

ALTER TABLE `user_received_coupon`
    ADD CONSTRAINT `PK_USER_RECEIVED_COUPON` PRIMARY KEY (`id`);