package com.travel.system.search;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "travel_user_account")
public class UserAccountDocument {
    @Id
    private String id;
    private String username;
    private String displayName;
    private String interests;
}
