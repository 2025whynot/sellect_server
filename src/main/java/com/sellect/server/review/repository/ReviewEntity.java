package com.sellect.server.review.repository;

import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.common.BaseTimeEntity;
import com.sellect.server.product.repository.ProductEntity;
import com.sellect.server.review.domain.Review;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@Table(name = "review")
public class ReviewEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    private String description;

    private float rating;

    public static ReviewEntity from(Review review) {
        return ReviewEntity.builder()
            .id(review.getId())
            .userEntity(UserEntity.from(review.getUser()))
            .productEntity(ProductEntity.from(review.getProduct()))
            .rating(review.getRating())
            .description(review.getDescription())
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getUpdatedAt())
            .deleteAt(review.getDeleteAt())
            .build();
    }

    public Review toModel() {
        return Review.builder()
            .id(id)
            .user(this.userEntity.toModel())
            .product(this.productEntity.toModel())
            .rating(this.rating)
            .description(this.description)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(this.getDeleteAt())
            .build();
    }
}
