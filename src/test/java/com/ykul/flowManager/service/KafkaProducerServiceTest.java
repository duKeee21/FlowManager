package com.ykul.flowManager.service;

import com.ykul.flowManager.dto.ConversionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, ConversionRequest> kafkaTemplate;

    @Test
    void sendConversionRequest_sendsToCorrectTopic() {
        KafkaProducerService service = new KafkaProducerService(kafkaTemplate);

        ReflectionTestUtils.setField(service, "requestsTopic", "file-conversion-requests");

        ConversionRequest request = new ConversionRequest("id-123", "input/id-123/test.txt");

        service.sendConversionRequest(request);

        verify(kafkaTemplate).send(eq("file-conversion-requests"), eq(request));
    }
}