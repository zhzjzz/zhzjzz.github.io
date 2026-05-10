package com.travel.system.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Diary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(length = 10000)
    private String content;
    private String mediaUrl;
    private String mediaType;
    private String compressedMediaUrl;
    private Long originalSizeBytes;
    private Long compressedSizeBytes;
    private String compressionStatus;
    private String aigcAnimationUrl;
    private String aigcStatus;
    private Double heatScore;
    private Long likeCount;
    private Long favoriteCount;
    private Long commentCount;
    private Long shareCount;
    private Boolean isPublic;
    private String shareToken;
    private Double score;
    private Long views;
    private LocalDateTime publishedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private Destination destination;
}
