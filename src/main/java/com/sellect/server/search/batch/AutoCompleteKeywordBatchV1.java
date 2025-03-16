//package com.sellect.server.search.batch;
//
//import com.sellect.server.search.repository.SearchLogEntity;
//import com.sellect.server.search.repository.jpa.AutoCompleteKeywordEntity;
//import com.sellect.server.search.repository.jpa.AutoCompleteKeywordJpaRepository;
//import jakarta.persistence.EntityManagerFactory;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//import javax.sql.DataSource;
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
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.scope.context.ChunkContext;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.batch.item.database.JdbcBatchItemWriter;
//import org.springframework.batch.item.database.JpaPagingItemReader;
//import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.transaction.PlatformTransactionManager;
//
//@Configuration
//@EnableBatchProcessing
//@RequiredArgsConstructor
//@Slf4j
//public class AutoCompleteKeywordBatchV1 {
//
//    private static final int CHUNK_SIZE = 5000;
//
//    private final JobRepository jobRepository;
//    private final PlatformTransactionManager transactionManager;
//    private final EntityManagerFactory entityManagerFactory;
//    private final DataSource dataSource;
//    private final AutoCompleteKeywordJpaRepository autoCompleteKeywordRepository;
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
//        JpaPagingItemReader<SearchLogEntity> searchLogItemReader,
//        ItemProcessor<SearchLogEntity, AutoCompleteKeywordEntity> autoCompleteKeywordProcessor,
//        JdbcBatchItemWriter<AutoCompleteKeywordEntity> autoCompleteKeywordWriter) {
//        return new StepBuilder("processAutoCompleteKeywordStep", jobRepository)
//            .<SearchLogEntity, AutoCompleteKeywordEntity>chunk(CHUNK_SIZE, transactionManager)
//            .reader(searchLogItemReader)
//            .processor(autoCompleteKeywordProcessor)
//            .writer(autoCompleteKeywordWriter)
//            .listener(new StepExecutionListener() {
//                @Override
//                public void beforeStep(StepExecution stepExecution) {
//                    log.info("Step [{}] started at {}", stepExecution.getStepName(),
//                        stepExecution.getStartTime());
//                }
//
//                @Override
//                public ExitStatus afterStep(StepExecution stepExecution) {
//                    log.info("Step [{}] completed with status {}, readCount: {}, writeCount: {}",
//                        stepExecution.getStepName(), stepExecution.getStatus(),
//                        stepExecution.getReadCount(), stepExecution.getWriteCount());
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
//                    log.debug("Chunk completed, items processed: {}, written: {}",s
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
//    public JpaPagingItemReader<SearchLogEntity> searchLogItemReader(
//        @Value("#{jobParameters['startDate']}") String startDateStr,
//        @Value("#{jobParameters['endDate']}") String endDateStr) {
//
//        LocalDateTime startDate = LocalDateTime.parse(startDateStr);
//        LocalDateTime endDate = LocalDateTime.parse(endDateStr);
//
//        JpaPagingItemReader<SearchLogEntity> reader = new JpaPagingItemReader<>();
//        reader.setEntityManagerFactory(entityManagerFactory);
//        reader.setQueryString("SELECT DISTINCT s FROM SearchLogEntity s "
//            + "WHERE s.timestamp BETWEEN :startDate AND :endDate "
//            + "AND s.filterApplied = false "
//            + "AND s.resultCount > 0 ");
//        reader.setParameterValues(Map.of("startDate", startDate, "endDate", endDate));
//        reader.setPageSize(CHUNK_SIZE);
//
//        log.info("Reader initialized with startDate=[{}], endDate=[{}]", startDateStr, endDateStr);
//        return reader;
//    }
//
//    @Bean
//    @StepScope
//    public ItemProcessor<SearchLogEntity, AutoCompleteKeywordEntity> autoCompleteKeywordProcessor() {
//        Map<String, AutoCompleteKeywordEntity> chunkCache = new HashMap<>();
//
//        return item -> {
//            String keyword = item.getKeyword();
//            AutoCompleteKeywordEntity cached = chunkCache.get(keyword);
//            if (cached != null) {
//                cached.incrementFrequency();
//                return null;
//            }
//
//            AutoCompleteKeywordEntity entity = autoCompleteKeywordRepository.findByKeyword(keyword)
//                .orElse(null);
//            if (entity == null) {
//                entity = AutoCompleteKeywordEntity.builder()
//                    .keyword(keyword)
//                    .frequency(1L)
//                    .createdAt(LocalDateTime.now())
//                    .updatedAt(LocalDateTime.now())
//                    .build();
//            } else {
//                entity.incrementFrequency();
//            }
//            chunkCache.put(keyword, entity);
//            return entity;
//        };
//    }
//
//    @Bean
//    @StepScope
//    public JdbcBatchItemWriter<AutoCompleteKeywordEntity> autoCompleteKeywordWriter() {
//        return new JdbcBatchItemWriterBuilder<AutoCompleteKeywordEntity>()
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
//}