package com.vectoredu.backend.controller;

import com.vectoredu.backend.dto.authDto.request.LoginUserDto;
import com.vectoredu.backend.dto.authDto.request.RefreshToken;
import com.vectoredu.backend.dto.authDto.request.RegisterUserDto;
import com.vectoredu.backend.dto.authDto.request.VerifyUserDto;
import com.vectoredu.backend.dto.authDto.response.LoginResponse;
import com.vectoredu.backend.model.User;
import com.vectoredu.backend.service.authservice.AuthenticationService;
import com.vectoredu.backend.service.authservice.EmailUpdateService;
import com.vectoredu.backend.service.authservice.JwtService;
import com.vectoredu.backend.service.authservice.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Аутентификация", description = "Операции, связанные с аутентификацией")
@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private final EmailUpdateService emailUpdateService;
    private final PasswordService passwordService;

    @Operation(summary = "Регистрация нового пользователя", responses = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Неверные данные")
    })
    @PostMapping("/signup")
    public ResponseEntity<User> register(@RequestBody RegisterUserDto registerUserDto) {
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @Operation(summary = "Аутентификация пользователя и получение JWT", responses = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно аутентифицирован"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    // Логика аутентификации и получения JWT токенов
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto){
        LoginResponse loginResponse = authenticationService.authenticate(loginUserDto);
        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Обновление access-токена", responses = {
            @ApiResponse(responseCode = "200", description = "Токен успешно обновлен"),
            @ApiResponse(responseCode = "401", description = "Ошибка валидации токена")
    })
    // Логика получения нового access токена по refresh токену
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshAccessToken(@RequestBody RefreshToken refreshToken){
        String newAccessToken = authenticationService.refreshAccessToken(refreshToken.getToken());
        return ResponseEntity.ok(newAccessToken);
    }

    @Operation(summary = "Подтверждение аккаунта пользователя", responses = {
            @ApiResponse(responseCode = "200", description = "Аккаунт успешно подтвержден"),
            @ApiResponse(responseCode = "400", description = "Неверные данные для подтверждения")
    })
    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        authenticationService.verifyUser(verifyUserDto);
        return ResponseEntity.ok("Аккаунт успешно подтвержден");
    }

    @Operation(summary = "Повторная отправка кода для подтверждения аккаунта", responses = {
            @ApiResponse(responseCode = "200", description = "Код для подтверждения успешно отправлен"),
            @ApiResponse(responseCode = "400", description = "Ошибка при отправке кода")
    })
    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        authenticationService.resendVerificationCode(email);
        return ResponseEntity.ok("Код для подтверждения отправлен");
    }

    /**
     * Запрос на восстановление пароля неаутентифицированным пользователем.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> requestPasswordReset(@RequestParam String email) {
        passwordService.requestPasswordReset(email);
        return ResponseEntity.ok("На вашу почту отправлено письмо с инструкциями по восстановлению пароля.");
    }

    /**
     * Подтверждение восстановления пароля.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        passwordService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Пароль успешно сброшен.");
    }
}