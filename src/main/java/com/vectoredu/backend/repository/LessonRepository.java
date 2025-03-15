package com.vectoredu.backend.repository;

import com.vectoredu.backend.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @Query("SELECT MAX(l.order) FROM Lesson l WHERE l.block.id = :blockId")
    Integer findMaxOrderByBlockId(@Param("blockId") Long blockId);
}
