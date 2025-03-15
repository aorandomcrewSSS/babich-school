package com.vectoredu.backend.dto.courseDto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
@AllArgsConstructor
@NoArgsConstructor
public class LessonToCreate {
    private Long blockId;
    private String title;
    private String description;
    private int order;
}
