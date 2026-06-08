package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

@Service
public class DiaryHeatService {
    public double compute(Diary diary) {
        return Math.round(value(diary.getLikeCount()) * 10.0) / 10.0;
    }

    private double value(Number number) {
        return number == null ? 0.0 : number.doubleValue();
    }
}
