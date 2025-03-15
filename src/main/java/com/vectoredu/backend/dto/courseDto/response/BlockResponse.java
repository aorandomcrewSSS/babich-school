package com.vectoredu.backend.dto.courseDto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlockResponse {
    private Long id;
    private String title;
    private Long courseId;
    private String imageUrl;
    private int order;
}
