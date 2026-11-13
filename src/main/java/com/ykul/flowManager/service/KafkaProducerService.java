package com.ykul.flowManager.service;

import com.ykul.flowManager.dto.ConversionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private static final String TOPIC = "file-conversion-requests";
    private final KafkaTemplate<String, ConversionRequest> kafkaTemplate;

    public void sendConversionRequest(ConversionRequest request) {
        kafkaTemplate.send(TOPIC, request);
    }
}