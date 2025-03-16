//package com.sellect.server.search.batch;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//import javax.sql.DataSource;
//import lombok.AccessLevel;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.ChunkListener;
//import org.springframework.batch.core.ExitStatus;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.JobExecution;
//import org.springframework.batch.core.JobExecutionListener;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.StepExecution;
//import org.springframework.batch.core.StepExecutionListener;
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.batch.core.configuration.annotation.JobScope;
//import org.springframework.batch.core.configuration.annotation.StepScope;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.launch.support.RunIdIncrementer;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.scope.context.ChunkContext;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.batch.item.database.JdbcBatchItemWriter;
//import org.springframework.batch.item.database.JdbcPagingItemReader;
//import org.springframework.batch.item.database.Order;
//import org.springframework.batch.item.database.PagingQueryProvider;
//import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
//import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
//import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.core.BeanPropertyRowMapper;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.transaction.PlatformTransactionManager;
//
//@Configuration
//@EnableBatchProcessing
//@RequiredArgsConstructor
//@Slf4j
//public class AutoCompleteKeywordBatchV3 {
//
//    private static final int CHUNK_SIZE = 1000;
//
//    private final JobRepository jobRepository;
//    private final PlatformTransactionManager transactionManager;
//    private final DataSource dataSource;
//    private final JdbcTemplate jdbcTemplate;
//
//    @Bean
//    public Job processAutoCompleteKeywordJob(Step processAutoCompleteKeywordStep) {
//        return new JobBuilder("processAutoCompleteKeywordJob", jobRepository)
//            .incrementer(new RunIdIncrementer())
//            .start(processAutoCompleteKeywordStep)
//            .listener(new JobExecutionListener() {
//                @Override
//                public void beforeJob(JobExecution jobExecution) {
//                    log.info("Job [{}] started at {}", jobExecution.getJobInstance().getJobName(),
//                        jobExecution.getStartTime());
//                }
//
//                @Override
//                public void afterJob(JobExecution jobExecution) {
//                    log.info("Job [{}] completed with status {} at {}",
//                        jobExecution.getJobInstance().getJobName(), jobExecution.getStatus(),
//                        jobExecution.getEndTime());
//                }
//            })
//            .build();
//    }
//
//    @Bean
//    @JobScope
//    public Step processAutoCompleteKeywordStep() throws Exception {
//
//        // 마지막 처리된 PK 가져오기
//        Long lastProcessedId = jdbcTemplate.queryForObject(
//            "SELECT last_processed_id FROM batch_job_metadata WHERE job_name = ?",
//            Long.class, "processAutoCompleteKeywordJob"
//        );
//
//        // 테이블에서 최대 PK 가져오기
//        Long maxId = jdbcTemplate.queryForObject(
//            "SELECT id FROM search_log ORDER BY id DESC LIMIT 1",
//            Long.class
//        );
//
//        log.info("Processing range: lastProcessedId={} to maxId={}", lastProcessedId, maxId);
//
//        return new StepBuilder("processAutoCompleteKeywordStep", jobRepository)
//            .<SearchLog, AutoCompleteKeyword>chunk(CHUNK_SIZE, transactionManager)
//            .reader(searchLogItemReader(lastProcessedId, maxId))
//            .processor(autoCompleteKeywordProcessor())
//            .writer(autoCompleteKeywordWriter())
//            .listener(new StepExecutionListener() {
//                @Override
//                public void beforeStep(StepExecution stepExecution) {
//                    // StepExecutionContext 에 lastProcessedId와 maxId 저장
//                    stepExecution.getExecutionContext().putLong("lastProcessedId", lastProcessedId);
//                    stepExecution.getExecutionContext().putLong("maxId", maxId);
//                    log.info("Step [{}] started at {}", stepExecution.getStepName(),
//                        stepExecution.getStartTime());
//                }
//
//                @Override
//                public ExitStatus afterStep(StepExecution stepExecution) {
//                    if (stepExecution.getExitStatus() != ExitStatus.COMPLETED) {
//                        return stepExecution.getExitStatus();
//                    }
//
//                    // 마지막 PK를 batch_job_metadata 에 저장
//                    jdbcTemplate.update(
//                        "INSERT INTO batch_job_metadata (job_name, last_processed_id, last_execution_time) " +
//                            "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE last_processed_id = ?, last_execution_time = ?",
//                        "processAutoCompleteKeywordJob", maxId, LocalDateTime.now(), maxId, LocalDateTime.now()
//                    );
//                    log.info("Step [{}] completed with status {}, readCount: {}, writeCount: {}, lastProcessedId: {}",
//                        stepExecution.getStepName(), stepExecution.getStatus(),
//                        stepExecution.getReadCount(), stepExecution.getWriteCount(), maxId);
//                    return ExitStatus.COMPLETED;
//                }
//            })
//            .listener(new ChunkListener() {
//                @Override
//                public void beforeChunk(ChunkContext context) {
//                    log.debug("Chunk started, chunk size: {}",
//                        context.getStepContext().getStepExecution().getReadCount());
//                }
//
//                @Override
//                public void afterChunk(ChunkContext context) {
//                    log.debug("Chunk completed, items processed: {}, written: {}",
//                        context.getStepContext().getStepExecution().getReadCount(),
//                        context.getStepContext().getStepExecution().getWriteCount());
//                }
//
//                @Override
//                public void afterChunkError(ChunkContext context) {
//                    log.error("Chunk failed with error, readCount: {}",
//                        context.getStepContext().getStepExecution().getReadCount());
//                }
//            })
//            .build();
//    }
//
//    @Bean
//    @StepScope
//    public JdbcPagingItemReader<SearchLog> searchLogItemReader(
//        @Value("#{stepExecutionContext['lastProcessedId']}") Long lastProcessedId,
//        @Value("#{stepExecutionContext['maxId']}") Long maxId) throws Exception {
//
//        Map<String, Object> parameterValues = new HashMap<>();
//        parameterValues.put("startId", lastProcessedId + 1);
//        parameterValues.put("endId", maxId);
//
//        return new JdbcPagingItemReaderBuilder<SearchLog>()
//            .name("searchLogItemReader")
//            .dataSource(dataSource)
//            .queryProvider(createSearchLogQueryProvider())
//            .parameterValues(parameterValues)
//            .pageSize(CHUNK_SIZE)
//            .rowMapper(new BeanPropertyRowMapper<>(SearchLog.class))
//            .build();
//    }
//
//    private PagingQueryProvider createSearchLogQueryProvider() throws Exception {
//        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
//        queryProvider.setDataSource(dataSource);
//        queryProvider.setSelectClause("SELECT id, keyword, timestamp, filter_applied, result_count");
//        queryProvider.setFromClause("FROM search_log");
//        queryProvider.setWhereClause("WHERE id BETWEEN :startId AND :endId " +
//            "AND filter_applied = false " +
//            "AND result_count > 0");
//        queryProvider.setSortKeys(Map.of("id", Order.ASCENDING)); // PK로 정렬
//        return queryProvider.getObject();
//    }
//
//    @Bean
//    @StepScope
//    public ItemProcessor<SearchLog, AutoCompleteKeyword> autoCompleteKeywordProcessor() {
//        return item -> {
//            if (item.getKeyword() == null) {
//                log.error("Received null keyword: {}", item);
//                return null;
//            }
//
//            return AutoCompleteKeyword.builder()
//                .keyword(item.getKeyword())
//                .frequency(1L)
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//        };
//    }
//
//    @Bean
//    @StepScope
//    public JdbcBatchItemWriter<AutoCompleteKeyword> autoCompleteKeywordWriter() {
//        return new JdbcBatchItemWriterBuilder<AutoCompleteKeyword>()
//            .dataSource(dataSource)
//            .sql("INSERT INTO auto_complete_keyword (keyword, frequency, created_at, updated_at) " +
//                "VALUES (?, ?, ?, ?) " +
//                "ON DUPLICATE KEY UPDATE frequency = frequency + VALUES(frequency), updated_at = VALUES(updated_at)")
//            .itemPreparedStatementSetter((item, ps) -> {
//                if (item.getKeyword() == null) {
//                    log.error("Attempting to write null keyword: {}", item);
//                    throw new IllegalArgumentException("Keyword cannot be null");
//                }
//                ps.setString(1, item.getKeyword());
//                ps.setLong(2, item.getFrequency());
//                ps.setObject(3, item.getCreatedAt());
//                ps.setObject(4, item.getUpdatedAt());
//            })
//            .build();
//    }
//
//    @Getter
//    @Setter
//    @Builder
//    @NoArgsConstructor(access = AccessLevel.PRIVATE)
//    @AllArgsConstructor(access = AccessLevel.PRIVATE)
//    public static class SearchLog {
//        private Long id;
//        private String keyword;
//        private String userIdentifier;
//        private int resultCount;
//        private boolean filterApplied;
//        private LocalDateTime timestamp;
//        private Long categoryId;
//        private Long brandId;
//    }
//
//    @Getter
//    @Setter
//    @Builder
//    @NoArgsConstructor(access = AccessLevel.PRIVATE)
//    @AllArgsConstructor(access = AccessLevel.PRIVATE)
//    public static class AutoCompleteKeyword {
//        private Long id;
//        private String keyword;
//        private Long frequency;
//        private LocalDateTime createdAt;
//        private LocalDateTime updatedAt;
//        private LocalDateTime deleteAt;
//
//        public AutoCompleteKeyword incrementFrequency(long count) {
//            return AutoCompleteKeyword.builder()
//                .id(this.id)
//                .keyword(this.keyword)
//                .frequency(this.frequency += count)
//                .createdAt(this.createdAt)
//                .updatedAt(LocalDateTime.now())
//                .deleteAt(this.deleteAt)
//                .build();
//        }
//    }
//}