package com.appsdeveloperblog.ws.consumerservice.handler;

import com.appsdeveloperblog.ws.consumerservice.error.NotRetryableException;
import com.appsdeveloperblog.ws.consumerservice.error.RetryableException;
import com.appsdeveloperblog.ws.consumerservice.io.ProcessedEventEntity;
import com.appsdeveloperblog.ws.consumerservice.io.ProcessedEventRepository;
import com.appsdeveloperblog.ws.core.ProductCreatedEvent;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@KafkaListener(topics = {"product-created-events-topic"})
public class ProductCreatedEventHandler {

    private final RestTemplate restTemplate;
    private final ProcessedEventRepository processedEventRepository;

    private final Logger logger = LoggerFactory.getLogger(ProductCreatedEventHandler.class);

    public ProductCreatedEventHandler(RestTemplate restTemplate, ProcessedEventRepository processedEventRepository) {
        this.restTemplate = restTemplate;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaHandler
    @Transactional
    public void handle(
            @Payload ProductCreatedEvent productCreatedEvent,
            @Header(value = "messageId", required = true) String messageId,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey
    ) {
        logger.info("Received event {}", productCreatedEvent.getTitle());
        // check if this message was already processed before
        ProcessedEventEntity existingRecord = processedEventRepository.findByMessageId(messageId);
        if (existingRecord != null) {
            logger.info("Found existing record for messageId={}", messageId);
            return;
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:8082/response/200", HttpMethod.GET, null, String.class);
            if (response.getStatusCode().value() == HttpStatus.OK.value()) {
                logger.info("Successfully received response {}", response.getBody());
            }
        } catch (ResourceAccessException ex) {
            logger.error("Error while fetching product created event '{}'", ex.getMessage());
            throw new RetryableException("Error while fetching product created event");
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new NotRetryableException(e);
        }

        try {
            //Save a unique message id in a database table
            processedEventRepository.save(new ProcessedEventEntity(messageId, productCreatedEvent.getProductId()));
        } catch (DataIntegrityViolationException ex) {
            throw new NotRetryableException(ex);
        }
    }
}
