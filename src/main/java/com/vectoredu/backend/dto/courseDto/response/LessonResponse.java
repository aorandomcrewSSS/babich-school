package com.vectoredu.backend.dto.courseDto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LessonResponse {
    private Long id;
    private Long blockId;
    private String title;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private String sheetUrl;
    private int order;
}
