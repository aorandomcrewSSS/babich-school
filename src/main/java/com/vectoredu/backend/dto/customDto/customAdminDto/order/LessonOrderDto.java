package com.vectoredu.backend.dto.customDto.customAdminDto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LessonOrderDto {
    private Long id;
    private int order;
}
