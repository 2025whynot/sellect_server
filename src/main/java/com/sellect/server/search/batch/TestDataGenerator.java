package com.sellect.server.search.batch;

import com.sellect.server.search.repository.SearchLogEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TestDataGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    private static final int TOTAL_RECORDS = 10_000_000; // 1,000만 개
    private static final int UNIQUE_KEYWORDS = 50_000; // 고유 키워드 5만 개 (중복 비율 증가)
    private static final double FILTER_APPLIED_RATE = 0.2; // 필터 적용 20%
    private static final double USER_RATE = 0.7; // 회원 비율 70%
    private static final double TOP_1_PERCENT_RATE = 0.01; // 상위 1%
    private static final double MID_10_PERCENT_RATE = 0.10; // 중간 10%
    private static final double TOP_1_PERCENT_PROBABILITY = 0.80; // 상위 1%가 80%
    private static final double MID_10_PERCENT_PROBABILITY = 0.15; // 중간 10%가 15%

    @Transactional
    public void generateSearchLogData() {
        // 1. 키워드 풀 생성
        List<String> keywordPool = generateKeywordPool(UNIQUE_KEYWORDS);

        // 2. 하루 시간 범위 설정 (2025-03-10 00:00:00 ~ 23:59:59)
        LocalDateTime startTime = LocalDateTime.of(2025, 3, 10, 0, 0, 0);
        long timeIncrement = 86400_000 / TOTAL_RECORDS; // 하루를 밀리초 단위로 나눔

        Random random = new Random();
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            // timestamp: 순차적으로 증가
            LocalDateTime timestamp = startTime.plus(i * timeIncrement, ChronoUnit.MILLIS);

            // keyword: 계층화된 Zipf 분포 적용
            String keyword = pickKeyword(keywordPool, random);

            // userIdentifier: USER_ 또는 GUEST_ 접두사
            String userIdentifier = generateUserIdentifier(random);

            // resultCount: 정규 분포 기반
            int resultCount = generateResultCount(random);

            // filterApplied: 20% 확률로 true
            boolean filterApplied = random.nextDouble() < FILTER_APPLIED_RATE;

            // categoryId, brandId: 임의 값
            Long categoryId = random.nextBoolean() ? random.nextLong(1, 100) : null;
            Long brandId = random.nextBoolean() ? random.nextLong(1, 50) : null;

            // SearchLogEntity 생성 및 저장
            SearchLogEntity log = SearchLogEntity.builder()
                .keyword(keyword)
                .userIdentifier(userIdentifier)
                .resultCount(resultCount)
                .filterApplied(filterApplied)
                .timestamp(timestamp)
                .categoryId(categoryId)
                .brandId(brandId)
                .build();

            entityManager.persist(log);

            // 배치 처리: 10,000건 마다 flush 및 clear
            if (i % 10_000 == 0) {
                entityManager.flush();
                entityManager.clear();
                System.out.println("Processed: " + i + " records");
            }
        }
        entityManager.flush();
        entityManager.clear();
    }

    private List<String> generateKeywordPool(int size) {
        List<String> keywords = new ArrayList<>(size);
        Random random = new Random();
        // 현실적인 인기 키워드 확장
        String[] popularPrefixes = {
            "iphone", "samsung", "nike", "adidas", "macbook", "sony", "lg", "hp",
            "dell", "jeans", "airpods", "galaxy", "puma", "gucci", "tv", "watch"
        };
        String[] midTierPrefixes = {
            "tablet", "headphones", "jacket", "sneakers", "bag", "camera", "monitor", "keyboard"
        };

        for (int i = 0; i < size; i++) {
            if (i < size * TOP_1_PERCENT_RATE) { // 상위 1% (인기 키워드)
                String prefix = popularPrefixes[random.nextInt(popularPrefixes.length)];
                keywords.add(prefix + "_" + random.nextInt(10)); // 예: "iphone_5"
            } else if (i < size * MID_10_PERCENT_RATE) { // 중간 10% (중간 인기 키워드)
                String prefix = midTierPrefixes[random.nextInt(midTierPrefixes.length)];
                keywords.add(prefix + "_" + random.nextInt(50)); // 예: "tablet_42"
            } else { // 나머지 89% (롱테일 키워드)
                keywords.add(generateRandomKeyword(random));
            }
        }
        return keywords;
    }

    private String generateRandomKeyword(Random random) {
        String[] prefixes = {"phone", "laptop", "book", "shoe", "shirt", "car", "tv", "watch"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        int suffix = random.nextInt(1000);
        return prefix + "_" + suffix; // 예: "phone_123"
    }

    private String pickKeyword(List<String> keywordPool, Random random) {
        int top1Percent = (int) (keywordPool.size() * TOP_1_PERCENT_RATE); // 상위 1%
        int mid10Percent = (int) (keywordPool.size() * MID_10_PERCENT_RATE); // 중간 10%
        double rand = random.nextDouble();

        if (rand < TOP_1_PERCENT_PROBABILITY) { // 상위 1% (80%)
            return keywordPool.get(random.nextInt(top1Percent));
        } else if (rand < TOP_1_PERCENT_PROBABILITY + MID_10_PERCENT_PROBABILITY) { // 중간 10% (15%)
            return keywordPool.get(random.nextInt(mid10Percent - top1Percent) + top1Percent);
        } else { // 나머지 89% (5%)
            return keywordPool.get(random.nextInt(keywordPool.size() - mid10Percent) + mid10Percent);
        }
    }

    private String generateUserIdentifier(Random random) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        return random.nextDouble() < USER_RATE ? "USER_" + sessionId : "GUEST_" + sessionId;
    }

    private int generateResultCount(Random random) {
        double rand = random.nextDouble();
        if (rand < 0.9) return ThreadLocalRandom.current().nextInt(1, 101); // 90%: 1~100
        else if (rand < 0.99) return ThreadLocalRandom.current().nextInt(101, 501); // 9%: 101~500
        else return ThreadLocalRandom.current().nextInt(501, 1001); // 1%: 501~1000
    }
}