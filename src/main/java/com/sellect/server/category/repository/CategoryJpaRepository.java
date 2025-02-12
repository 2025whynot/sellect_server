package com.sellect.server.category.repository;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    boolean existsByIdAndDeleteAt(Long categoryId, LocalDateTime deleteAt);
}
