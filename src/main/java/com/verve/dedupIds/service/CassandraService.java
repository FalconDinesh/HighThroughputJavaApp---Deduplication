package com.verve.dedupIds.service;

import com.verve.dedupIds.model.UniqueRequests;
import com.verve.dedupIds.repository.UniqueRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@EnableScheduling
public class CassandraService {

    private static final Logger logger = LoggerFactory.getLogger(CassandraService.class);

    @Autowired
    private UniqueRequestRepository uniqueRequestRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Mono<String> processRequest(int id, String endpoint) {
        String currentMinute = LocalDateTime.now().format(formatter);
        UniqueRequests requestKey = new UniqueRequests(currentMinute, id);

        // Insert the unique request entry directly into Cassandra, don't need to perform
        // any check since cassandra automatically does the deduplication if primary keys were same
        try {
            System.out.println("Processing id: "+  id);
            uniqueRequestRepository.save(requestKey).then(Mono.just("ok"));
            return Mono.just("ok");
        }
        catch (Exception e){
            logger.error("Error processing request", e);
            return Mono.just("failed");
        }
    }

    @Scheduled(fixedRate = 60000)
    public void logUniqueRequests() {
        String currentMinute = LocalDateTime.now().format(formatter);
        Mono<Integer> count = uniqueRequestRepository.countByMinute(currentMinute);
        logger.info("Unique requests in the last minute - Cassandra: {}", count);
    }
}
