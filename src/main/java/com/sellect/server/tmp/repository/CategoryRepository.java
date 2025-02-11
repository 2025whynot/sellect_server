package com.sellect.server.tmp.repository;

import com.sellect.server.tmp.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c where c.name LIKE %:keyword%")
    List<Category> findContainingName(@Param("keyword") String keyword);

    Boolean existsByName(String name);
}
