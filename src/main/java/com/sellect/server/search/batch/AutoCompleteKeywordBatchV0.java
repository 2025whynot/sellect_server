//package com.sellect.server.search.batch;
//
//import com.sellect.server.search.repository.SearchLogEntity;
//import com.sellect.server.search.repository.jpa.AutoCompleteKeywordEntity;
//import com.sellect.server.search.repository.jpa.AutoCompleteKeywordJpaRepository;
//import jakarta.persistence.EntityManagerFactory;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
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
//import org.springframework.batch.item.database.JpaItemWriter;
//import org.springframework.batch.item.database.JpaPagingItemReader;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.transaction.PlatformTransactionManager;
//
//@Configuration
//@EnableBatchProcessing
//@RequiredArgsConstructor
//@Slf4j
//public class AutoCompleteKeywordBatchV0 {
//
//    private static final Integer CHUNK_SIZE = 1000;
//
//    @Bean
//    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
//        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
//        jobLauncher.setJobRepository(jobRepository);
//        jobLauncher.afterPropertiesSet();
//        log.info("JobLauncher initialized");
//        return jobLauncher;
//    }
//
//    @Bean
//    public Job processAutoCompleteKeywordJobV0(
//        JobRepository jobRepository,
//        Step processAutoCompleteKeywordStep) {
//
//        return new JobBuilder("processAutoCompleteKeywordJobV0", jobRepository)
//            .incrementer(new RunIdIncrementer())
//            .start(processAutoCompleteKeywordStep)
//            .listener(new JobExecutionListener() {
//                @Override
//                public void beforeJob(JobExecution jobExecution) {
//                    log.info("Job [{}] started at {}",
//                        jobExecution.getJobInstance().getJobName(),
//                        jobExecution.getStartTime());
//                }
//
//                @Override
//                public void afterJob(JobExecution jobExecution) {
//                    log.info("Job [{}] completed with status {} at {}",
//                        jobExecution.getJobInstance().getJobName(),
//                        jobExecution.getStatus(),
//                        jobExecution.getEndTime());
//                }
//            })
//            .build();
//    }
//
//    @Bean
//    @JobScope
//    public Step processAutoCompleteKeywordStep(
//        JobRepository jobRepository,
//        PlatformTransactionManager transactionManager,
//        JpaPagingItemReader<SearchLogEntity> searchLogItemReader,
//        ItemProcessor<SearchLogEntity, AutoCompleteKeywordEntity> autoCompleteKeywordProcessor,
//        JpaItemWriter<AutoCompleteKeywordEntity> autoCompleteKeywordWriter) {
//
//        return new StepBuilder("processAutoCompleteKeywordStep", jobRepository)
//            .<SearchLogEntity, AutoCompleteKeywordEntity>chunk(CHUNK_SIZE, transactionManager)
//            .reader(searchLogItemReader)
//            .processor(autoCompleteKeywordProcessor)
//            .writer(autoCompleteKeywordWriter)
//            .listener(new StepExecutionListener() {
//                @Override
//                public void beforeStep(StepExecution stepExecution) {
//                    log.info("Step [{}] started at {}",
//                        stepExecution.getStepName(),
//                        stepExecution.getStartTime());
//                }
//
//                @Override
//                public ExitStatus afterStep(StepExecution stepExecution) {
//                    log.info("Step [{}] completed with status {}, readCount: {}, writeCount: {}",
//                        stepExecution.getStepName(),
//                        stepExecution.getStatus(),
//                        stepExecution.getReadCount(),
//                        stepExecution.getWriteCount());
//                    return stepExecution.getExitStatus();
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
//    public JpaPagingItemReader<SearchLogEntity> searchLogItemReader(
//        EntityManagerFactory entityManagerFactory,
//        @Value("#{jobParameters['startDate']}") String startDateStr,
//        @Value("#{jobParameters['endDate']}") String endDateStr) {
//
//        LocalDateTime startDate = LocalDateTime.parse(startDateStr);
//        LocalDateTime endDate = LocalDateTime.parse(endDateStr);
//
//        JpaPagingItemReader<SearchLogEntity> reader = new JpaPagingItemReader<>();
//        reader.setEntityManagerFactory(entityManagerFactory);
//        reader.setQueryString("SELECT DISTINCT s FROM SearchLogEntity s "
//            + "WHERE s.timestamp BETWEEN :startDate AND :endDate " // 하룻동안의 검색어 로그만 처리
//            + "AND s.filterApplied = false " // 필터 적용된 검색어는 제외
//            + "AND s.resultCount > 0 "); // 검색 결과가 있는 검색어만 처리
//        reader.setParameterValues(Map.of("startDate", startDate, "endDate", endDate));
//        reader.setPageSize(CHUNK_SIZE);
//
//        log.info("Reader initialized with startDate [{}], endDate [{}]", startDateStr, endDateStr);
//
//        return reader;
//    }
//
//    @Bean
//    @StepScope
//    public ItemProcessor<SearchLogEntity, AutoCompleteKeywordEntity> autoCompleteKeywordProcessor(
//        AutoCompleteKeywordJpaRepository autoCompleteKeywordRepository) {
//        Map<String, AutoCompleteKeywordEntity> chunkCache = new HashMap<>();
//
//        return item -> {
//            String keyword = item.getKeyword();
//
//            // Chunk 내 캐시 확인
//            AutoCompleteKeywordEntity cached = chunkCache.get(keyword);
//            if (cached != null) {
//                cached.incrementFrequency();
//                return null; // Writer 로 전달하지 않음
//            }
//
//            // TODO: 병렬 실행 시 Writer 에서 데이저 저장할 때 동시성 문제는 발생하지 않는지 체크
//            // DB 조회
//            AutoCompleteKeywordEntity autoCompleteKeyword = autoCompleteKeywordRepository.findByKeyword(keyword)
//                .orElse(null);
//
//            if (autoCompleteKeyword == null) {
//                autoCompleteKeyword = AutoCompleteKeywordEntity.builder()
//                    .keyword(keyword)
//                    .frequency(1L)
//                    .createdAt(LocalDateTime.now())
//                    .updatedAt(LocalDateTime.now())
//                    .build();
//                chunkCache.put(keyword, autoCompleteKeyword);
//            } else {
//                autoCompleteKeyword.incrementFrequency();
//                chunkCache.put(keyword, autoCompleteKeyword);
//            }
//            return autoCompleteKeyword;
//        };
//    }
//
//    @Bean
//    @StepScope
//    public JpaItemWriter<AutoCompleteKeywordEntity> autoCompleteKeywordWriter(
//        EntityManagerFactory entityManagerFactory) {
//        JpaItemWriter<AutoCompleteKeywordEntity> writer = new JpaItemWriter<>();
//        writer.setEntityManagerFactory(entityManagerFactory);
//        log.info("Writer initialized");
//        return writer;
//    }
//}
