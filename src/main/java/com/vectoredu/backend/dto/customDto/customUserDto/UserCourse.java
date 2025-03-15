package com.vectoredu.backend.dto.customDto.customUserDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCourse {
    private Long id;
    private String title;
    private String imageUrl;
    private Integer completedBlocks;
    private Integer blocksCount;
}
