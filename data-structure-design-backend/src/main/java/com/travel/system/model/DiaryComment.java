package com.travel.system.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiaryComment {
    private Long id;
    private Long diaryId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
}
