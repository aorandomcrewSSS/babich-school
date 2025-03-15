package com.vectoredu.backend.model.progress;

import com.vectoredu.backend.model.Lesson;
import com.vectoredu.backend.model.User;
import com.vectoredu.backend.model.enums.ProgressStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lesson_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"lesson_id", "user_id"}))
public class LessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status;

    public LessonProgress(Lesson lesson, User user, ProgressStatus status) {
        this.lesson = lesson;
        this.user = user;
        this.status = status;
    }
}

