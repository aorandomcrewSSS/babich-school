package com.vectoredu.backend.dto.courseDto.request;

import com.vectoredu.backend.model.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class CourseToCreate {
    private String title;
    private Integer price;
    private Difficulty difficulty;
    private String chat;
}
