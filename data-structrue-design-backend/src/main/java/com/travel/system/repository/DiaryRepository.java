package com.travel.system.repository;

import com.travel.system.model.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findByTitleContainingIgnoreCase(String title);
}
