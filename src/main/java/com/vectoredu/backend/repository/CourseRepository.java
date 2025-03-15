package com.vectoredu.backend.repository;

import com.vectoredu.backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.blocks WHERE c.id = :courseId")
    Optional<Course> findByIdWithBlocks(@Param("courseId") Long courseId);
}
