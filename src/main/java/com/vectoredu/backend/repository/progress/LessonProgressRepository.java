package com.vectoredu.backend.repository.progress;

import com.vectoredu.backend.model.Lesson;
import com.vectoredu.backend.model.User;
import com.vectoredu.backend.model.enums.ProgressStatus;
import com.vectoredu.backend.model.progress.LessonProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByLessonAndUser(Lesson lesson, User user);

    List<LessonProgress> findByUserAndLessonIn(User user, List<Lesson> lessons);

    List<LessonProgress> findByLessonInAndUser(List<Lesson> lessons, User user);

    List<LessonProgress> findByUserAndStatus(User user, ProgressStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lp FROM LessonProgress lp WHERE lp.lesson = :lesson AND lp.user = :user")
    Optional<LessonProgress> findByLessonAndUserWithLock(@Param("lesson") Lesson lesson, @Param("user") User user);


    @Modifying
    @Transactional
    @Query("DELETE FROM LessonProgress lp WHERE lp.lesson.id = :lessonId")
    void deleteByLessonId(@Param("lessonId") Long lessonId);
}
