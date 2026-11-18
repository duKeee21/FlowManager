package com.ykul.flowManager.consumer;

import com.ykul.flowManager.dto.ConversionResult;
import com.ykul.flowManager.entity.FileRecord;
import com.ykul.flowManager.entity.FileStatus;
import com.ykul.flowManager.repository.FileRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversionResultConsumerTest {

    @Mock
    private FileRecordRepository repository;
    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private ConversionResultConsumer consumer;

    @Test
    void consume_success_ackCalled() {
        UUID id = UUID.randomUUID();
        FileRecord record = new FileRecord();
        record.setId(id);
        record.setStatus(FileStatus.IN_PROGRESS);
        when(repository.findById(id)).thenReturn(Optional.of(record));

        ConversionResult result = new ConversionResult();
        result.setOriginalEventId(id.toString());
        result.setOutputFilePaths(List.of("converted/" + id + ".pdf"));

        consumer.consume(result, ack);

        ArgumentCaptor<FileRecord> captor = ArgumentCaptor.forClass(FileRecord.class);
        verify(repository).save(captor.capture());
        FileRecord saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(FileStatus.SUCCESS);
        assertThat(saved.getConvertedPath()).isEqualTo("converted/" + id + ".pdf");
        assertThat(saved.getErrorMessage()).isNull();

        verify(ack).acknowledge();
    }

    @Test
    void consume_error_ackCalled() {
        UUID id = UUID.randomUUID();
        FileRecord record = new FileRecord();
        record.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(record));

        ConversionResult result = new ConversionResult();
        result.setOriginalEventId(id.toString());
        result.setErrorMessage("Unsupported file type");

        consumer.consume(result, ack);

        ArgumentCaptor<FileRecord> captor = ArgumentCaptor.forClass(FileRecord.class);
        verify(repository).save(captor.capture());
        FileRecord saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(FileStatus.ERROR);
        assertThat(saved.getErrorMessage()).isEqualTo("Unsupported file type");

        verify(ack).acknowledge();
    }

    @Test
    void consume_invalidUuid_ackNotCalled() {
        ConversionResult result = new ConversionResult();
        result.setOriginalEventId("not-a-uuid");
        result.setErrorMessage("whatever");

        assertThatThrownBy(() -> consumer.consume(result, ack))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ack, never()).acknowledge();
        verify(repository, never()).save(any());
    }

    @Test
    void consume_fileNotFound_ackCalled() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        ConversionResult result = new ConversionResult();
        result.setOriginalEventId(id.toString());
        result.setErrorMessage("error");

        consumer.consume(result, ack);

        verify(repository, never()).save(any());
        verify(ack).acknowledge();
    }
}