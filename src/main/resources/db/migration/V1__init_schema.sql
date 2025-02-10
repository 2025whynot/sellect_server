CREATE TABLE `data_user_behavior`
(
    `user_id`          INT NULL,
    `clicked_products` JSON NULL,
    `wishlist`         JSON NULL,
    `purchased`        JSON NULL,
    `created_at`       DATETIME NULL,
    `updated_at`       DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`       DATETIME NULL
);

CREATE TABLE `payment`
(
    `id`         BIGINT        NOT NULL,
    `orders_id`  BIGINT        NOT NULL,
    `price`      DECIMAL(10, 2) NOT NULL,
    `created_at` DATETIME NULL,
    `deleted_at` DATETIME NULL
);

CREATE TABLE `category`
(
    `id`         BIGINT        NOT NULL,
    `name`       VARCHAR(255)  NOT NULL,
    `parent_id`  BIGINT NULL DEFAULT NULL,  -- 가장 부모는 NULL 사용
    `depth`      TINYINT NULL,
    `created_at` DATETIME NULL,
    `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL
);

CREATE TABLE `review_helpful_vote`
(
    `id`         BIGINT     NOT NULL,
    `review_id`  BIGINT     NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL,
    `deleted_at` DATETIME NULL,
    `user_id`    BIGINT     NOT NULL
);

CREATE TABLE `user_auth`
(
    `id`         BIGINT         NOT NULL,
    `user_id`    BIGINT         NOT NULL,
    `email`      VARCHAR(255)   NOT NULL,
    `password`   VARCHAR(255)   NOT NULL,
    `created_at` DATETIME       NOT NULL,
    `updated_at` DATETIME       NOT NULL,
    `deleted_at` DATETIME NULL
);

CREATE TABLE `product_image`
(
    `id`             BIGINT         NOT NULL,
    `product_id`     BIGINT         NOT NULL,
    `image_url`      VARCHAR(255)   NOT NULL,
    `representative` TINYINT(1)     NOT NULL,
    `sequence`       INT UNSIGNED   NOT NULL,
    `created_at`     DATETIME       NOT NULL,
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`     DATETIME NULL
);

CREATE TABLE `preferred_brand`
(
    `id`         BIGINT NOT NULL,
    `user_id`    BIGINT NOT NULL,
    `brand_id`   BIGINT NOT NULL,
    `created_at` DATETIME NULL,
    `updated_at` DATETIME NULL,
    `deleted_at` DATETIME NULL
);

CREATE TABLE `brand`
(
    `id`         BIGINT         NOT NULL,
    `name`       VARCHAR(255)   NOT NULL,
    `created_at` DATETIME NULL,
    `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL
);

CREATE TABLE `orders`
(
    `id`           BIGINT NOT NULL,
    `user_id`      BIGINT NOT NULL,
    `order_number` VARCHAR(50) NULL,
    `created_at`   DATETIME NULL,
    `deleted_at`   DATETIME NULL
);

CREATE TABLE `order_item`
(
    `id`         BIGINT NOT NULL,
    `orders_id`  BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity`   INT UNSIGNED NOT NULL,
    `created_at` DATETIME NULL,
    `deleted_at` DATETIME NULL
);

CREATE TABLE `seller`
(
    `id`         BIGINT        NOT NULL,
    `uuid`       VARCHAR(36)   NOT NULL,
    `nickname`   VARCHAR(255)  NOT NULL,
    `created_at` DATETIME      NOT NULL,
    `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL
);

CREATE TABLE `seller_auth`
(
    `id`         BIGINT         NOT NULL,
    `seller_id`  BIGINT         NOT NULL,
    `email`      VARCHAR(255)   NOT NULL,
    `password`   VARCHAR(255)   NOT NULL,
    `created_at` DATETIME       NOT NULL,
    `updated_at` DATETIME       NOT NULL,
    `deleted_at` DATETIME NULL
);

CREATE TABLE `product`
(
    `id`          BIGINT        NOT NULL,
    `seller_id`   BIGINT        NOT NULL,
    `category_id` BIGINT        NOT NULL,
    `brand_id`    BIGINT        NOT NULL,
    `price`       DECIMAL(10, 2) NOT NULL,
    `name`        VARCHAR(255)  NOT NULL,
    `stock`       INT UNSIGNED  NOT NULL,
    `created_at`  DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME NULL,
    `deleted_at`  DATETIME NULL
);

CREATE TABLE `user`
(
    `id`         BIGINT       NOT NULL,
    `uuid`       VARCHAR(50)  NOT NULL,
    `nickname`   VARCHAR(50)  NOT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL,
    `deleted_at` DATETIME NULL
);

CREATE TABLE `review`
(
    `id`                 BIGINT     NOT NULL,
    `user_id`            BIGINT     NOT NULL,
    `product_id`         BIGINT     NOT NULL,
    `rating`             FLOAT NULL,
    `text`               TEXT NULL,
    `helpful_vote_count` INT NULL,
    `created_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME NOT NULL,
    `deleted_at`         DATETIME NULL
);

-- 🔹 Primary Key 설정
ALTER TABLE `payment`
    ADD CONSTRAINT `PK_PAYMENT` PRIMARY KEY (`id`);

ALTER TABLE `category`
    ADD CONSTRAINT `PK_CATEGORY` PRIMARY KEY (`id`);

ALTER TABLE `review_helpful_vote`
    ADD CONSTRAINT `PK_REVIEW_HELPFUL_VOTE` PRIMARY KEY (`id`);

ALTER TABLE `user_auth`
    ADD CONSTRAINT `PK_USER_AUTH` PRIMARY KEY (`id`);

ALTER TABLE `product_image`
    ADD CONSTRAINT `PK_PRODUCT_IMAGE` PRIMARY KEY (`id`);

ALTER TABLE `preferred_brand`
    ADD CONSTRAINT `PK_PREFERRED_BRAND` PRIMARY KEY (`id`);

ALTER TABLE `brand`
    ADD CONSTRAINT `PK_BRAND` PRIMARY KEY (`id`);

ALTER TABLE `orders`
    ADD CONSTRAINT `PK_ORDERS` PRIMARY KEY (`id`);

ALTER TABLE `order_item`
    ADD CONSTRAINT `PK_ORDER_ITEM` PRIMARY KEY (`id`);

ALTER TABLE `seller`
    ADD CONSTRAINT `PK_SELLER` PRIMARY KEY (`id`);

ALTER TABLE `seller_auth`
    ADD CONSTRAINT `PK_SELLER_AUTH` PRIMARY KEY (`id`);

ALTER TABLE `product`
    ADD CONSTRAINT `PK_PRODUCT` PRIMARY KEY (`id`);

ALTER TABLE `user`
    ADD CONSTRAINT `PK_USER` PRIMARY KEY (`id`);

ALTER TABLE `review`
    ADD CONSTRAINT `PK_REVIEW` PRIMARY KEY (`id`);
