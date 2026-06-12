package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

@Service
public class DiaryHeatService {
    public double compute(Diary diary) {
        double heat = value(diary.getScore()) * 10.0
                + value(diary.getViews()) * 0.2
                + value(diary.getLikeCount()) * 5.0
                + value(diary.getFavoriteCount()) * 8.0
                + value(diary.getCommentCount()) * 6.0
                + value(diary.getShareCount()) * 10.0;
        return Math.round(heat * 10.0) / 10.0;
    }

    private double value(Number number) {
        return number == null ? 0.0 : number.doubleValue();
    }
}
