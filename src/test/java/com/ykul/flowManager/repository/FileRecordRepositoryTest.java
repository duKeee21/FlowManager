package com.ykul.flowManager.repository;

import com.ykul.flowManager.entity.FileRecord;
import com.ykul.flowManager.entity.FileStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
class FileRecordRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private FileRecordRepository repository;

    @Test
    void saveAndFindById() {
        FileRecord record = new FileRecord();
        record.setFileName("test.txt");
        record.setStatus(FileStatus.SUCCESS);
        record.setConvertedPath("converted/qwerty.pdf");

        FileRecord saved = repository.save(record);

        assertThat(saved.getId()).isNotNull();

        FileRecord found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getFileName()).isEqualTo("test.txt");
        assertThat(found.getStatus()).isEqualTo(FileStatus.SUCCESS);
        assertThat(found.getConvertedPath()).isEqualTo("converted/qwerty.pdf");
    }
}