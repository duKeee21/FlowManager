package com.ykul.flowManager.integration;

import com.ykul.flowManager.service.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinioServiceIT extends AbstractIntegrationTest {

    @Autowired
    private MinioService minioService;

    private static final String BUCKET = "test-bucket";

    @BeforeAll
    static void setupBucket() throws Exception {
        try (MinioClient client = MinioClient.builder()
                .endpoint(minio.getS3URL())
                .credentials(minio.getUserName(), minio.getPassword())
                .build()) {

            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
            }
        }
    }


    @Test
    void uploadAndDownload_roundTrip() throws Exception {
        String objectName = "test/" + UUID.randomUUID() + "/test.txt";
        byte[] content = "Test!".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", content);

        minioService.uploadFile(objectName, file);

        try (InputStream stream = minioService.downloadFile(objectName)) {
            byte[] downloaded = stream.readAllBytes();
            assertThat(downloaded).isEqualTo(content);
        }
    }

    @Test
    void downloadFile_notExists_throws() {
        String nonExistentObject = "test/does-not-exist/" + UUID.randomUUID() + "/fake.txt";

        assertThatThrownBy(() -> minioService.downloadFile(nonExistentObject))
                .isInstanceOf(Exception.class);
    }
}