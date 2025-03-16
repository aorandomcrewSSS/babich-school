package com.vectoredu.backend.controller;

import com.vectoredu.backend.dto.customDto.customUserDto.UserCourse;
import com.vectoredu.backend.dto.customDto.customUserDto.CustomBlockDto;
import com.vectoredu.backend.dto.customDto.customUserDto.CustomCourseDto;
import com.vectoredu.backend.dto.customDto.customUserDto.CustomLessonDto;
import com.vectoredu.backend.dto.authDto.response.UserResponse;
import com.vectoredu.backend.service.UserService;
import com.vectoredu.backend.service.authservice.EmailUpdateService;
import com.vectoredu.backend.service.authservice.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/users")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailUpdateService emailUpdateService;
    private final PasswordService passwordService;


    @GetMapping("/me")
    public ResponseEntity<UserResponse> getAuthenticatedUser() {
        return ResponseEntity.ok(userService.getAuthenticatedUser());
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<String> requestPasswordReset(@RequestParam String oldPassword,
                                                       @RequestParam String newPassword) {
        passwordService.requestPasswordChange(oldPassword,newPassword);
        return ResponseEntity.ok("Ссылка для сброса пароля отправлена на вашу почту");
    }

    @GetMapping("/confirm-reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token) {
        passwordService.confirmPasswordChange(token);
        return ResponseEntity.ok("Пароль успешно изменен");
    }

    @PostMapping("/request-email-update")
    public ResponseEntity<String> requestEmailUpdate(@RequestParam String newEmail) {
        emailUpdateService.requestEmailUpdate(newEmail);
        return ResponseEntity.ok("На новый email отправлено письмо с подтверждением");
    }

    @GetMapping("/confirm-email-update")
    public ResponseEntity<String> confirmEmailUpdate(@RequestParam String token) {
        emailUpdateService.confirmEmailUpdate(token);
        return ResponseEntity.ok("Email успешно обновлен");
    }
    @PostMapping("/update-user")
    public ResponseEntity<UserResponse> updateUser(
            @RequestParam String newFirstName,
            @RequestParam String newLastName
    ) {
        return ResponseEntity.ok(userService.updateUserInformation(newFirstName,newLastName));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadImage(@RequestParam("image") MultipartFile imageFile) {
        return ResponseEntity.ok(userService.uploadImage(imageFile));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<UserCourse>> getAllUserCourses() {
        return ResponseEntity.ok(userService.getAllUserCourses());
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<CustomCourseDto> getCourseDetails(@PathVariable Long courseId) {
        return ResponseEntity.ok(userService.getCourseDetails(courseId));
    }

    @GetMapping("/blocks/{blockId}")
    public ResponseEntity<CustomBlockDto> getBlockDetails(@PathVariable Long blockId) {
        return ResponseEntity.ok(userService.getBlockDetails(blockId));
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<CustomLessonDto> getLessonDetails(@PathVariable Long lessonId) {
        return ResponseEntity.ok(userService.getLessonDetails(lessonId));
    }

    // Эндпоинт для старта урока
    @PostMapping("/lessons/{lessonId}/start")
    public ResponseEntity<Void> startLesson(@PathVariable Long lessonId) {
        userService.startLesson(lessonId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();  // Возвращаем статус 204 No Content
    }

    // Эндпоинт для завершения урока
    @PostMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<Void> completeLesson(@PathVariable Long lessonId) {
        userService.completeLesson(lessonId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();  // Возвращаем статус 204 No Content
    }
}
