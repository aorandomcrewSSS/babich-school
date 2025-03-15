package com.vectoredu.backend.dto.customDto.customAdminDto;

import com.vectoredu.backend.dto.customDto.customAdminDto.subquery.SubBlockAdmin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomCourseAdminDto {
    private String courseTitle;
    private List<SubBlockAdmin> blocks;
    private String chat;
}
