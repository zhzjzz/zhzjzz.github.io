package com.travel.system.search;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "travel_diary")
public class DiaryDocument {
    @Id
    private String id;
    private String title;
    private String content;
    private String destinationName;
}
