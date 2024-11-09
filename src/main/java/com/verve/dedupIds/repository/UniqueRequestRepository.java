package com.verve.dedupIds.repository;

import com.verve.dedupIds.model.UniqueRequests;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UniqueRequestRepository extends ReactiveCassandraRepository<UniqueRequests, String> {
    @Query("SELECT COUNT(*) FROM unique_requests WHERE minute = ?0")
    Mono<Integer> countByMinute(String minute);
}

