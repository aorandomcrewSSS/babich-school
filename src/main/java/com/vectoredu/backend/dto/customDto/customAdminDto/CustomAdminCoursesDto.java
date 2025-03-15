package com.vectoredu.backend.dto.customDto.customAdminDto;

import com.vectoredu.backend.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomAdminCoursesDto {
    private Long courseId;
    private String title;
    private Integer price;
    private Status status;
    private Integer userCount;
    private String imageUrl;
}
