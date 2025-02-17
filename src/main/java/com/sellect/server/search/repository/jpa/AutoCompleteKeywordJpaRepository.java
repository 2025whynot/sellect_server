package com.sellect.server.search.repository.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AutoCompleteKeywordJpaRepository extends JpaRepository<AutoCompleteKeywordEntity, Long> {

    @Query("SELECT s FROM AutoCompleteKeywordEntity s "
        + "WHERE s.keyword LIKE :prefix% "
        + "ORDER BY s.frequency DESC "
        + "LIMIT :limit")
    List<AutoCompleteKeywordEntity> findTopByKeywordStartingWith(
        @Param("prefix") String prefix,
        @Param("limit") int limit
    );

}
