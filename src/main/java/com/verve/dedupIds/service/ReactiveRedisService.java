package com.verve.dedupIds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Latest version, using web flux(reactive programming paradigm) to tackle the concurrent thread limit
 * and blocking resources. Significantly able to increase thr throughput
 */
@Service
@EnableScheduling
public class ReactiveRedisService {
    private static final String TOPIC = "unique_requests";

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ReactiveRedisService.class);

    public String getCurrentMinute(){
        String currentTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString();
        LocalDateTime dateTime = LocalDateTime.parse(currentTime).truncatedTo(ChronoUnit.MINUTES);
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public Mono<String> processIds(int id, String endpoint) {
        String currMin = getCurrentMinute();
        return reactiveRedisTemplate.opsForSet().add(currMin, String.valueOf(id))
                .flatMap(added -> {
                    if (endpoint != null && !endpoint.isEmpty()) {
                        return sendHttpRequest(endpoint);  // Asynchronously send HTTP request
// EXTENSION 1 ->       //sendHTTPPostRequest(endpoint);
                    }
//                    System.out.println("Processing id: "+ id);
                    return Mono.just("ok");
                })
                .onErrorResume(e -> {
                    logger.error("Error in processing the request", e);
                    return Mono.just("failed");
                });
    }

    //Runs each minute
    @Scheduled(fixedRate = 60000)
    public void logUniqueRequests() {
        String currMin = getCurrentMinute();
        reactiveRedisTemplate.opsForSet().size(currMin)
                .subscribe(uniqueCount -> {
                    logger.info("Unique requests in the last minute - reactive Redis: {}", uniqueCount);
                    /*EXTENSION 3 -> WRITING TO KAFKA
                    kafkaTemplate.send(TOPIC, String.valueOf(count));
                     */
                    reactiveRedisTemplate.expire(currMin, Duration.ofMinutes(5)).subscribe();
                });
    }


    // Asynchronously send HTTP Get request using WebClient instead of httpclient
    private Mono<String> sendHttpRequest(String endpoint) {
        return WebClient.create(endpoint + "?count=" +
                        reactiveRedisTemplate.opsForSet().size(getCurrentMinute()))
                .get()
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                    logger.info("Sent request to: {}. and the response is: {}",
                            endpoint, response.getStatusCode());
                    return "ok";
                })
                .onErrorResume(e -> {
                    logger.error("Error in sending HTTP request: {}", endpoint, e);
                    return Mono.just("failed");
                });
    }

    // Asynchronously send http request with body using WebClient
    private Mono<String> sendHTTPPostRequest(String endpoint) {
        return WebClient.create(endpoint)
                .post()
                .bodyValue("{\"count\":" +
                        reactiveRedisTemplate.opsForSet().size(getCurrentMinute()) + "}")
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                    logger.info("Sent POST request to: {}. and the response is: {}",
                            endpoint, response.getStatusCode());
                    return "ok";
                })
                .onErrorResume(e -> {
                    logger.error("Error in sending HTTP POST request: {}", endpoint, e);
                    return Mono.just("failed");
                });
    }


}
