# Hi, Welcome!
This document outlines the development process of this Java application that processes HTTP requests with a focus on handling unique requests efficiently using reactive programming. Below are the step-by-step processes and the challenges that I faced and different approaches I tried. 


I wanted to let you know that the assignment was completed through an iterative process. After the first approach, I reviewed the results and made some refinements. Each iteration brought me closer to the desired outcome, ensuring the final version aligns with the goals. This approach allowed me to fine-tune the work progressively and address any specific needs.


## Approaches Tried
    1. Using ConcurrentHashMap in local (deprecated)
    2. Using Redis as a distributed in-mem data store. (Deprecated)
    3. Integrated Spring Webflux with Redis (Currently In-Use)


### Initial setup


* Started by creating a simple Spring Boot application with a REST endpoint `api/verve/accept` that accepts an integer ID as a mandatory query parameter. The response returns `ok` or `failed` based on processing.


* To find out the unique integers processed in a minute, I decided to keep the operations as minimal as possible to reduce the operational overhead. So I initially started with **ConcurrentHashMap** as it's **thread-safe and ideal for concurrent environments**. Performed a put for each request and, using a scheduler, fetched the number of keys in the hashmap, then later cleared all the contents in it after each minute.


* Increased the default pool size of embedded tomcat in Spring Boot from 200 to 500. `server.tomcat.threads.max=500`


* It worked with the basic setup. I tried to find the maximum throughput of the application. So I looked into some of the `load testing softwares` and picked up **Apache Jmeter** as it is simple and easy to use.


* In the initial testing with `200k requests`, the application was able to process almost on an average of **8~9k requests per second** and with a latency of **18ms**. But the drawback is that since the hashmap is in local memory, when the application is deployed at the multi-instance level, it'll fail to identify duplicate IDs also got timeout issue for 1% of requests. So I moved on to the next approach.

&nbsp;
![Using ConcurrentHashMap](https://raw.githubusercontent.com/FalconDinesh/HighThroughputJavaApp/refs/heads/main/benchmark-jmeter/HashMap%20-%20report.png)


### Using Redis

* To achieve the multi-instace deduplication, I have decided to use distributed cache, or db; since Redis is an in-memory data store, I choose Redis. When we use the traditional get/set approach, it involves operational overhead, so I decided to go with `RedisSet`, which will perform it on the server side.

* I have redis running in the docker.

* The functionality is simple. we push the incomming request IDs into a set for the given **minute as a key** and for each minute getting the count of the elements in it.


* I tested using the same Jmeter with the same 200k requests, and the application was able to process **8~9 requests per second** and also with very low **avg latency of 12ms**. I'm using a very traditional way of programming and synchronous usage of Redis and Rest APIs. So I decided to try something that runs `non-blocking and asynchronously`.

&nbsp;
![Using Redis](https://raw.githubusercontent.com/FalconDinesh/HighThroughputJavaApp/refs/heads/main/benchmark-jmeter/redis%20-%20benchmark.png)



### Integrating Redis with Spring Web Flux


* WebFlux is non-blocking and uses a reactive programming paradigm, which allows the application to handle more concurrent requests efficiently.


* I made a few changes to the application to make it asynchronous and nonblocking.
```
    1. Replaced HttpClient with WebClient from WebFlux, which allows to send asynchronous requests to the endpoint.
    
    2. Mono<String> to enable async handling of requests.
    
    3. Used ReactiveRedisTemplate instead of Jedis to interact with Redis asynchronously.
```

* By these changes, I was able to effectively reduce the latency and throughput of the application. When tested with the same volume of 200k requests from `1000 clients each sending 200 req/sec`, the application was able to process **12~13k requests per second** with low latency (10 ms).


![Reactive Programming with Redis](https://raw.githubusercontent.com/FalconDinesh/HighThroughputJavaApp/refs/heads/main/benchmark-jmeter/Reactive_redis-benchmark.png)


* I further dug down and found that my JVM with web Flux has reached the max concurrent thread pool limit, so I `reduced the client size to 50 and each of them sending 4000 requests.` The latency drop was huge and came down to **3.89ms** ms, achieving up to more than **12k requests/sec**. I tested with my personal laptop with 16 GB of memory and 4 cores of processor, so when deployed in multiple servers with load balancers it should handle more, I guess.


```bash
The results indicate that the application will likely scale better as you increase the request volume, especially with load balancing or additional resources and optimizations like auto-scaling and more code optimizations.
```


#### Since I'm familiar with Cassandra, I had an idea to use Cassandra as a unique value store instead of Redis.


* In Cassandra, the combination of primary keys is always unique; if we try to insert a record with the same primary keys, it'll replace the existing record. So by setting the minute value as the parititon key and the actual ID as the clustering key, we could just perform an insert and fetch the record count of each minute. Since Cassandra is a write-heavy database, it could withstand more workload, especially if done using Spark or other processing engines it can easily process upto 20k ops/sec also we could use the data for history/audit/analytic purpose.


* It is not completed; I just tried it.


## This is one of the most enjoyable tasks I have done. Thanks team!.