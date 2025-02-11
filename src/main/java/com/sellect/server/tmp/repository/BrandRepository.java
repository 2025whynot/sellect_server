package com.sellect.server.tmp.repository;

import com.sellect.server.tmp.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    @Query("SELECT b FROM Brand b WHERE b.name LIKE %:keyword%")
    List<Brand> findContainingName(@Param("keyword") String keyword);

    Boolean existsByName(String name);
}
