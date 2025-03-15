package com.vectoredu.backend.model;

import com.vectoredu.backend.model.progress.LessonProgress;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "lessons")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "video_url")
    private String videoUrl; // Ссылка на видео в S3

    @Column(name = "sheet_url")
    private String sheetUrl;

    private String description;

    @Column(name = "order_position")
    private int order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<LessonProgress> lessonProgresses;
}
