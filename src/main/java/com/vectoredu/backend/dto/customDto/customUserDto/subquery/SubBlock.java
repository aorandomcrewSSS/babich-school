package com.vectoredu.backend.dto.customDto.customUserDto.subquery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubBlock {
    private Long blockId;
    private String blockTitle;
    private String imageUrl;
    private Integer completedLessons;
    private Integer lessonsCount;
    private int order;
}
