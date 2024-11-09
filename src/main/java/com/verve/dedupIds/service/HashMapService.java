package com.verve.dedupIds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Initial simple impl for the dedupe logic but this won't satisfy the Extension 2 so moved to redis
 */
@Service
@EnableScheduling
public class HashMapService {

    private static final Logger logger = LoggerFactory.getLogger(HashMapService.class);

    // thread-safe map to store unique ids
    private ConcurrentHashMap<Integer, Boolean> requestMap = new ConcurrentHashMap<>();
    private AtomicInteger uniqueCount = new AtomicInteger(0);


    public String processIdsUsingHashMap(int id, String endpoint){
        try {
            // Deduplication check
            if (requestMap.putIfAbsent(id, true) == null) {
                uniqueCount.incrementAndGet();
            }
            // If an endpoint is provided, fire an http get request
            if (endpoint != null) {
                sendHttpRequest(endpoint);
            }
            System.out.println("Processing the id: "+ id);
            return "ok";
        } catch (Exception e) {
            logger.error("Error while processing the Id: {}", id, e);
            return "failed";
        }
    }

    // Logging the count of unique requests every 1 minute
    @Scheduled(fixedRate = 60000)
    public void logUniqueRequests() {
        int count = uniqueCount.getAndSet(0);
        logger.info("Unique requests in the last minute-HashMap: {}", count);
        requestMap.clear();
    }

    // mehtod to send the http get request with unique request count as query parameter
    private void sendHttpRequest(String endpoint) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = endpoint + "?count=" + uniqueCount.get();
                ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
                logger.info("Sent HTTP request to: {}. Response code: {}, Response body: {}",
                        endpoint, responseEntity.getStatusCode().value(), responseEntity.getBody());
        } catch (Exception e) {
            logger.error("Error sending HTTP request to: {}", endpoint, e);
        }
    }
}
