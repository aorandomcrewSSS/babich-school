package com.vectoredu.backend.model;

import com.vectoredu.backend.model.progress.BlockProgress;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blocks")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "order_position")
    private int order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @OneToMany(mappedBy = "block", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();

    @OneToMany(mappedBy = "block", fetch = FetchType.LAZY)
    private List<BlockProgress> blockProgress;
}
