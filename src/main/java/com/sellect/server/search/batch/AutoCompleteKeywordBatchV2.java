//package com.sellect.server.search.batch;
//
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.HashMap;
//import java.util.Map;
//import javax.sql.DataSource;
//import lombok.AccessLevel;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.RequiredArgsConstructor;
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
//import org.springframework.batch.core.launch.JobLauncher;
//import org.springframework.batch.core.launch.support.RunIdIncrementer;
//import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
//import org.springframework.batch.core.partition.support.Partitioner;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.scope.context.ChunkContext;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.item.ExecutionContext;
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
//import org.springframework.core.task.SimpleAsyncTaskExecutor;
//import org.springframework.core.task.TaskExecutor;
//import org.springframework.jdbc.core.BeanPropertyRowMapper;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.transaction.PlatformTransactionManager;
//
//@Configuration
//@EnableBatchProcessing
//@RequiredArgsConstructor
//@Slf4j
//public class AutoCompleteKeywordBatchV2 {
//
//    private static final int CHUNK_SIZE = 100;
//    private static final int THREAD_POOL_SIZE = 5;
//
//    private final JobRepository jobRepository;
//    private final PlatformTransactionManager transactionManager;
//    private final DataSource dataSource;
//    private final JdbcTemplate jdbcTemplate;
//
//    @Bean
//    public JobLauncher jobLauncher() throws Exception {
//        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
//        jobLauncher.setJobRepository(jobRepository);
//        jobLauncher.afterPropertiesSet();
//        log.info("JobLauncher initialized");
//        return jobLauncher;
//    }
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
//    public Step processAutoCompleteKeywordStep(
//        @Value("#{jobParameters['startDate']}") String startDateStr,
//        @Value("#{jobParameters['endDate']}") String endDateStr) throws Exception {
//        return new StepBuilder("processAutoCompleteKeywordStep", jobRepository)
//            .partitioner("partitionedStep", searchLogPartitioner(startDateStr, endDateStr))
//            .step(slaveStep())
//            .gridSize(THREAD_POOL_SIZE)
//            .taskExecutor(taskExecutor())
//            .build();
//    }
//
//    @Bean
//    public TaskExecutor taskExecutor() {
//        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
//        executor.setConcurrencyLimit(THREAD_POOL_SIZE);
//        executor.setThreadNamePrefix("Batch-Thread-");
//        return executor;
//    }
//
//    @Bean
//    @JobScope
//    public Partitioner searchLogPartitioner(
//        @Value("#{jobParameters['startDate']}") String startDateStr,
//        @Value("#{jobParameters['endDate']}") String endDateStr) {
//        return gridSize -> {
//            LocalDateTime startDate = LocalDateTime.parse(startDateStr);
//            LocalDateTime endDate = LocalDateTime.parse(endDateStr);
//            long totalSeconds = ChronoUnit.SECONDS.between(startDate, endDate);
//            long partitionSize = totalSeconds / gridSize;
//
//            Map<String, ExecutionContext> partitions = new HashMap<>();
//            for (int i = 0; i < gridSize; i++) {
//                LocalDateTime partitionStart = startDate.plusSeconds(i * partitionSize);
//                LocalDateTime partitionEnd = (i == gridSize - 1) ? endDate : partitionStart.plusSeconds(partitionSize);
//
//                ExecutionContext context = new ExecutionContext();
//                context.putString("partitionStart", partitionStart.toString());
//                context.putString("partitionEnd", partitionEnd.toString());
//                partitions.put("partition" + i, context);
//            }
//            log.info("Partitioner created with {} partitions, startDate=[{}], endDate=[{}]",
//                gridSize, startDateStr, endDateStr);
//            return partitions;
//        };
//    }
//
//    @Bean
//    public Step slaveStep() throws Exception {
//        return new StepBuilder("slaveStep", jobRepository)
//            .<SearchLog, AutoCompleteKeyword>chunk(CHUNK_SIZE, transactionManager)
//            .reader(searchLogItemReader(null, null))
//            .processor(autoCompleteKeywordProcessor())
//            .writer(autoCompleteKeywordWriter())
//            .listener(new StepExecutionListener() {
//                @Override
//                public void beforeStep(StepExecution stepExecution) {
//                    log.info("Slave Step [{}] started at {}", stepExecution.getStepName(),
//                        stepExecution.getStartTime());
//                }
//
//                @Override
//                public ExitStatus afterStep(StepExecution stepExecution) {
//                    log.info("Slave Step [{}] completed with status {}, readCount: {}, writeCount: {}",
//                        stepExecution.getStepName(), stepExecution.getStatus(),
//                        stepExecution.getReadCount(), stepExecution.getWriteCount());
//                    return ExitStatus.COMPLETED;
//                }
//            })
//            .listener(new ChunkListener() {
//                @Override
//                public void beforeChunk(ChunkContext context) {
//                    log.debug("Chunk started in step [{}], partition={}, readCount={}",
//                        context.getStepContext().getStepName(),
//                        context.getStepContext().getStepExecution().getExecutionContext().getString("partitionStart"),
//                        context.getStepContext().getStepExecution().getReadCount());
//                }
//
//                @Override
//                public void afterChunk(ChunkContext context) {
//                    log.debug("Chunk completed in step [{}], partition={}, readCount={}, writeCount={}",
//                        context.getStepContext().getStepName(),
//                        context.getStepContext().getStepExecution().getExecutionContext().getString("partitionStart"),
//                        context.getStepContext().getStepExecution().getReadCount(),
//                        context.getStepContext().getStepExecution().getWriteCount());
//                }
//
//                @Override
//                public void afterChunkError(ChunkContext context) {
//                    log.error("Chunk failed in step [{}], partition={}, readCount={}",
//                        context.getStepContext().getStepName(),
//                        context.getStepContext().getStepExecution().getExecutionContext().getString("partitionStart"),
//                        context.getStepContext().getStepExecution().getReadCount());
//                }
//            })
//            .build();
//    }
//
//    @Bean
//    @StepScope
//    public JdbcPagingItemReader<SearchLog> searchLogItemReader(
//        @Value("#{stepExecutionContext['partitionStart']}") String partitionStartStr,
//        @Value("#{stepExecutionContext['partitionEnd']}") String partitionEndStr) throws Exception {
//
//        LocalDateTime partitionStart = LocalDateTime.parse(partitionStartStr);
//        LocalDateTime partitionEnd = LocalDateTime.parse(partitionEndStr);
//
//        Map<String, Object> parameterValues = new HashMap<>();
//        parameterValues.put("startDate", partitionStart);
//        parameterValues.put("endDate", partitionEnd);
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
//        queryProvider.setSelectClause("SELECT DISTINCT keyword, timestamp");
//        queryProvider.setFromClause("FROM search_log");
//        queryProvider.setWhereClause("WHERE timestamp BETWEEN :startDate AND :endDate " +
//            "AND filter_applied = false " +
//            "AND result_count > 0");
//        queryProvider.setSortKeys(Map.of("timestamp", Order.ASCENDING)); // 정렬 키 추가
//        return queryProvider.getObject();
//    }
//
//    @Bean
//    @StepScope
//    public ItemProcessor<SearchLog, AutoCompleteKeyword> autoCompleteKeywordProcessor() {
//        Map<String, AutoCompleteKeyword> chunkCache = new HashMap<>();
//
//        return item -> {
//            String keyword = item.getKeyword();
//            AutoCompleteKeyword cached = chunkCache.get(keyword);
//            if (cached != null) {
//                cached.incrementFrequency(1);
//                return null;
//            }
//
//            // JdbcTemplate 으로 키워드 조회
//            String sql = "SELECT keyword, frequency, created_at, updated_at " +
//                "FROM auto_complete_keyword WHERE keyword = ?";
//            AutoCompleteKeyword autoCompleteKeyword = jdbcTemplate.query(sql, rs -> {
//                if (rs.next()) {
//                    return AutoCompleteKeyword.builder()
//                        .id(rs.getLong("id"))
//                        .keyword(rs.getString("keyword"))
//                        .frequency(rs.getLong("frequency"))
//                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
//                        .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
//                        .build();
//                }
//                return null;
//            }, keyword);
//
//            if (autoCompleteKeyword == null) {
//                AutoCompleteKeyword.builder()
//                    .keyword(keyword)
//                    .frequency(1L)
//                    .createdAt(LocalDateTime.now())
//                    .updatedAt(LocalDateTime.now())
//                    .build();
//            } else {
//                autoCompleteKeyword.incrementFrequency(1);
//            }
//            chunkCache.put(keyword, autoCompleteKeyword);
//            return autoCompleteKeyword;
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
//                ps.setString(1, item.getKeyword());
//                ps.setLong(2, item.getFrequency());
//                ps.setObject(3, item.getCreatedAt());
//                ps.setObject(4, item.getUpdatedAt());
//            })
//            .build();
//    }
//
//    @Getter
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