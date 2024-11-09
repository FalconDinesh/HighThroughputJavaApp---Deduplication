package com.verve.dedupIds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DedupIdsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DedupIdsApplication.class, args);
	}
}








//run -d --name redis_server -p 6379:6379 redis:latest
//

// To generate csv file
// jmeter -n -t "C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\Verve Assignment.jmx" -l C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\results.csv

// To generate Graph - HashMap
// jmeter -n -t "C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\Verve Assignment.jmx" -l C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\hashMap_results.csv -e -o C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\Jmeter_Graph_HashMapReport

// To generate Graph - Redis
// jmeter -n -t "C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\Verve Assignment.jmx" -l C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\redis_results.csv -e -o C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\Jmeter_Graph_Redis

// To generate Graph - Reactive Redis
//jmeter -n -t "C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\Verve Assignment.jmx" -l C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\reactive_redis_results.csv -e -o C:\Users\DELL\Documents\JOB\Verve\HighThroughputJavaApp\benchmark-jmeter\Jmeter_Graph_Reactive_Redis_report