package com.appsdeveloperblog.ws.productsmicroservice.service;

import com.appsdeveloperblog.ws.core.ProductCreatedEvent;
import com.appsdeveloperblog.ws.productsmicroservice.rest.Product;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service // mark this class as spring component
// and allow Spring framework to discover this class at the time when our application starts up
// create a new Instance of this class and add it to spring context
// then we can use spring dependency injection and inject it into other classes
public class ProductServiceImpl implements ProductService {

    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
    private final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String createProduct(Product product) throws Exception {
        String productId = UUID.randomUUID().toString();

        // TODO: Persist product Details into database table before publishing an event
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(
                productId,
                product.getTitle(),
                product.getPrice(),
                product.getQuantity()
        );

        // kafkaTemplate.send("product-created-events-topic", productId, productCreatedEvent); // asynchronous communication
        // not waiting for acknowledgement

        /*CompletableFuture<SendResult<String, ProductCreatedEvent>> future =
                kafkaTemplate.send("product-created-events-topic", productId, productCreatedEvent);

        future.whenComplete((result, exception) -> {
            if (exception != null) {
                logger.error("Failed to send message: " + exception.getMessage(), exception);
            } else {
                logger.info("Message sent successfully: " + result.getRecordMetadata());
            }
        });*/

        //future.join();// this will block the current thread until the future is complete ,
        // and returns the result of computation (became a synchronous operation)
        // remove this line if you want an asynchronous => no waiting

        ProducerRecord<String, ProductCreatedEvent> record = new ProducerRecord<>(
                "product-created-events-topic",
                productId,
                productCreatedEvent
        );
        record.headers().add("messageId", UUID.randomUUID().toString().getBytes());

        SendResult<String, ProductCreatedEvent> result =
                kafkaTemplate.send(record).get();
        logger.info("Partition: {}", result.getRecordMetadata().partition());
        logger.info("Offset: {}", result.getRecordMetadata().offset());
        logger.info("Timestamp: {}", result.getRecordMetadata().timestamp());
        logger.info("Topic: {}", result.getRecordMetadata().topic());

        return productId;
    }
}
