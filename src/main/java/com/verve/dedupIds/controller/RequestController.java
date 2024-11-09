package com.verve.dedupIds.controller;

import com.verve.dedupIds.service.CassandraService;
import com.verve.dedupIds.service.HashMapService;
import com.verve.dedupIds.service.ReactiveRedisService;
import com.verve.dedupIds.service.RedisSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


/**
 * Controller class to handle in coming requests - servlet dispatcher like
 */
@RestController
public class RequestController {

    @Autowired
    HashMapService hashMapService;

    @Autowired
    RedisSetService redisSetService;

    @Autowired
    ReactiveRedisService reactiveRedisService;

    @Autowired
    CassandraService cassandraService;

    private static final Logger logger = LoggerFactory.getLogger(RequestController.class);

    /***
     //* Tried with sync requests in default tomcat thread pools-200
    @GetMapping(value = "/api/verve/accept")
    public String acceptIds(@RequestParam int id, @RequestParam(required = false) String endpoint){
//        return hashMapService.processIdsUsingHashMap(id, endpoint); // 8500 rps
             //               or
        return redisSetService.processIdsUsingRedisSet(id, endpoint); //8000 rps
    }
     */

    /*
    Using spring webFLux to achieve multiple tasks simultaneously without blocking by
    asynchronous processing
     */
    @GetMapping(value = "/api/verve/accept")
    public Mono<String> asyncAcceptIds(@RequestParam int id, @RequestParam(required = false) String endpoint){
        return reactiveRedisService.processIds(id, endpoint);   // 12-13k rps
//        return cassandraService.processRequest(id, endpoint);
    }
}


