package com.appsdeveloperblog.ws.consumerservice.io;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long>, Serializable {
    ProcessedEventEntity findByMessageId(String messageId);
}
