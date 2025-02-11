package com.sellect.server.tmp.repository;

import com.sellect.server.tmp.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductTempRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p where p.name LIKE %:keyword%")
    List<Product> findContainingName(@Param("keyword") String keyword);

    List<Product> findByIdIn(List<Long> ids);
}
