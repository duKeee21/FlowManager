package com.ykul.flowManager.service;

import com.ykul.flowManager.dto.ConversionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    @Value("${app.kafka.topics.requests}")
    private  String requestsTopic;
    private final KafkaTemplate<String, ConversionRequest> kafkaTemplate;

    public void sendConversionRequest(ConversionRequest request) {
        kafkaTemplate.send(requestsTopic, request);
    }
}