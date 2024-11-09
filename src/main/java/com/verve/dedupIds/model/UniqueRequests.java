package com.verve.dedupIds.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Getter
@Setter
@ToString
@Table(value = "unique_requests")
public class UniqueRequests {

    @PrimaryKeyColumn(name = "minute", type = PrimaryKeyType.PARTITIONED)
    private String minute;

    @PrimaryKeyColumn(name = "id", type = PrimaryKeyType.CLUSTERED)
    private int id;

    public UniqueRequests(String minute, int id) {
        this.minute = minute;
        this.id = id;
    }
}
