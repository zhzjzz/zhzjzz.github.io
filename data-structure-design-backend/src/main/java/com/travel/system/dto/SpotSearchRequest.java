package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpotSearchRequest {
    private String keyword;
    private String category;
    private String sortBy;
    private Integer limit;
}
