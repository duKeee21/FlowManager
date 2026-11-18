package com.ykul.flowManager.service;

import com.ykul.flowManager.entity.FileRecord;
import com.ykul.flowManager.entity.FileStatus;
import com.ykul.flowManager.repository.FileRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private MinioService minioService;
    @Mock
    private FileRecordRepository repository;

    @InjectMocks
    private FileService fileService;

    @Test
    void getFileRecord_found() {
        UUID id = UUID.randomUUID();
        FileRecord record = new FileRecord();
        record.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(record));

        FileRecord result = fileService.getFileRecord(id);

        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getFileRecord_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getFileRecord(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void downloadFile_success() throws Exception {
        FileRecord record = new FileRecord();
        record.setId(UUID.randomUUID());
        record.setStatus(FileStatus.SUCCESS);
        record.setConvertedPath("converted/test.pdf");

        ByteArrayInputStream stream = new ByteArrayInputStream("PDF".getBytes());
        when(minioService.downloadFile("converted/test.pdf")).thenReturn(stream);

        InputStreamResource resource = fileService.downloadFile(record);

        assertThat(resource).isNotNull();
        assertThat(resource.getInputStream()).isEqualTo(stream);
    }

    @Test
    void downloadFile_notReady_throws() {
        FileRecord record = new FileRecord();
        record.setStatus(FileStatus.IN_PROGRESS);

        assertThatThrownBy(() -> fileService.downloadFile(record))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Файл еще не готов.");
    }

    @Test
    void downloadFile_error_throws() {
        FileRecord record = new FileRecord();
        record.setStatus(FileStatus.ERROR);
        record.setConvertedPath(null);

        assertThatThrownBy(() -> fileService.downloadFile(record))
                .isInstanceOf(IllegalStateException.class);
    }
}