package com.sellect.server.product.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.AmazonS3;
import com.sellect.server.common.exception.StorageException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.product.config.properties.S3StorageProperties;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class S3StorageServiceTest {

    // TODO: Fake S3 서버를 사용하여 테스트를 진행해야 함
    private AmazonS3 s3Client;
    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(AmazonS3.class);
        S3StorageProperties properties = new S3StorageProperties();
        String bucketName = "test-bucket";
        properties.setBucketName(bucketName);
        s3StorageService = new S3StorageService(s3Client, properties);
    }

    @Nested
    class StoreAndReturnNewFilenameTests {
        @Test
        @DisplayName("파일을 정상적으로 저장")
        void shouldStoreFileSuccessfully() throws IOException {
        }

        @Test
        @DisplayName("파일의 name 값이 공백이면 예외가 발생해야 한다")
        void shouldThrowExceptionWhenFilenameIsNull() {
            MockMultipartFile file = new MockMultipartFile("blank", "test.txt",
                "text/plain", "Hello World".getBytes());

            assertThatThrownBy(() -> s3StorageService.store(file, ""))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining(BError.NOT_EXIST.getMessage()
                    .replace("%1", "file name"));
        }

        @Test
        @DisplayName("IOException 발생 시 예외가 발생해야 한다")
        void shouldThrowExceptionOnIOException() throws IOException {
            MockMultipartFile file = mock(MockMultipartFile.class);
            when(file.getInputStream()).thenThrow(new IOException("Test IOException"));

            assertThatThrownBy(() -> s3StorageService.store(file, file.getName()))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining(BError.FAIL_FOR_REASON.getMessage()
                    .replace("%1", "file")
                    .replace("%2", "Test IOException"));
        }
    }

    @Nested
    class LoadAsPathTests {
        @Test
        @DisplayName("올바른 S3 URL을 반환해야 한다")
        void shouldReturnCorrectS3Url() throws MalformedURLException {
        }
    }

    @Nested
    class LoadAsResourceTests {
        @Test
        @DisplayName("유효한 리소스를 반환해야 한다")
        void shouldReturnValidResource() throws URISyntaxException, MalformedURLException {
        }

        @Test
        @DisplayName("파일이 존재하지 않으면 예외가 발생해야 한다")
        void shouldThrowExceptionWhenFileNotFound() {
        }
    }
}
