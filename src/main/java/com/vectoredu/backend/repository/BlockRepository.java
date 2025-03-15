package com.vectoredu.backend.repository;

import com.vectoredu.backend.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    // Метод для нахождения всех блоков по id курса
    List<Block> findByCourseId(Long courseId);

    @Query("SELECT b FROM Block b LEFT JOIN FETCH b.lessons WHERE b.course.id = :courseId")
    List<Block> findBlocksWithLessonsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT MAX(b.order) FROM Block b WHERE b.course.id = :courseId")
    Integer findMaxOrderByCourseId(@Param("courseId") Long courseId);
}
