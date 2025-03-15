package com.vectoredu.backend.dto.courseDto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class BlockToCreate {
    private Long courseId;
    private String title;
    private int order;
}
