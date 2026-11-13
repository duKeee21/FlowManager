package com.ykul.flowManager.consumer;

import com.ykul.flowManager.dto.ConversionResult;
import com.ykul.flowManager.entity.FileStatus;
import com.ykul.flowManager.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@Component
@RequiredArgsConstructor
public class ConversionResultConsumer {

    private final FileRecordRepository repository;

    @KafkaListener(topics = "file-conversion-results", groupId = "flow-manager-group")
    public void consume(ConversionResult result) {
        UUID fileId = UUID.fromString(result.getOriginalEventId());

        repository.findById(fileId).ifPresentOrElse(record -> {
            if (result.getErrorMessage() != null) {
                record.setStatus(FileStatus.ERROR);
                record.setErrorMessage(result.getErrorMessage());
            } else {
                record.setStatus(FileStatus.SUCCESS);
                List<String> paths = result.getOutputFilePaths();
                if (paths != null && !paths.isEmpty()) {
                    record.setConvertedPath(paths.getFirst());
                }
            }
            repository.save(record);
        }, () -> log.warn("Файл с ID {} не найден", fileId));
    }
}