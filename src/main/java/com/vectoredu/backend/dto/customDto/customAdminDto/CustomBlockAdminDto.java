package com.vectoredu.backend.dto.customDto.customAdminDto;

import com.vectoredu.backend.dto.customDto.customAdminDto.subquery.SubLessonAdmin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomBlockAdminDto {
    private String blockTitle;
    private List<SubLessonAdmin> lessons;
}
