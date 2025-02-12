package com.sellect.server.tmp.repository;

import com.sellect.server.tmp.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductTempRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameLike(String keyword);

    List<Product> findByIdIn(List<Long> ids);
}
