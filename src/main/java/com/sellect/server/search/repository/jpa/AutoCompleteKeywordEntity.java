package com.sellect.server.search.repository.jpa;

import com.sellect.server.common.BaseTimeEntity;
import com.sellect.server.search.domain.AutoCompleteKeyword;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "auto_complete_keyword")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AutoCompleteKeywordEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyword;

    private Long frequency; // null일 경우 온전한 검색어가 아님을 의미

    public static AutoCompleteKeywordEntity from(AutoCompleteKeyword autoCompleteKeyword) {
        return AutoCompleteKeywordEntity.builder()
            .id(autoCompleteKeyword.getId())
            .keyword(autoCompleteKeyword.getKeyword())
            .frequency(autoCompleteKeyword.getFrequency())
            .createdAt(autoCompleteKeyword.getCreatedAt()) // BaseTimeEntity 필드 포함
            .updatedAt(autoCompleteKeyword.getUpdatedAt()) // BaseTimeEntity 필드 포함
            .deleteAt(autoCompleteKeyword.getDeleteAt()) // BaseTimeEntity 필드 포함
            .build();
    }

    public AutoCompleteKeyword toModel() {
        return AutoCompleteKeyword.builder()
            .id(this.id)
            .keyword(this.keyword)
            .frequency(this.frequency)
            .createdAt(this.getCreatedAt()) // BaseTimeEntity 필드 포함
            .updatedAt(this.getUpdatedAt()) // BaseTimeEntity 필드 포함
            .deleteAt(this.getDeleteAt()) // BaseTimeEntity 필드 포함
            .build();
    }
}
