package com.vectoredu.backend.dto.customDto.customAdminDto;

import com.vectoredu.backend.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomUserAdminDto {
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private Integer courseCount;
}
