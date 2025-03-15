package com.vectoredu.backend.dto.customDto.customAdminDto.subquery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubLessonAdmin {
    private Long lessonId;
    private String lessonTitle;
    private String imageUrl;
    private int order;
}
