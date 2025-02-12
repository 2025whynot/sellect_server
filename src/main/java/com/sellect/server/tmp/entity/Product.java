package com.sellect.server.tmp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_temp")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Float price;
    private String name;
    private Integer stock;
}
