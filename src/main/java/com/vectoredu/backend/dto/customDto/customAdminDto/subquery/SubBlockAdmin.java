package com.vectoredu.backend.dto.customDto.customAdminDto.subquery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubBlockAdmin {
    private Long id;
    private String title;
    private String imageUrl;
    private int order;
}
