package com.sellect.server.brand.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {

    @Query("SELECT b FROM BrandEntity b WHERE b.name LIKE %:keyword%")
    List<BrandEntity> findContainingName(@Param("keyword") String keyword);

    Boolean existsByName(String name);
}