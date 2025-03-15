package com.vectoredu.backend.dto.authDto.request;

import com.vectoredu.backend.util.validators.ValidEmail;
import com.vectoredu.backend.util.validators.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserDto {

    @NotBlank()
    private String firstName;

    @NotBlank()
    private String lastName;

    @ValidEmail
    private String email;

    @ValidPassword
    private String password;

}
