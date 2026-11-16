package com.ykul.flowManager.service;

import com.ykul.flowManager.dto.ConversionRequest;
import com.ykul.flowManager.entity.FileRecord;
import com.ykul.flowManager.entity.FileStatus;
import com.ykul.flowManager.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioService minioService;
    private final KafkaProducerService kafkaProducer;
    private final FileRecordRepository repository;

    public UUID uploadFile(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename();

        FileRecord record = new FileRecord();
        record.setFileName(originalName);
        record.setStatus(FileStatus.IN_PROGRESS);
        record = repository.save(record);

        String sourcePath = "input/" + record.getId() + "/" + originalName;
        minioService.uploadFile(sourcePath, file);

        record.setSourcePath(sourcePath);
        record = repository.save(record);

        kafkaProducer.sendConversionRequest(new ConversionRequest(record.getId().toString(), sourcePath));

        return record.getId();
    }

    public FileRecord getFileRecord(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Файл не найден."));
    }

    public InputStreamResource downloadFile(FileRecord record) throws Exception {
        if (record.getStatus() != FileStatus.SUCCESS || record.getConvertedPath() == null) {
            throw new IllegalStateException("Файл еще не готов. Текущий статус: " + record.getStatus());
        }

        InputStream stream = minioService.downloadFile(record.getConvertedPath());
        return new InputStreamResource(stream);
    }
}