package com.travel.system.search;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "travel_road_node")
public class RoadNodeDocument {
    @Id
    private String id;
    private String name;
    private String nodeType;
    private Double latitude;
    private Double longitude;
}
