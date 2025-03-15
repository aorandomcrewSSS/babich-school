package com.vectoredu.backend.dto.customDto.customUserDto;

import com.vectoredu.backend.dto.customDto.customUserDto.subquery.SubLesson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomBlockDto {
    private String blockTitle;
    private List<SubLesson> lessons;
}
