package com.ykul.flowManager.controller;

import com.ykul.flowManager.dto.ErrorResponse;
import com.ykul.flowManager.dto.StatusResponse;
import com.ykul.flowManager.dto.UploadResponse;
import com.ykul.flowManager.entity.FileRecord;
import com.ykul.flowManager.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {

        UUID id = fileService.uploadFile(file);

        return ResponseEntity.accepted().body(new UploadResponse(
                id.toString(),
                "IN_PROGRESS",
                "Файл принят."
        ));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable UUID id) {
        try {
            FileRecord record = fileService.getFileRecord(id);
            return ResponseEntity.ok(new StatusResponse(
                    record.getId().toString(),
                    record.getStatus().name(),
                    record.getErrorMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable UUID id) {
        try {
            FileRecord record = fileService.getFileRecord(id);

            InputStreamResource resource = fileService.downloadFile(record);

            String baseName = record.getFileName().replaceAll("\\.[^.]+$", "");
            String downloadName = baseName + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("FILE_NOT_READY", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("ERROR", "Ошибка при скачивании файла"));
        }
    }
}