package com.sellect.server.search.repository.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchKeywordJpaRepository extends JpaRepository<SearchKeywordEntity, Long> {

    @Query("SELECT s FROM SearchKeywordEntity s "
        + "WHERE s.keyword LIKE :prefix% "
        + "ORDER BY s.frequency DESC "
        + "LIMIT :limit")
    List<SearchKeywordEntity> findTopByKeywordStartingWith(
        @Param("prefix") String prefix,
        @Param("limit") int limit
    );

}
