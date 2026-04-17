package com.travel.system.search;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "travel_road_edge")
public class RoadEdgeDocument {
    @Id
    private String id;
    private Long fromNodeId;
    private String fromNodeName;
    private Long toNodeId;
    private String toNodeName;
    private Double distanceMeters;
    private Double idealSpeed;
    private Double congestion;
    private String allowedTransport;
}
