package com.sellect.server.category.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

  boolean existsByIdAndDeleteAt(Long categoryId, LocalDateTime deleteAt);

  boolean existsByName(String name);

  @Query("SELECT c FROM CategoryEntity c where c.name LIKE %:keyword%")
  List<CategoryEntity> findContainingName(@Param("keyword") String keyword);

  Optional<CategoryEntity> findByName(String name);

}
