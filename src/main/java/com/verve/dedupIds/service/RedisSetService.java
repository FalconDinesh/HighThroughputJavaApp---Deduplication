package com.verve.dedupIds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * The 2nd impl where using redis to satisfy the extension 2, using distributed in-mem cache
 */
@Service
@EnableScheduling
public class RedisSetService {

    private static final Logger logger = LoggerFactory.getLogger(RedisSetService.class);

    private JedisPool jedisPool;

    public RedisSetService() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(64);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        int redisTimeout = 2000;
        int redisPort = 6379;
        String redisHost = "localhost";
        jedisPool = new JedisPool(poolConfig, redisHost, redisPort, redisTimeout);
    }

    public String getCurrentMinute(){
        String currentTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString();
        LocalDateTime dateTime = LocalDateTime.parse(currentTime).truncatedTo(ChronoUnit.MINUTES);
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public void setKeyExpiry(String minuteKey, int ttlInSeconds) {
        try(Jedis jedis = jedisPool.getResource()){
            jedis.expire(minuteKey, ttlInSeconds);  // Set expiration for cleanup
        }catch (Exception e) {
            logger.error("Error in setting key expiry for: {}", minuteKey, e);
        }
    }

    // getting the count of unique requests for the current minute
    public long getUniqueCount(String minuteKey) {
        try(Jedis jedis = jedisPool.getResource()) {
            return jedis.scard(minuteKey);
        }catch (Exception e) {
            logger.error("Error in getting unique count for key: {}", minuteKey, e);
            return 0;
        }
    }

    public String processIdsUsingRedisSet(int id, String endpoint){
        String currMin = getCurrentMinute();
        try(Jedis jedis = jedisPool.getResource()){
//            System.out.println("Processing id: "+ id);
            jedis.sadd(currMin, String.valueOf(id));
            if(endpoint != null  && !endpoint.isEmpty()){
                sendHttpRequest(getUniqueCount(getCurrentMinute()), endpoint);
            }
            return "ok";
        } catch (Exception e) {
            logger.error("Error while processing Id: {}", id, e);
            return "failed";
        }
    }

    // method to send http GET request with count of unique requets as query parameter
    private void sendHttpRequest(Long count, String endpoint) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = endpoint + "?count=" + count;
                    ResponseEntity < String > responseEntity = restTemplate.getForEntity(url, String.class);
            logger.info("Sent request to: {}. Response code: {}, Response body: {}",
                    endpoint, responseEntity.getStatusCode().value(), responseEntity.getBody());
        } catch (Exception e) {
            logger.error("Error sending HTTP request to: {}", endpoint, e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void logUniqueRequests() {
        String currMin = getCurrentMinute();
        long uniqueCount = getUniqueCount(currMin);
        logger.info("Unique requests in the last minute - Redis: {}", uniqueCount);
        setKeyExpiry(currMin, 300);  // Expire keys after 5 mins
    }

    public JedisPoolConfig getJedisCongif(){
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(64);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        return poolConfig;
    }
    //note - should be removed in clean-up
    //things to change -current logic took for 100k requests is 25sec, can we use concurrency or async like webflux?

}