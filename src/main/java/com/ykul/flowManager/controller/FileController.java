package com.ykul.flowManager.controller;

import com.ykul.flowManager.dto.ConversionRequest;
import com.ykul.flowManager.dto.ErrorResponse;
import com.ykul.flowManager.dto.StatusResponse;
import com.ykul.flowManager.dto.UploadResponse;
import com.ykul.flowManager.entity.FileRecord;
import com.ykul.flowManager.entity.FileStatus;
import com.ykul.flowManager.repository.FileRecordRepository;
import com.ykul.flowManager.service.KafkaProducerService;
import com.ykul.flowManager.service.MinioService;
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

import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioService minioService;
    private final KafkaProducerService kafkaProducer;
    private final FileRecordRepository repository;

    @PostMapping
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        UUID id = UUID.randomUUID();
        String originalName = file.getOriginalFilename();
        String sourcePath = "input/" + id + "/" + originalName;

        minioService.uploadFile(sourcePath, file);

        FileRecord record = new FileRecord();
        record.setId(id);
        record.setFileName(originalName);
        record.setSourcePath(sourcePath);
        record.setStatus(FileStatus.IN_PROGRESS);

        repository.save(record);

        kafkaProducer.sendConversionRequest(new ConversionRequest(id.toString(), sourcePath));

        return ResponseEntity.accepted().body(new UploadResponse(
                id.toString(),
                "IN_PROGRESS",
                "Файл принят. Идет процесс конвертации."
        ));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable UUID id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok(new StatusResponse(
                        record.getId().toString(),
                        record.getStatus().name(),
                        record.getErrorMessage()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable UUID id) throws Exception {
        FileRecord record = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        if (record.getStatus() != FileStatus.SUCCESS || record.getConvertedPath() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("FILE_NOT_READY", "Файл еще не готов к скачиванию. Текущий статус: " + record.getStatus()));
        }

        InputStream stream = minioService.downloadFile(record.getConvertedPath());
        InputStreamResource resource = new InputStreamResource(stream);

        String baseName = record.getFileName().replaceAll("\\.[^.]+$", "");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}