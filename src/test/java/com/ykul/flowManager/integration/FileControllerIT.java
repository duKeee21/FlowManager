package com.ykul.flowManager.integration;

import com.ykul.flowManager.entity.FileRecord;
import com.ykul.flowManager.entity.FileStatus;
import com.ykul.flowManager.repository.FileRecordRepository;
import com.ykul.flowManager.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FileControllerIT extends AbstractIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileRecordRepository repository;

    @Autowired
    private MinioService minioService;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void uploadFile_returns202AndSavesToDb() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "MegaTest".getBytes()
        );

        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        assertThat(repository.findAll()).hasSize(1);
        FileRecord saved = repository.findAll().getFirst();
        assertThat(saved.getFileName()).isEqualTo("test.txt");
        assertThat(saved.getSourcePath()).contains("input/" + saved.getId());
    }

    @Test
    void getStatus_returnsStatusResponse() throws Exception {
        FileRecord record = new FileRecord();
        record.setFileName("test.txt");
        record.setStatus(FileStatus.SUCCESS);
        repository.save(record);

        mockMvc.perform(get("/api/files/{id}/status", record.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record.getId().toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getStatus_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/files/{id}/status", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadFile_success_returnsPdf() throws Exception {
        String convertedPath = "converted/" + UUID.randomUUID() + ".pdf";
        byte[] pdfBytes = "DUMMY PDF CONTENT".getBytes();
        MockMultipartFile pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", pdfBytes);
        minioService.uploadFile(convertedPath, pdfFile);

        FileRecord record = new FileRecord();
        record.setFileName("test.txt");
        record.setStatus(FileStatus.SUCCESS);
        record.setConvertedPath(convertedPath);
        repository.save(record);

        MvcResult result = mockMvc.perform(get("/api/files/{id}/download", record.getId()))
                .andExpect(status().isOk())
                .andExpect(result1 -> assertThat(result1.getResponse().getContentType()).isEqualTo("application/pdf"))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdfBytes);
    }

    @Test
    void downloadFile_notReady_returns409() throws Exception {
        FileRecord record = new FileRecord();
        record.setFileName("test.txt");
        record.setStatus(FileStatus.IN_PROGRESS);
        repository.save(record);

        mockMvc.perform(get("/api/files/{id}/download", record.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FILE_NOT_READY"));
    }
}