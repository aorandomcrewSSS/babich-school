package com.vectoredu.backend.controller;

import com.vectoredu.backend.dto.courseDto.request.BlockToCreate;
import com.vectoredu.backend.dto.courseDto.request.CourseToCreate;
import com.vectoredu.backend.dto.courseDto.request.LessonToCreate;
import com.vectoredu.backend.dto.courseDto.response.BlockResponse;
import com.vectoredu.backend.dto.courseDto.response.CourseResponse;
import com.vectoredu.backend.dto.courseDto.response.LessonResponse;
import com.vectoredu.backend.dto.customDto.customAdminDto.CustomAdminCoursesDto;
import com.vectoredu.backend.dto.customDto.customAdminDto.CustomBlockAdminDto;
import com.vectoredu.backend.dto.customDto.customAdminDto.CustomCourseAdminDto;
import com.vectoredu.backend.dto.customDto.customAdminDto.CustomUserAdminDto;
import com.vectoredu.backend.dto.customDto.customAdminDto.order.BlockOrderDto;
import com.vectoredu.backend.dto.customDto.customAdminDto.order.LessonOrderDto;
import com.vectoredu.backend.dto.customDto.customUserDto.CustomLessonDto;
import com.vectoredu.backend.model.enums.Difficulty;
import com.vectoredu.backend.model.enums.Status;
import com.vectoredu.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // === Назначение курса пользователю ===
    @PostMapping("/users/{email}/courses/{courseId}")
    public ResponseEntity<Void> assignCourseToUser(
            @PathVariable String email,
            @PathVariable Long courseId) {
        adminService.assignCourseToUser(email, courseId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // === Удаление курса у пользователя ===
    @DeleteMapping("/users/{email}/courses/{courseId}")
    public ResponseEntity<Void> removeCourseFromUser(
            @PathVariable String email,
            @PathVariable Long courseId) {
        adminService.removeCourseFromUser(email, courseId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // === КУРСЫ ===

    @PostMapping(value = "/courses")
    public ResponseEntity<CourseResponse> createCourse(
            @RequestParam String title,
            @RequestParam Integer price,
            @RequestParam Difficulty difficulty,
            @RequestParam(required = false) String chat
    ) {
        CourseToCreate courseToCreate = new CourseToCreate();
        courseToCreate.setTitle(title).setPrice(price).setDifficulty(difficulty).setChat(chat);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCourse(courseToCreate));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long courseId) {
        return ResponseEntity.ok(adminService.getCourseById(courseId));
    }

    @PutMapping(value = "/courses/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long courseId,
            @RequestParam(required = false) String newTitle,
            @RequestParam(required = false) Integer price,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String chat
    ) {
        return ResponseEntity.ok(adminService.updateCourse(courseId, newTitle,price, status, difficulty, chat));
    }

    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        adminService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    // === БЛОКИ ===

    @PostMapping(value = "/blocks")
    public ResponseEntity<BlockResponse> createBlock(
            @RequestParam Long courseId,
            @RequestParam String title) {
        BlockToCreate blockToCreate = new BlockToCreate();
        blockToCreate.setCourseId(courseId);
        blockToCreate.setTitle(title);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createBlock(blockToCreate));
    }

    @GetMapping("/blocks/{blockId}")
    public ResponseEntity<BlockResponse> getBlockById(@PathVariable Long blockId) {
        return ResponseEntity.ok(adminService.getBlockById(blockId));
    }

    @PutMapping(value = "/blocks/{blockId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BlockResponse> updateBlock(
            @PathVariable Long blockId,
            @RequestParam(required = false) String newTitle) {
        return ResponseEntity.ok(adminService.updateBlock(blockId, newTitle));
    }

    @DeleteMapping("/blocks/{blockId}")
    public ResponseEntity<Void> deleteBlock(@PathVariable Long blockId) {
        adminService.deleteBlock(blockId);
        return ResponseEntity.noContent().build();
    }

    // === УРОКИ ===

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<LessonResponse> getLessonById(@PathVariable Long lessonId) {
        return ResponseEntity.ok(adminService.getLessonById(lessonId));
    }

    @PostMapping(value = "/lessons")
    public ResponseEntity<LessonResponse> createLesson(
            @RequestParam Long blockId,
            @RequestParam String title,
            @RequestParam(required = false) String description
    ) {

        LessonToCreate lessonToCreate = new LessonToCreate()
                .setBlockId(blockId)
                .setTitle(title)
                .setDescription(description);

        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createLesson(lessonToCreate));
    }

    @PutMapping(value = "/lessons/{lessonId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable Long lessonId,
            @RequestParam(required = false) String newTitle,
            @RequestParam(required = false) String newDescription,
            @RequestParam(required = false) String newSheetUrl
    ) {

        return ResponseEntity.ok(adminService.updateLesson(lessonId, newTitle, newDescription, newSheetUrl));
    }

    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        adminService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/courses/{courseId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponse> uploadCourseImage(
            @PathVariable Long courseId,
            @RequestParam MultipartFile imageFile) {
        return ResponseEntity.ok(adminService.uploadCourseImage(courseId, imageFile));
    }

    @PostMapping(value = "/blocks/{blockId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BlockResponse> uploadBlockImage(
            @PathVariable Long blockId,
            @RequestParam MultipartFile imageFile) {
        return ResponseEntity.ok(adminService.uploadBlockImage(blockId, imageFile));
    }

    @PostMapping(value = "/lessons/{lessonId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponse> uploadLessonImage(
            @PathVariable Long lessonId,
            @RequestParam MultipartFile imageFile
    ){
        return ResponseEntity.ok(adminService.uploadLessonImage(lessonId, imageFile));
    }

    @PostMapping(value = "/lessons/{lessonId}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponse> uploadVideoFile(
            @PathVariable Long lessonId,
            @RequestParam MultipartFile videoFile) {
        return ResponseEntity.ok(adminService.uploadVideoFile(lessonId, videoFile));
    }

    @GetMapping(value = "/courses")
    public ResponseEntity<List<CustomAdminCoursesDto>> getAllCourses() {
        return ResponseEntity.ok(adminService.getCoursesWithUserCount());
    }

    @GetMapping(value = "/users")
    public ResponseEntity<List<CustomUserAdminDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getUsersWithCourseCount());
    }

    // === Получение деталей курса ===
    @GetMapping("/courses/{courseId}/details")
    public ResponseEntity<CustomCourseAdminDto> getCourseDetails(@PathVariable Long courseId) {
        return ResponseEntity.ok(adminService.getCourseDetails(courseId));
    }

    // === Получение деталей блока ===
    @GetMapping("/blocks/{blockId}/details")
    public ResponseEntity<CustomBlockAdminDto> getBlockDetails(@PathVariable Long blockId) {
        return ResponseEntity.ok(adminService.getBlockDetails(blockId));
    }

    // === Получение деталей урока ===
    @GetMapping("/lessons/{lessonId}/details")
    public ResponseEntity<CustomLessonDto> getLessonDetails(@PathVariable Long lessonId) {
        return ResponseEntity.ok(adminService.getLessonDetails(lessonId));
    }

    // Метод для изменения порядка блоков в курсе
    @PostMapping("/courses/{courseId}/reorder-blocks")
    public ResponseEntity<Void> reorderBlocks(
            @PathVariable Long courseId,
            @RequestBody List<BlockOrderDto> blockOrder) {

        adminService.reorderBlocks(courseId, blockOrder);
        return ResponseEntity.ok().build(); // Возвращаем успешный ответ
    }

    // Метод для изменения порядка уроков в блоке
    @PostMapping("/blocks/{blockId}/reorder-lessons")
    public ResponseEntity<Void> reorderLessons(
            @PathVariable Long blockId,
            @RequestBody List<LessonOrderDto> lessonOrder) {

        adminService.reorderLessons(blockId, lessonOrder);
        return ResponseEntity.ok().build(); // Возвращаем успешный ответ
    }

    @PutMapping("/{email}/role")
    public ResponseEntity<String> setUserRole(@PathVariable String email) {
        adminService.setUserRole(email);
        return ResponseEntity.ok("Роль ADMIN успешно назначена пользователю " + email);
    }
}

