package com.vectoredu.backend.dto.courseDto.response;

import com.vectoredu.backend.model.enums.Difficulty;
import com.vectoredu.backend.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseResponse {
    private Long id;
    private String title;
    private Integer price;
    private Difficulty difficulty;
    private Status status;
    private String imageUrl;
    private String chat;
}
