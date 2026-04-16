package com.travel.system.search;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "travel_food")
public class FoodDocument {
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String cuisine;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String storeName;
    
    @Field(type = FieldType.Keyword)
    private String destinationName;
    
    private Double heat;
    private Double rating;
}
