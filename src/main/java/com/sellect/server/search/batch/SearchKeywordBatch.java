//package com.sellect.server.search.batch;
//
//import com.sellect.server.search.domain.SearchKeyword;
//import com.sellect.server.search.repository.SearchKeywordRepository;
//import org.springframework.batch.core.*;
//import org.springframework.batch.core.configuration.annotation.*;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.launch.support.SimpleJobLauncher;
//import org.springframework.batch.core.step.tasklet.Tasklet;
//import org.springframework.batch.core.step.tasklet.TaskletStep;
//import org.springframework.batch.repeat.RepeatStatus;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.launch.JobLauncher;
//import org.springframework.batch.core.job.SimpleJob;
//
//import java.nio.file.*;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Configuration
//@EnableBatchProcessing
//public class SearchKeywordBatch {
//    private final JobRepository jobRepository;
//    private final SearchKeywordRepository repository;
//
//    public SearchKeywordBatch(JobRepository jobRepository, SearchKeywordRepository repository) {
//        this.jobRepository = jobRepository;
//        this.repository = repository;
//    }
//
//    @Bean
//    public JobLauncher jobLauncher() {
//        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
//        jobLauncher.setJobRepository(jobRepository);
//        return jobLauncher;
//    }
//
//    @Bean
//    public Job processSearchKeywordsJob() {
//        return new JobBuilder("searchKeywordJob", jobRepository)
//            .start(processSearchKeywordStep())
//            .build();
//    }
//
//    @Bean
//    public TaskletStep processSearchKeywordStep() {
//        return new TaskletStep("searchKeywordStep", jobRepository, searchKeywordTasklet());
//    }
//
//    @Bean
//    public Tasklet searchKeywordTasklet() {
//        return (contribution, chunkContext) -> {
//            Path logFilePath = Paths.get("logs/search_api.log");
//            Map<String, Long> keywordCount = new HashMap<>();
//
//            if (Files.exists(logFilePath)) {
//                List<String> lines = Files.readAllLines(logFilePath);
//                keywordCount = lines.stream()
//                    .map(this::extractKeyword)
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.groupingBy(k -> k, Collectors.counting()));
//            }
//
//            updateSearchKeywordTable(keywordCount);
//            Files.deleteIfExists(logFilePath);
//            return RepeatStatus.FINISHED;
//        };
//    }
//
//    private String extractKeyword(String logLine) {
//        if (logLine.contains("keyword=")) {
//            return logLine.split("keyword=")[1].split("&")[0];
//        }
//        return null;
//    }
//
//    @Transactional
//    public void updateSearchKeywordTable(Map<String, Long> keywordCount) {
//        keywordCount.forEach((keyword, count) -> {
//            repository.findByKeyword(keyword);
//            searchKeyword.incrementFrequency(count);
//            repository.save(searchKeyword);
//        });
//    }
//}
