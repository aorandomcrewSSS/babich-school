package com.vectoredu.backend.dto.customDto.customUserDto;

import com.vectoredu.backend.dto.customDto.customUserDto.subquery.SubBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomCourseDto {
    private String courseTitle;
    private List<SubBlock> blocks;
    private String chat;
}
