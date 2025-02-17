package com.sellect.server.search.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellect.server.search.domain.AutoCompleteKeyword;
import com.sellect.server.search.repository.FakeAutoCompleteKeywordRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoCompleteSimpleServiceTest {

    private final FakeAutoCompleteKeywordRepository searchKeywordRepository = new FakeAutoCompleteKeywordRepository();
    private final AutoCompleteService sut = new AutoCompleteSimpleService(searchKeywordRepository);

    @BeforeEach
    void setUp() {
        // "app"으로 시작하는 검색어 저장
        searchKeywordRepository.saveAll(
            List.of(
                AutoCompleteKeyword.builder().id(1L).keyword("app").frequency(1L).build(),
                AutoCompleteKeyword.builder().id(2L).keyword("apple").frequency(3L).build(),
                AutoCompleteKeyword.builder().id(3L).keyword("application").frequency(4L).build(),
                AutoCompleteKeyword.builder().id(4L).keyword("appointment").frequency(2L).build(),
                AutoCompleteKeyword.builder().id(5L).keyword("appearance").frequency(7L).build(),
                AutoCompleteKeyword.builder().id(6L).keyword("approach").frequency(1L).build()
            )
        );

        // "app"으로 시작하지 않는 검색어 저장
        searchKeywordRepository.saveAll(
            List.of(
                AutoCompleteKeyword.builder().id(7L).keyword("ap").frequency(1L).build(),
                AutoCompleteKeyword.builder().id(8L).keyword("abstract").frequency(3L).build(),
                AutoCompleteKeyword.builder().id(9L).keyword("banana").frequency(4L).build(),
                AutoCompleteKeyword.builder().id(10L).keyword("waffle").frequency(2L).build(),
                AutoCompleteKeyword.builder().id(11L).keyword("in-app").frequency(7L).build(),
                AutoCompleteKeyword.builder().id(12L).keyword("customer").frequency(1L).build()
            )
        );
    }

    @Test
    @DisplayName("입력된 접두사 'app'으로 시작하는 검색어 목록을 반환해야 한다.")
    void getAutoCompleteResult_shouldReturnMatchingKeywords() {
        // when
        List<String> results = sut.getAutoCompleteResult("app");

        // then
        assertThat(results).containsExactlyInAnyOrder("app", "apple", "application", "appointment", "appearance", "approach");
    }

    @Test
    @DisplayName("존재하지 않는 접두사로 검색할 경우 빈 리스트를 반환해야 한다.")
    void getAutoCompleteResult_shouldReturnEmptyListWhenNoMatches() {
        // when
        List<String> results = sut.getAutoCompleteResult("xyz");

        // then
        assertThat(results).isEmpty();
    }
}
