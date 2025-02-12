CREATE TABLE `cart`
(
    `id`         BIGINT   NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `product_id` BIGINT   NOT NULL,
    `stock`      INT      NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `data_user_behavior`
(
    `user_id`          BIGINT NULL,
    `clicked_products` JSON NULL,
    `wishlist`         JSON NULL,
    `purchased`        JSON NULL,
    `created_at`       DATETIME NULL,
    `updated_at`       DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `delete_at`       DATETIME NULL
);

CREATE TABLE `user_received_coupon`
(
    `id`         BIGINT   NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `coupon_id`  BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `payment`
(
    `id`         BIGINT         NOT NULL,
    `orders_id`  BIGINT         NOT NULL,
    `price`      DECIMAL(10, 2) NOT NULL,
    `created_at` DATETIME NULL,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `category`
(
    `id`         BIGINT       NOT NULL,
    `name`       VARCHAR(255) NOT NULL,
    `parent_id`  BIGINT NULL,
    `depth`      TINYINT NULL,
    `created_at` DATETIME NULL,
    `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `review_helpful_vote`
(
    `id`         BIGINT   NOT NULL,
    `review_id`  BIGINT   NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `user_auth`
(
    `id`         BIGINT       NOT NULL,
    `user_id`    BIGINT       NOT NULL,
    `email`      VARCHAR(255) NOT NULL,
    `password`   VARCHAR(255) NOT NULL,
    `created_at` DATETIME     NOT NULL,
    `updated_at` DATETIME     NOT NULL,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `product_image`
(
    `id`             BIGINT       NOT NULL,
    `product_id`     BIGINT       NOT NULL,
    `image_url`      VARCHAR(255) NOT NULL,
    `representative` TINYINT(1) NOT NULL,
    `sequence`       INT UNSIGNED NOT NULL,
    `created_at`     DATETIME     NOT NULL,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `delete_at`     DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `preferred_brand`
(
    `id`         BIGINT NOT NULL,
    `user_id`    BIGINT NOT NULL,
    `brand_id`   BIGINT NOT NULL,
    `created_at` DATETIME NULL,
    `updated_at` DATETIME NULL,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `brand`
(
    `id`         BIGINT       NOT NULL,
    `name`       VARCHAR(255) NOT NULL,
    `created_at` DATETIME NULL,
    `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `orders`
(
    `id`                      BIGINT         NOT NULL,
    `user_id`                 BIGINT         NOT NULL,
    `user_received_coupon_id` BIGINT NULL,
    `price`                   DECIMAL(10, 2) NOT NULL,
    `order_number`            VARCHAR(50)    NOT NULL,
    `created_at`              DATETIME       NOT NULL,
    `delete_at`              DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `coupon`
(
    `id`            BIGINT   NOT NULL,
    `seller_id`     BIGINT   NOT NULL,
    `discount_cost` INT      NOT NULL,
    `quantity`      INT      NOT NULL,
    `expired_at`    DATETIME NOT NULL,
    `created_at`    DATETIME NOT NULL,
    `updated_at`    DATETIME NOT NULL,
    `delete_at`    DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `order_item`
(
    `id`         BIGINT NOT NULL,
    `orders_id`  BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity`   INT UNSIGNED NOT NULL,
    `created_at` DATETIME NULL,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `seller`
(
    `id`         BIGINT       NOT NULL,
    `uuid`       VARCHAR(36)  NOT NULL,
    `nickname`   VARCHAR(255) NOT NULL,
    `created_at` DATETIME     NOT NULL,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `seller_auth`
(
    `id`         BIGINT       NOT NULL,
    `seller_id`  BIGINT       NOT NULL,
    `email`      VARCHAR(255) NOT NULL,
    `password`   VARCHAR(255) NOT NULL,
    `created_at` DATETIME     NOT NULL,
    `updated_at` DATETIME     NOT NULL,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `product`
(
    `id`          BIGINT         NOT NULL,
    `seller_id`   BIGINT         NOT NULL,
    `category_id` BIGINT         NOT NULL,
    `brand_id`    BIGINT         NOT NULL,
    `price`       DECIMAL(10, 2) NOT NULL,
    `name`        VARCHAR(255)   NOT NULL,
    `stock`       INT UNSIGNED NOT NULL,
    `created_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME       NOT NULL,
    `delete_at`  DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `user`
(
    `id`         BIGINT      NOT NULL,
    `uuid`       VARCHAR(50) NOT NULL,
    `nickname`   VARCHAR(50) NOT NULL,
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL,
    `delete_at` DATETIME NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `review`
(
    `id`                 BIGINT   NOT NULL,
    `user_id`            BIGINT   NOT NULL,
    `product_id`         BIGINT   NOT NULL,
    `rating`             FLOAT NULL,
    `text`               TEXT NULL,
    `helpful_vote_count` INT NULL,
    `created_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME NOT NULL,
    `delete_at`         DATETIME NULL,
    PRIMARY KEY (`id`)
);
