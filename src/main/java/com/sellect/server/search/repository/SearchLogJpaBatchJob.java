package com.sellect.server.search.repository;

import com.sellect.server.search.domain.AutoCompleteKeyword;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SearchLogJpaBatchJob {

    private final SearchLogRepository searchLogRepository;
    private final AutoCompleteKeywordRepository autoCompleteKeywordRepository;

    @Transactional
//    @Scheduled(cron = "0 0 2 * * ?") // 매일 오전 2시 실행
    public void updateAutoCompleteKeyword() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();

        // 1. 조회: 24시간 내 검색 로그 가져오기 (JPA 조회, 영속성 컨텍스트 X)
        List<Object[]> searchLogs = searchLogRepository.findSearchLogsForBatch(startTime, endTime);
        System.out.println("배치 시작: " + searchLogs.size() + "개의 검색어 처리 중...");

        // 2. 키워드별 중복 유저 제거 후 빈도수 집계
        Map<String, Set<String>> keywordUserMap = new HashMap<>();
        for (Object[] log : searchLogs) {
            String keyword = (String) log[0];
            String userIdentifier = (String) log[1];

            keywordUserMap.putIfAbsent(keyword, new HashSet<>());
            keywordUserMap.get(keyword).add(userIdentifier); // 같은 키워드는 유저별로 중복 제거
        }

        // 3. `auto_complete_keyword` 테이블 업데이트 (JPA 사용, 영속성 컨텍스트 거침)
        for (Map.Entry<String, Set<String>> entry : keywordUserMap.entrySet()) {
            String keyword = entry.getKey();
            int uniqueUserCount = entry.getValue().size(); // 중복 제거된 유저 수

            Optional<AutoCompleteKeyword> existingKeyword = autoCompleteKeywordRepository.findByKeyword(keyword);
            if (existingKeyword.isPresent()) {
                AutoCompleteKeyword autoCompleteKeyword = existingKeyword.get();
                autoCompleteKeyword.incrementFrequency(uniqueUserCount);
                autoCompleteKeywordRepository.save(autoCompleteKeyword);
            } else {
                AutoCompleteKeyword newKeyword = AutoCompleteKeyword.builder()
                    .keyword(keyword)
                    .frequency((long) uniqueUserCount)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .deleteAt(null)
                    .build();
                autoCompleteKeywordRepository.save(newKeyword);
            }
        }

        System.out.println("자동완성 배치 완료!");
    }
}
