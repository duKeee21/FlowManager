package com.ykul.flowManager.controller;

import com.ykul.flowManager.entity.FileRecord;
import com.ykul.flowManager.entity.FileStatus;
import com.ykul.flowManager.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @Test
    void uploadFile_returns202AndId() throws Exception {
        UUID id = UUID.randomUUID();
        when(fileService.uploadFile(any())).thenReturn(id);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Test".getBytes()
        );

        mockMvc.perform(multipart("/api/files").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void getStatus_returnsStatusResponse() throws Exception {
        UUID id = UUID.randomUUID();
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setStatus(FileStatus.SUCCESS);
        when(fileService.getFileRecord(id)).thenReturn(record);

        mockMvc.perform(get("/api/files/{id}/status", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getStatus_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(fileService.getFileRecord(id)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/files/{id}/status", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadFile_success_returnsPdf() throws Exception {
        UUID id = UUID.randomUUID();
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setFileName("test.txt");
        record.setStatus(FileStatus.SUCCESS);
        record.setConvertedPath("converted/abc.pdf");

        when(fileService.getFileRecord(id)).thenReturn(record);
        when(fileService.downloadFile(record)).thenReturn(
                new InputStreamResource(new ByteArrayInputStream("PDF".getBytes()))
        );

        mockMvc.perform(get("/api/files/{id}/download", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"test.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void downloadFile_notReady_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setStatus(FileStatus.IN_PROGRESS);
        when(fileService.getFileRecord(id)).thenReturn(record);
        when(fileService.downloadFile(record))
                .thenThrow(new IllegalStateException("Файл не готов"));

        mockMvc.perform(get("/api/files/{id}/download", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FILE_NOT_READY"));
    }
}