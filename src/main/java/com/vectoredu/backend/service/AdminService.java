package com.vectoredu.backend.service;

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
import com.vectoredu.backend.dto.customDto.customAdminDto.subquery.SubBlockAdmin;
import com.vectoredu.backend.dto.customDto.customAdminDto.subquery.SubLessonAdmin;
import com.vectoredu.backend.dto.customDto.customUserDto.CustomLessonDto;
import com.vectoredu.backend.model.*;
import com.vectoredu.backend.model.enums.Difficulty;
import com.vectoredu.backend.model.enums.Role;
import com.vectoredu.backend.model.enums.Status;
import com.vectoredu.backend.repository.*;
import com.vectoredu.backend.repository.progress.BlockProgressRepository;
import com.vectoredu.backend.repository.progress.LessonProgressRepository;
import com.vectoredu.backend.util.exception.NotFoundException;
import com.vectoredu.backend.util.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final CourseRepository courseRepository;
    private final BlockRepository blockRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final BlockProgressRepository blockProgressRepository;
    private final S3Service s3Service;


    public void setUserRole(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        user.setRole(Role.ADMIN);

        userRepository.save(user);
    }

    public List<CustomUserAdminDto> getUsersWithCourseCount() {
        return userRepository.findAll().stream()
                .filter(User::isEnabled) // Фильтрация только включенных пользователей
                .map(user -> new CustomUserAdminDto(
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getCourses().size()
                ))
                .collect(Collectors.toList());
    }

    public List<CustomAdminCoursesDto> getCoursesWithUserCount() {
        return courseRepository.findAll().stream()
                .map(course -> new CustomAdminCoursesDto(
                        course.getId(),
                        course.getTitle(),
                        course.getPrice(),
                        course.getStatus(),
                        course.getUsers().size(),
                        course.getImageUrl()
                ))
                .collect(Collectors.toList());
    }

    public CustomCourseAdminDto getCourseDetails(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Курс не найден"));

        // Загружаем блоки с уроками отдельно
        List<Block> blocksWithLessons = blockRepository.findBlocksWithLessonsByCourseId(courseId);

        List<SubBlockAdmin> blocks = blocksWithLessons.stream()
                .map(block -> new SubBlockAdmin(block.getId(),
                        block.getTitle(),
                        block.getImageUrl(),
                        block.getOrder()))
                .sorted(Comparator.comparingInt(SubBlockAdmin::getOrder))
                .toList();

        return CustomCourseAdminDto.builder()
                .courseTitle(course.getTitle())
                .blocks(blocks)
                .chat(course.getChat())
                .build();
    }

    public CustomBlockAdminDto getBlockDetails(Long blockId) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Блок не найден"));

        List<Lesson> blockLessons = block.getLessons();

        List<SubLessonAdmin> lessons = blockLessons.stream()
                .map(lesson -> new SubLessonAdmin(
                        lesson.getId(),
                        lesson.getTitle(),
                        lesson.getImageUrl(),
                        lesson.getOrder()
                ))
                .sorted(Comparator.comparingInt(SubLessonAdmin::getOrder))
                .toList();

        return CustomBlockAdminDto.builder()
                .blockTitle(block.getTitle())
                .lessons(lessons)
                .build();
    }

    public CustomLessonDto getLessonDetails(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));

        return CustomLessonDto.builder()
                .lessonTitle(lesson.getTitle())
                .videoUrl(lesson.getVideoUrl())
                .description(lesson.getDescription())
                .sheetUrl(lesson.getSheetUrl())
                .build();
    }

    public void assignCourseToUser(String email, Long courseId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Курс не найден"));

        // Проверяем, не назначен ли уже курс пользователю
        if (user.getCourses().contains(course)) {
            throw new ValidationException("Курс уже назначен этому пользователю");
        }

        user.getCourses().add(course);
        userRepository.save(user);
    }

    public void removeCourseFromUser(String email, Long courseId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Курс не найден"));

        // Проверяем, что курс назначен пользователю
        if (!user.getCourses().contains(course)) {
            throw new ValidationException("Курс не назначен этому пользователю");
        }

        user.getCourses().remove(course);
        userRepository.save(user);
    }

    // === КУРСЫ ===

    public CourseResponse createCourse(CourseToCreate courseToCreate) {
        validateCourseInput(courseToCreate);

        User user = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Course course = Course.builder()
                .title(courseToCreate.getTitle())
                .price(courseToCreate.getPrice())
                .difficulty(courseToCreate.getDifficulty())
                .status(Status.PENDING)
                .author(user)
                .chat(courseToCreate.getChat())
                .build();

        courseRepository.save(course);
        return mapToCourseResponse(course);
    }

    public CourseResponse updateCourse(Long courseId, String newTitle, Integer newPrice, Status newStatus, Difficulty newDifficulty, String newChat) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Курс не найден"));

        if (newTitle != null && !newTitle.isBlank()) {
            course.setTitle(newTitle);
        }

        if (newPrice != null && newPrice >= 0) {
            course.setPrice(newPrice);
        }

        if (newStatus != null) {
            course.setStatus(newStatus);
        }

        if (newDifficulty != null) {
            course.setDifficulty(newDifficulty);
        }

        if (newChat != null) {
            if (!newChat.startsWith("https://t.me/")) {
                throw new ValidationException("Ссылка на чат должна начинаться с 'https://t.me/'");
            }
            course.setChat(newChat);
        }

        courseRepository.save(course);
        return mapToCourseResponse(course);
    }

    public CourseResponse getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Курс не найден"));
        return mapToCourseResponse(course);
    }

    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Курс не найден"));

        // Удалить все записи в lesson_progress, связанные с уроками этого курса
        for (Block block : course.getBlocks()) {
            for (Lesson lesson : block.getLessons()) {
                lessonProgressRepository.deleteByLessonId(lesson.getId());
            }
        }

        for (Block block : course.getBlocks()) {
            blockProgressRepository.deleteByBlockId(block.getId());
        }

        for (User user : course.getUsers()) {
            user.getCourses().remove(course);
        }

        course.getBlocks().forEach(block ->
                block.getLessons().forEach(this::deleteLessonVideo)
        );

        course.getBlocks().forEach(this::deleteBlockImage);

        deleteCourseImage(course);

        courseRepository.delete(course);
    }

    // === БЛОКИ ===
    public BlockResponse createBlock(BlockToCreate blockToCreate) {
        validateBlockInput(blockToCreate);
        Course course = courseRepository.findById(blockToCreate.getCourseId())
                .orElseThrow(() -> new NotFoundException("Курс не найден"));

        // Находим максимальный порядок для блоков в курсе
        Integer maxOrder = blockRepository.findMaxOrderByCourseId(course.getId());

        // Если блоки уже есть, устанавливаем порядок на 1 больше максимального
        int order = (maxOrder == null) ? 0 : maxOrder + 1;

        Block block = Block.builder()
                .title(blockToCreate.getTitle())
                .course(course)
                .order(order)  // Устанавливаем порядок
                .build();

        blockRepository.save(block);
        return mapToBlockResponse(block);
    }

    public BlockResponse updateBlock(Long blockId, String newTitle) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Блок не найден"));

        if (newTitle != null && !newTitle.isBlank()) {
            block.setTitle(newTitle);
        }

        blockRepository.save(block);
        return mapToBlockResponse(block);
    }

    public BlockResponse getBlockById(Long blockId) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Блок не найден"));
        return mapToBlockResponse(block);
    }

    public void deleteBlock(Long blockId) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Блок не найден"));

        blockProgressRepository.deleteByBlockId(blockId);

        for (Lesson lesson : block.getLessons()) {
            lessonProgressRepository.deleteByLessonId(lesson.getId()); // Удаление прогресса
            lessonRepository.delete(lesson); // Затем удаление урока
        }

        block.getLessons().forEach(this::deleteLessonVideo);

        deleteBlockImage(block);

        blockRepository.delete(block);
    }

    public void reorderBlocks(Long courseId, List<BlockOrderDto> blockOrder) {
        List<Block> blocks = blockRepository.findByCourseId(courseId);

        // Обновляем порядок блоков
        for (BlockOrderDto dto : blockOrder) {
            blocks.stream()
                    .filter(block -> block.getId().equals(dto.getId()))
                    .findFirst()
                    .ifPresent(block -> block.setOrder(dto.getOrder()));
        }

        blockRepository.saveAll(blocks);
    }

    // === УРОКИ ===
    public LessonResponse getLessonById(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));
        return mapToLessonResponse(lesson);
    }

    public LessonResponse createLesson(LessonToCreate lessonToCreate) {
        validateLessonInput(lessonToCreate);


        Block block = blockRepository.findById(lessonToCreate.getBlockId())
                .orElseThrow(() -> new NotFoundException("Блок не найден"));

        // Находим максимальный порядок для уроков в блоке
        Integer maxOrder = lessonRepository.findMaxOrderByBlockId(block.getId());

        // Если уроки уже есть, устанавливаем порядок на 1 больше максимального
        int order = (maxOrder == null) ? 0 : maxOrder + 1;

        Lesson lesson = Lesson.builder()
                .title(lessonToCreate.getTitle())
                .description(lessonToCreate.getDescription())
                .block(block)
                .order(order)  // Устанавливаем порядок
                .build();

        lessonRepository.save(lesson);
        return mapToLessonResponse(lesson);
    }

    public LessonResponse updateLesson(Long lessonId, String newTitle, String newDescription, String newSheetUrl) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));

        if (newTitle != null && !newTitle.isBlank()) {
            lesson.setTitle(newTitle);
        }
        if (newDescription != null && !newDescription.isBlank()) {
            lesson.setDescription(newDescription);
        }

        if (newSheetUrl != null && !newSheetUrl.isBlank()) {
            lesson.setSheetUrl(newSheetUrl);
        }

        lessonRepository.save(lesson);
        return mapToLessonResponse(lesson);
    }

    public void deleteLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));

        // Удаление всех записей из lesson_progress, связанных с этим уроком
        lessonProgressRepository.deleteByLessonId(lessonId);

        // Удаление видео из S3
        deleteLessonVideo(lesson);

        lessonRepository.delete(lesson);
    }

    public void reorderLessons(Long blockId, List<LessonOrderDto> lessonOrder) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Блок не найден"));

        // Обновляем порядок уроков
        for (LessonOrderDto dto : lessonOrder) {
            block.getLessons().stream()
                    .filter(lesson -> lesson.getId().equals(dto.getId()))
                    .findFirst()
                    .ifPresent(lesson -> lesson.setOrder(dto.getOrder()));
        }

        lessonRepository.saveAll(block.getLessons());
    }

    // === МЕТОДЫ ДЛЯ ЗАГРУЗКИ ФАЙЛОВ ===

    public CourseResponse uploadCourseImage(Long courseId, MultipartFile imageFile) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Курс не найден"));

        if (imageFile != null && !imageFile.isEmpty()) {

            String imageFileName = generateFileName(course.getTitle(), imageFile.getOriginalFilename());
            try {
                s3Service.uploadImage(imageFileName, imageFile);
            } catch (IOException e) {
                throw new RuntimeException("Не удалось загрузить новое изображение курса на S3", e);
            }

            // Удаляем старое изображение, если оно есть
            if (course.getImageUrl() != null) {
                String oldImageFileName = extractFileNameFromUrl(course.getImageUrl());
                s3Service.deleteFile(oldImageFileName);
            }
            course.setImageUrl(s3Service.getFileUrl(imageFileName));
        }

        courseRepository.save(course);

        return mapToCourseResponse(course);
    }

    public BlockResponse uploadBlockImage(Long blockId, MultipartFile imageFile) {

        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Блок не найден"));

        if (imageFile != null && !imageFile.isEmpty()) {

            String imageFileName = generateFileName(block.getTitle(), imageFile.getOriginalFilename());
            try {
                s3Service.uploadImage(imageFileName, imageFile);
            } catch (IOException e) {
                throw new RuntimeException("Не удалось загрузить новое изображение курса на S3", e);
            }

            // Удаляем старое изображение, если оно есть
            if (block.getImageUrl() != null) {
                String oldImageFileName = extractFileNameFromUrl(block.getImageUrl());
                s3Service.deleteFile(oldImageFileName);
            }

            block.setImageUrl(s3Service.getFileUrl(imageFileName));
        }

        blockRepository.save(block);

        return mapToBlockResponse(block);
    }

    public LessonResponse uploadLessonImage(Long lessonId, MultipartFile imageFile) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));

        if (imageFile != null && !imageFile.isEmpty()) {

            String imageFileName = generateFileName(lesson.getTitle(), imageFile.getOriginalFilename());
            try {
                s3Service.uploadImage(imageFileName, imageFile);
            } catch (IOException e) {
                throw new RuntimeException("Не удалось загрузить новое изображение курса на S3", e);
            }

            // Удаляем старое изображение, если оно есть
            if (lesson.getImageUrl() != null) {
                String oldImageFileName = extractFileNameFromUrl(lesson.getImageUrl());
                s3Service.deleteFile(oldImageFileName);
            }

            lesson.setImageUrl(s3Service.getFileUrl(imageFileName));
        }

        lessonRepository.save(lesson);

        return mapToLessonResponse(lesson);
    }

    public LessonResponse uploadVideoFile(Long lessonId, MultipartFile videoFile) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));

        if (videoFile != null && !videoFile.isEmpty()) {

            String newFileName = generateFileName(lesson.getTitle(), videoFile.getOriginalFilename());
            try {
                s3Service.uploadVideo(newFileName, videoFile);
            } catch (IOException e) {
                throw new RuntimeException("Не удалось загрузить новое видео в S3", e);
            }

            String oldVideoUrl = lesson.getVideoUrl();
            if (oldVideoUrl != null) {
                String oldFileName = extractFileNameFromUrl(oldVideoUrl);
                s3Service.deleteFile(oldFileName);
            }

            lesson.setVideoUrl(s3Service.getFileUrl(newFileName));
        }

        lessonRepository.save(lesson);

        return mapToLessonResponse(lesson);
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private void validateCourseInput(CourseToCreate courseToCreate) {
        if (courseToCreate.getTitle() == null || courseToCreate.getTitle().isBlank()) {
            throw new ValidationException("Название курса не может быть пустым");
        }

        if (courseToCreate.getPrice() == null || courseToCreate.getPrice() < 0) {
            throw new ValidationException("Цена не введена или введена не корректно");
        }

        if (courseToCreate.getDifficulty() == null) {
            throw new ValidationException("Сложность курса не может быть пустой");
        }
    }

    private void validateBlockInput(BlockToCreate blockToCreate) {
        if (blockToCreate.getTitle() == null || blockToCreate.getTitle().isBlank()) {
            throw new ValidationException("Название блока не может быть пустым");
        }
    }

    private void validateLessonInput(LessonToCreate lessonToCreate) {
        if (lessonToCreate.getTitle() == null || lessonToCreate.getTitle().isBlank()) {
            throw new ValidationException("Название урока не может быть пустым");
        }
    }

    private String generateFileName(String title, String originalFileName) {
        return title.replaceAll("\\s+", "_") + "_" + originalFileName;
    }

    private String extractFileNameFromUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new NotFoundException("Не удалось получить аутентифицированного пользователя");
    }

    private void deleteLessonVideo(Lesson lesson) {
        if (lesson.getVideoUrl() != null) {
            String fileName = extractFileNameFromUrl(lesson.getVideoUrl());
            s3Service.deleteFile(fileName);
        }
    }

    private void deleteBlockImage(Block block) {
        if (block.getImageUrl() != null) {
            String fileName = extractFileNameFromUrl(block.getImageUrl());
            s3Service.deleteFile(fileName);
        }
    }

    private void deleteCourseImage(Course course) {
        if (course.getImageUrl() != null) {
            String fileName = extractFileNameFromUrl(course.getImageUrl());
            s3Service.deleteFile(fileName);
        }
    }

    private CourseResponse mapToCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .price(course.getPrice())
                .difficulty(course.getDifficulty())
                .status(course.getStatus())
                .imageUrl(course.getImageUrl())
                .chat(course.getChat())
                .build();
    }

    private BlockResponse mapToBlockResponse(Block block) {
        return BlockResponse.builder()
                .id(block.getId())
                .title(block.getTitle())
                .courseId(block.getCourse().getId())
                .imageUrl(block.getImageUrl())
                .order(block.getOrder())
                .build();
    }

    private LessonResponse mapToLessonResponse(Lesson lesson) {

        return LessonResponse.builder()
                .id(lesson.getId())
                .blockId(lesson.getBlock().getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .imageUrl(lesson.getImageUrl())
                .videoUrl(lesson.getVideoUrl())
                .sheetUrl(lesson.getSheetUrl())
                .order(lesson.getOrder())
                .build();
    }
}