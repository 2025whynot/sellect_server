package com.sellect.server.search.repository.jpa;

import com.blazebit.persistence.CTE;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@CTE
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class MatchedProduct {

    @Id
    public Long id;

    public String name;

    public BigDecimal price;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "brand_id")
    public Long brandId;

    public Integer priority;

}
