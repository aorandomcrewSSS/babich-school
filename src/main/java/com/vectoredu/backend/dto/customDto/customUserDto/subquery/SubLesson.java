package com.vectoredu.backend.dto.customDto.customUserDto.subquery;

import com.vectoredu.backend.model.enums.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubLesson {
    private Long lessonId;
    private String lessonTitle;
    private String imageUrl;
    private ProgressStatus progress;
    private int order;
}
