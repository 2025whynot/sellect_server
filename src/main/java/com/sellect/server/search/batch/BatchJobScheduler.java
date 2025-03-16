//package com.sellect.server.search.batch;
//
//import java.time.LocalDateTime;
//import lombok.RequiredArgsConstructor;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.JobParameters;
//import org.springframework.batch.core.JobParametersBuilder;
//import org.springframework.batch.core.JobParametersInvalidException;
//import org.springframework.batch.core.launch.JobLauncher;
//import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
//import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
//import org.springframework.batch.core.repository.JobRestartException;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class BatchJobScheduler {
//
//    private final JobLauncher jobLauncher;
//
//    private final Job processAutoCompleteKeywordJob;
//
//    @Scheduled(cron = "0 0 1 * * ?") // 매일 새벽 1시 실행
//    public void runAutoCompleteKeywordJob()
//        throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
//        JobParameters jobParameters = new JobParametersBuilder()
//            .addString("startDate", LocalDateTime.now().minusDays(1).toString()) // 전날
//            .addString("endDate", LocalDateTime.now().toString()) // 오늘
//            .addLong("timestamp", System.currentTimeMillis()) // Job 인스턴스 고유성 보장
//            .toJobParameters();
//
//        jobLauncher.run(processAutoCompleteKeywordJob, jobParameters);
//    }
//}
