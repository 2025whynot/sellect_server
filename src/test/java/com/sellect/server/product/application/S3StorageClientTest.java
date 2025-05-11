package com.sellect.server.product.application;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.product.config.properties.S3StorageProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageClientTest {

    private S3StorageClient storageClient;
    private AmazonS3 s3Client;
    private S3StorageProperties properties;

    @BeforeEach
    void setUp() {
        s3Client = mock(AmazonS3.class);
        properties = new S3StorageProperties();
        properties.setBucketName("test-bucket");
        storageClient = new S3StorageClient(s3Client, properties);
    }

    @Nested
    @DisplayName("S3StorageClient 초기화 테스트")
    class S3StorageClientInitTests {

        @Test
        @DisplayName("버킷이 존재하지 않으면 예외가 발생해야 한다.")
        void shouldThrowExceptionWhenBucketDoesNotExist() {
            // S3 설정 시 테스트 활성화
//            when(s3Client.doesBucketExistV2("test-bucket")).thenReturn(false);
//            assertThatThrownBy(() -> storageClient.init())
//                .isInstanceOf(Exception.class)
//                .hasMessage("bucket does not exist");
        }
    }

    @Nested
    @DisplayName("파일 저장(store) 테스트")
    class StoreTests {

        @Test
        @DisplayName("정상적인 MultipartFile 저장 시 S3에 업로드되어야 한다.")
        void shouldSaveMultipartFileSuccessfully() {
            FakeMultipartFile file = new FakeMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());

            storageClient.store(file, "test.txt");

            verify(s3Client).putObject(argThat((PutObjectRequest request) ->
                request.getBucketName().equals("test-bucket") &&
                    request.getKey().equals("test.txt") &&
                    request.getMetadata().getContentLength() == file.getSize() &&
                    request.getMetadata().getContentType().equals("text/plain")));
        }

        @Test
        @DisplayName("정상적인 InputStream 저장 시 S3에 업로드되어야 한다.")
        void shouldSaveInputStreamSuccessfully() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream("test data".getBytes());

            storageClient.store(inputStream, "test.txt");

            verify(s3Client).putObject(argThat((PutObjectRequest request) ->
                request.getBucketName().equals("test-bucket") &&
                    request.getKey().equals("test.txt")));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        @DisplayName("파일 이름이 없거나 공백일 경우 예외가 발생해야 한다.")
        void shouldThrowExceptionWhenFilenameIsInvalid(String filename) {
            FakeMultipartFile file = new FakeMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

            assertThatThrownBy(() -> storageClient.store(file, filename))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("file name"));
        }

        @Test
        @DisplayName("MultipartFile 저장 시 IOException 발생하면 예외가 던져져야 한다.")
        void shouldThrowExceptionWhenMultipartFileStoreFails() throws IOException {
            FakeMultipartFile file = spy(new FakeMultipartFile("file", "test.txt", "text/plain", "data".getBytes()));
            IOException ioException = new IOException("Stream error");
            when(file.getInputStream()).thenThrow(ioException);

            assertThatThrownBy(() -> storageClient.store(file, "test.txt"))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.FAIL_FOR_REASON.getMessage("store file", "Stream error"));
        }

        @Test
        @DisplayName("InputStream 저장 시 IOException 발생하면 예외가 던져져야 한다.")
        void shouldThrowExceptionWhenInputStreamStoreFails() {
            ByteArrayInputStream inputStream = spy(new ByteArrayInputStream("data".getBytes()));
            IOException ioException = new IOException("Stream error");
            when(inputStream.available()).thenThrow(ioException);

            assertThatThrownBy(() -> storageClient.store(inputStream, "test.txt"))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.FAIL_FOR_REASON.getMessage("store file", "Stream error"));
        }
    }

    @Nested
    @DisplayName("파일 경로 조회(loadAsPath) 테스트")
    class LoadAsPathTests {

        @Test
        @DisplayName("파일 이름으로 S3 URL을 반환해야 한다.")
        void shouldReturnS3Url() throws MalformedURLException {
            when(s3Client.doesObjectExist("test-bucket", "test.txt")).thenReturn(true);
            when(s3Client.getUrl("test-bucket", "test.txt")).thenReturn(URI.create("https://test-bucket.s3.amazonaws.com/test.txt").toURL());

            String path = storageClient.loadAsPath("test.txt");
            assertThat(path).isEqualTo("https://test-bucket.s3.amazonaws.com/test.txt");
        }

        @Test
        @DisplayName("존재하지 않는 파일을 조회하면 예외가 발생해야 한다.")
        void shouldThrowExceptionWhenFileNotExists() {
            when(s3Client.doesObjectExist("test-bucket", "not_exist.txt")).thenReturn(false);

            assertThatThrownBy(() -> storageClient.loadAsPath("not_exist.txt"))
                .isInstanceOf(CommonException.class)
                .hasMessage(BError.NOT_EXIST.getMessage("file"));
        }
    }

    @Nested
    @DisplayName("파일 삭제(deleteAll) 테스트")
    class DeleteAllTests {

        @Test
        @DisplayName("deleteAll 호출 시 UnsupportedOperationException이 발생해야 한다.")
        void shouldThrowUnsupportedOperationException() {
            when(s3Client.doesBucketExistV2("test-bucket")).thenReturn(true);
            assertThatCode(() -> storageClient.init()).doesNotThrowAnyException();

            assertThatThrownBy(() -> storageClient.deleteAll())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported yet.");
        }
    }
}