package com.sellect.server.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sellect.server.common.exception.StorageException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.product.properties.StorageProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

class FileSystemStorageServiceTest {

    private static Path tempDir;
    private FileSystemStorageService storageService;

    @BeforeAll
    static void setupTempDir() throws IOException {
        tempDir = Files.createTempDirectory("storage-test");
    }

    @BeforeEach
    void setUp() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setLocation(tempDir.toString());
        storageService = new FileSystemStorageService(properties);
        storageService.init();
    }

    @AfterEach
    void tearDown() {
        storageService.deleteAll();
    }

    @AfterAll
    static void cleanupTempDir() throws IOException {
        Files.deleteIfExists(tempDir);
    }

    @Nested
    @DisplayName("파일 저장(store) 테스트")
    class StoreTests {

        @Test
        @DisplayName("정상적인 파일을 저장하면 경로에 존재해야 한다.")
        void store_shouldSaveFileSuccessfully() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("test.txt");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test data".getBytes()));

            Path storedPath = storageService.store(file);

            assertThat(Files.exists(storedPath)).isTrue();
        }

        @Test
        @DisplayName("비어있는 파일을 저장하면 예외가 발생해야 한다.")
        void store_shouldThrowExceptionWhenFileIsEmpty() {
            MultipartFile emptyFile = mock(MultipartFile.class);
            when(emptyFile.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> storageService.store(emptyFile))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining(BError.NOT_EXIST.getMessage().replace("%1", "file"));
        }

        @Test
        @DisplayName("파일 이름이 없을 경우 예외가 발생해야 한다.")
        void store_shouldThrowExceptionWhenFilenameIsNull() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn(null);

            assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(StorageException.class);
        }
    }

    @Nested
    @DisplayName("파일 목록 조회(loadAll) 테스트")
    class LoadAllTests {

        @Test
        @DisplayName("저장된 파일 목록을 가져와야 한다.")
        void loadAll_shouldReturnAllStoredFiles() throws IOException {
            Files.createFile(tempDir.resolve("file1.txt"));
            Files.createFile(tempDir.resolve("file2.txt"));

            Stream<Path> filesStream = storageService.loadAll();
            List<Path> files = filesStream.collect(Collectors.toList());

            assertThat(files).hasSize(2);
            assertThat(files).containsExactlyInAnyOrder(Path.of("file1.txt"), Path.of("file2.txt"));
        }

        @Test
        @DisplayName("저장된 파일이 없으면 빈 목록을 반환해야 한다.")
        void loadAll_shouldReturnEmptyListWhenNoFiles() {
            Stream<Path> filesStream = storageService.loadAll();
            List<Path> files = filesStream.collect(Collectors.toList());

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("파일 경로 조회(load) 테스트")
    class LoadTests {

        @Test
        @DisplayName("파일 이름으로 해당 파일의 경로를 반환해야 한다.")
        void load_shouldReturnFilePath() {
            Path path = storageService.load("test.txt");

            assertThat(path).isEqualTo(tempDir.resolve("test.txt"));
        }

        @Test
        @DisplayName("존재하지 않는 파일을 로드하면 적절한 경로를 반환해야 한다.")
        void load_shouldReturnPathEvenIfFileNotExists() {
            Path path = storageService.load("nonexistent.txt");

            assertThat(path).isEqualTo(tempDir.resolve("nonexistent.txt"));
        }
    }

    @Nested
    @DisplayName("파일 리소스 조회(loadAsResource) 테스트")
    class LoadAsResourceTests {

        @Test
        @DisplayName("존재하는 파일을 리소스로 로드할 수 있어야 한다.")
        void loadAsResource_shouldReturnValidResource() throws IOException {
            Path testFile = tempDir.resolve("sample.txt");
            Files.createFile(testFile);

            Resource resource = storageService.loadAsResource("sample.txt");

            assertThat(resource.exists()).isTrue();
            assertThat(resource.isReadable()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 파일을 리소스로 로드하면 예외가 발생해야 한다.")
        void loadAsResource_shouldThrowExceptionWhenFileNotExists() {
            assertThatThrownBy(() -> storageService.loadAsResource("not_exist.txt"))
                .isInstanceOf(StorageException.class);
        }

        @Test
        @DisplayName("파일이 아닌 디렉토리를 리소스로 로드하면 예외가 발생해야 한다.")
        void loadAsResource_shouldThrowExceptionWhenPathIsDirectory() throws IOException {
            Path dir = tempDir.resolve("subdir");
            Files.createDirectory(dir);

            assertThatThrownBy(() -> storageService.loadAsResource("subdir"))
                .isInstanceOf(StorageException.class);
        }
    }

    @Nested
    @DisplayName("파일 삭제(deleteAll) 테스트")
    class DeleteAllTests {

        @Test
        @DisplayName("파일 삭제 후, 저장소가 비어 있어야 한다.")
        void deleteAll_shouldRemoveAllFiles() throws IOException {
            Files.createFile(tempDir.resolve("file1.txt"));
            Files.createFile(tempDir.resolve("file2.txt"));

            storageService.deleteAll();

            assertThat(Files.list(tempDir).count()).isZero();
        }

        @Test
        @DisplayName("이미 비어 있는 경우에도 정상 동작해야 한다.")
        void deleteAll_shouldNotFailWhenNoFiles() {
            assertThatCode(() -> storageService.deleteAll()).doesNotThrowAnyException();
        }
    }
}
