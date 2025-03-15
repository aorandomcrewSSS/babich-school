package com.vectoredu.backend.util.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {
    String message() default "Password must contain at least one uppercase letter, one digit, and be at least 8 characters long";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
