package com.travel.system.dto;

import lombok.Data;

@Data
public class ItineraryImportRequest {
    private String sourceType;
    private String text;
    private Long diaryId;
    private String owner;
}
