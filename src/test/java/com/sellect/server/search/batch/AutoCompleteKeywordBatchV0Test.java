package com.sellect.server.search.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.sellect.server.config.SecurityConfig;
import com.sellect.server.product.application.S3StorageClient;
import com.sellect.server.search.repository.SearchLogEntity;
import com.sellect.server.search.repository.SearchLogJpaRepository;
import com.sellect.server.search.repository.jpa.AutoCompleteKeywordEntity;
import com.sellect.server.search.repository.jpa.AutoCompleteKeywordJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@SpringBatchTest
@Sql(scripts = "classpath:db/init/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@ImportAutoConfiguration(
    exclude = {
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class,
        SecurityConfig.class})
class AutoCompleteKeywordBatchV0Test {

    @MockBean
    S3StorageClient s3StorageClient;

    @MockBean
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private AutoCompleteKeywordBatchV0 batchConfig;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AutoCompleteKeywordJpaRepository autoCompleteKeywordRepository;

    @Autowired
    private SearchLogJpaRepository searchLogJpaRepository;

    private static final String START_DATE = "2025-03-01T00:00:00";
    private static final String END_DATE = "2025-03-01T23:59:59";
    private static final String TEST_USER = "testUser";

    @BeforeEach
    void setUp() {
        autoCompleteKeywordRepository.deleteAll();
        searchLogJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        autoCompleteKeywordRepository.deleteAll();
        searchLogJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("ItemReader가 지정된 날짜 범위 내 검색 로그를 올바르게 읽는지 확인")
    public void testSearchLogItemReader() throws Exception {
        // Given: 테스트 데이터 삽입
        SearchLogEntity log1 = SearchLogEntity.builder()
            .keyword("testKeyword1")
            .userIdentifier(TEST_USER)
            .resultCount(1)
            .filterApplied(false)
            .timestamp(LocalDateTime.parse(START_DATE))
            .build();
        SearchLogEntity log2 = SearchLogEntity.builder()
            .keyword("testKeyword2")
            .userIdentifier(TEST_USER)
            .resultCount(1)
            .filterApplied(false)
            .timestamp(LocalDateTime.parse(START_DATE))
            .build();
        searchLogJpaRepository.save(log1);
        searchLogJpaRepository.save(log2);

        JobParameters jobParameters = new JobParametersBuilder()
            .addString("startDate", START_DATE)
            .addString("endDate", END_DATE)
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        StepScopeTestUtils.doInStepScope(new StepExecution("searchLogStep", jobExecution), () -> {
            // When: Reader 실행
            JpaPagingItemReader<SearchLogEntity> reader = batchConfig.searchLogItemReader(
                entityManager.getEntityManagerFactory(),
                START_DATE,
                END_DATE);

            reader.open(new ExecutionContext());

            SearchLogEntity item1 = reader.read();
            SearchLogEntity item2 = reader.read();
            SearchLogEntity item3 = reader.read();

            // Then: 결과 검증
            assertThat(item1).isNotNull()
                .extracting(SearchLogEntity::getKeyword)
                .isEqualTo("testKeyword1");
            assertThat(item2).isNotNull()
                .extracting(SearchLogEntity::getKeyword)
                .isEqualTo("testKeyword2");
            assertThat(item3).isNull();

            reader.close();
            return null;
        });
    }

    @Test
    @DisplayName("ItemProcessor 가 새로운 키워드 생성 및 기존 키워드 업데이트를 올바르게 처리하는지 확인")
    @Transactional
    void testAutoCompleteKeywordProcessor() throws Exception {
        SearchLogEntity log = SearchLogEntity.builder()
            .keyword("testKeyword")
            .userIdentifier(TEST_USER)
            .resultCount(1)
            .filterApplied(false)
            .timestamp(LocalDateTime.now())
            .build();
        ItemProcessor<SearchLogEntity, AutoCompleteKeywordEntity> processor = batchConfig
            .autoCompleteKeywordProcessor(autoCompleteKeywordRepository);

        AutoCompleteKeywordEntity result1 = processor.process(log);

        assertThat(result1)
            .isNotNull()
            .extracting(AutoCompleteKeywordEntity::getKeyword, AutoCompleteKeywordEntity::getFrequency)
            .containsExactly("testKeyword", 1L);

        AutoCompleteKeywordEntity existing = AutoCompleteKeywordEntity.builder()
            .keyword("testKeyword")
            .frequency(1L)
            .build();
        entityManager.persist(existing);
        entityManager.flush();

        AutoCompleteKeywordEntity result2 = processor.process(log);

        assertThat(result2)
            .isNotNull()
            .extracting(AutoCompleteKeywordEntity::getKeyword, AutoCompleteKeywordEntity::getFrequency)
            .containsExactly("testKeyword", 2L);
    }

    @Test
    @DisplayName("ItemWriter가 AutoCompleteKeywordEntity를 데이터베이스에 올바르게 저장하는지 확인")
    @Transactional
    void testAutoCompleteKeywordWriter() throws Exception {
        // Given: Writer 준비 및 테스트 데이터
        AutoCompleteKeywordEntity entity = AutoCompleteKeywordEntity.builder()
            .keyword("testKeyword")
            .frequency(1L)
            .build();
        JpaItemWriter<AutoCompleteKeywordEntity> writer = batchConfig
            .autoCompleteKeywordWriter(entityManager.getEntityManagerFactory());

        // When: Writer 실행
        Chunk<AutoCompleteKeywordEntity> chunk = new Chunk<>();
        chunk.add(entity);
        writer.write(chunk);
        entityManager.flush();

        // Then: 데이터베이스 저장 검증
        AutoCompleteKeywordEntity saved = autoCompleteKeywordRepository.findByKeyword("testKeyword")
            .orElse(null);
        assertThat(saved)
            .isNotNull()
            .extracting(AutoCompleteKeywordEntity::getKeyword, AutoCompleteKeywordEntity::getFrequency)
            .containsExactly("testKeyword", 1L);
    }

    @Test
    @DisplayName("전체 Job이 성공적으로 실행되며 검색 로그를 기반으로 자동완성 키워드를 생성하는지 확인")
    void testFullJobExecution() throws Exception {
        SearchLogEntity log1 = SearchLogEntity.builder()
            .keyword("testKeyword1")
            .userIdentifier(TEST_USER)
            .resultCount(1)
            .filterApplied(false)
            .timestamp(LocalDateTime.parse(START_DATE))
            .build();
        SearchLogEntity log2 = SearchLogEntity.builder()
            .keyword("testKeyword2")
            .userIdentifier(TEST_USER)
            .resultCount(1)
            .filterApplied(false)
            .timestamp(LocalDateTime.parse(START_DATE))
            .build();
        searchLogJpaRepository.save(log1);
        searchLogJpaRepository.save(log2);

        JobParameters jobParameters = new JobParametersBuilder()
            .addString("startDate", START_DATE)
            .addString("endDate", END_DATE)
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<AutoCompleteKeywordEntity> results = autoCompleteKeywordRepository.findAll();
        assertThat(results)
            .hasSize(2)
            .extracting(AutoCompleteKeywordEntity::getKeyword, AutoCompleteKeywordEntity::getFrequency)
            .containsExactlyInAnyOrder(
                tuple("testKeyword1", 1L),
                tuple("testKeyword2", 1L)
            );
    }
}