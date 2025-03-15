package com.vectoredu.backend.dto.customDto.customUserDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomLessonDto {
    private String lessonTitle;
    private String videoUrl;
    private String description;
    private String sheetUrl;
}
