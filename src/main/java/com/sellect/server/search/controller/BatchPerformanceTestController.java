package com.sellect.server.search.controller;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.search.controller.response.BatchPerformanceTestResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchPerformanceTestController {

    private final JobLauncher jobLauncher;
    private final Job processAutoCompleteKeywordJob;

    @GetMapping("/performance-test")
    public ApiResponse<BatchPerformanceTestResponse> performanceTest() {

        try {
            // 테스트 데이터 기준으로 설정
            LocalDateTime now = LocalDateTime.of(2025, 3, 11, 0, 0, 0);
            LocalDateTime startDate = now.minusDays(1); // 하루 전

            // ISO 8601 형식으로 포맷팅
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            String startDateStr = startDate.format(formatter);
            String endDateStr = now.format(formatter);

            // JobParameters 설정
            JobParametersBuilder jobParametersBuilder = new JobParametersBuilder()
                .addString("startDate", startDateStr) // 하루 전
                .addString("endDate", endDateStr);    // 현재 시각
            JobParameters jobParameters = jobParametersBuilder
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

            // 비동기 Job 실행
            runJobAsync(jobParameters);

            return ApiResponse.ok(BatchPerformanceTestResponse.builder()
                .message("Batch job triggered asynchronously. Check server logs for execution details.")
                .build());
        } catch (Exception e) {
            throw new CommonException(BError.FAIL_FOR_REASON, "batch performance test",
                e.getMessage());
        }
    }

    @Async
    public void runJobAsync(JobParameters jobParameters)
        throws Exception {

        // 실행 시간 측정 시작
        long startTime = System.currentTimeMillis();
        log.info("Starting batch job with parameters: startDate={}, endDate={}",
            jobParameters.getString("startDate"), jobParameters.getString("endDate"));

        // Job 실행
        JobExecution jobExecution = jobLauncher.run(processAutoCompleteKeywordJob, jobParameters);

        // 실행 시간 측정 종료
        long endTime = System.currentTimeMillis();
        long executionTimeMs = endTime - startTime;
        double executionTimeSeconds = executionTimeMs / 1000.0;

        // 로그에 실행 시간 기록
        log.info("Batch job completed. JobId={}, Status={}, ExecutionTime={} seconds",
            jobExecution.getJobId(), jobExecution.getStatus(), executionTimeSeconds);
    }

}
