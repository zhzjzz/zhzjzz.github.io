package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

@Service
public class DiaryHeatService {
    public double compute(Diary diary) {
        double score = value(diary.getScore()) * 20.0;
        double views = Math.log10(value(diary.getViews()) + 1.0) * 12.0;
        double likes = value(diary.getLikeCount()) * 2.0;
        double favorites = value(diary.getFavoriteCount()) * 3.0;
        double comments = value(diary.getCommentCount()) * 4.0;
        double shares = value(diary.getShareCount()) * 5.0;
        return Math.round((score + views + likes + favorites + comments + shares) * 10.0) / 10.0;
    }

    private double value(Number number) {
        return number == null ? 0.0 : number.doubleValue();
    }
}
