package com.vectoredu.backend.service;

import com.vectoredu.backend.dto.customDto.customUserDto.UserCourse;
import com.vectoredu.backend.dto.customDto.customUserDto.CustomBlockDto;
import com.vectoredu.backend.dto.customDto.customUserDto.CustomCourseDto;
import com.vectoredu.backend.dto.customDto.customUserDto.CustomLessonDto;
import com.vectoredu.backend.dto.customDto.customUserDto.subquery.SubBlock;
import com.vectoredu.backend.dto.customDto.customUserDto.subquery.SubLesson;
import com.vectoredu.backend.dto.authDto.response.UserResponse;
import com.vectoredu.backend.model.Block;
import com.vectoredu.backend.model.Course;
import com.vectoredu.backend.model.Lesson;
import com.vectoredu.backend.model.User;
import com.vectoredu.backend.model.enums.ProgressStatus;
import com.vectoredu.backend.model.enums.Role;
import com.vectoredu.backend.model.enums.Status;
import com.vectoredu.backend.model.progress.BlockProgress;
import com.vectoredu.backend.model.progress.LessonProgress;
import com.vectoredu.backend.repository.BlockRepository;
import com.vectoredu.backend.repository.CourseRepository;
import com.vectoredu.backend.repository.LessonRepository;
import com.vectoredu.backend.repository.UserRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final BlockRepository blockRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final BlockProgressRepository blockProgressRepository;
    private final S3Service s3Service;

    public UserResponse getAuthenticatedUser() {
        User user = getAuthenticatedUserEntity();

        return UserResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .build();
    }

    public List<UserCourse> getAllUserCourses() {
        User user = userRepository.findByEmailWithCourses(getCurrentUserEmail())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        return user.getCourses().stream()
                .filter(course -> course.getStatus() == Status.ACTIVE || user.getRole() != Role.USER)
                .map(course -> mapToCourseResponse(course, user))// исключение дубликатов
                .toList();
    }

    public UserResponse updateUserInformation(String newFirstName, String newLastName) {
        String email = getCurrentUserEmail(); // Получаем email аутентифицированного пользователя

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (isValidName(newFirstName)) {
            user.setFirstName(newFirstName);
        } else if (newFirstName != null && !newFirstName.isBlank()) {
            throw new ValidationException("Некорректное имя: должно содержать только русские буквы, начинаться с заглавной буквы и быть не длиннее 20 символов");
        }

        if (isValidName(newLastName)) {
            user.setLastName(newLastName);
        } else if (newLastName != null && !newLastName.isBlank()) {
            throw new ValidationException("Некорректная фамилия: должна содержать только русские буквы, начинаться с заглавной буквы и быть не длиннее 20 символов");
        }

        userRepository.save(user);

        return UserResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .build();
    }

    public UserResponse uploadImage(MultipartFile imageFile) {
        User user = getAuthenticatedUserEntity();

        if (imageFile != null && !imageFile.isEmpty()) {
            if (user.getImageUrl() != null) {
                String oldImageFileName = extractFileNameFromUrl(user.getImageUrl());
                s3Service.deleteFile(oldImageFileName);
            }
            String imageFileName = generateFileName(user.getEmail(), imageFile.getOriginalFilename());
            try {
                s3Service.uploadImage(imageFileName, imageFile);
            } catch (IOException e) {
                throw new RuntimeException("Не удалось загрузить новое изображение курса на S3", e);
            }
            user.setImageUrl(s3Service.getFileUrl(imageFileName));
        }

        userRepository.save(user);

        return UserResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .build();
    }

    public CustomCourseDto getCourseDetails(Long courseId) {
        User user = userRepository.findByEmailWithCourses(getCurrentUserEmail())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Course course = courseRepository.findByIdWithBlocks(courseId)
                .orElseThrow(() -> new NotFoundException("Курс не найден или не принадлежит пользователю"));

        // Загружаем блоки с уроками отдельно
        List<Block> blocksWithLessons = blockRepository.findBlocksWithLessonsByCourseId(courseId);

        // Получаем завершенные уроки пользователя
        Set<Long> completedLessons = lessonProgressRepository.findByUserAndStatus(user, ProgressStatus.COMPLETED)
                .stream()
                .map(lp -> lp.getLesson().getId())
                .collect(Collectors.toSet());

        List<SubBlock> blocks = blocksWithLessons.stream()
                .map(block -> {
                    int completedLessonsCount = (int) block.getLessons().stream()
                            .filter(lesson -> completedLessons.contains(lesson.getId()))
                            .count();
                    return new SubBlock(block.getId(),
                            block.getTitle(),
                            block.getImageUrl(),
                            completedLessonsCount,
                            block.getLessons().size(),
                            block.getOrder());
                })
                .sorted(Comparator.comparingInt(SubBlock::getOrder))
                .toList();

        return CustomCourseDto.builder()
                .courseTitle(course.getTitle())
                .blocks(blocks)
                .chat(course.getChat())
                .build();
    }


    public CustomBlockDto getBlockDetails(Long blockId) {
        User user = userRepository.findByEmailWithCourses(getCurrentUserEmail())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Блок не найден"));

        boolean hasAccess = user.getCourses().stream()
                .anyMatch(course -> course.getBlocks().contains(block));

        if (!hasAccess) {
            throw new NotFoundException("Блок не найден или не принадлежит пользователю");
        }

        List<Lesson> blockLessons = block.getLessons();

        // Получаем все статусы прогресса разом
        Map<Long, ProgressStatus> lessonProgressMap = lessonProgressRepository.findByUserAndLessonIn(user, blockLessons)
                .stream()
                .collect(Collectors.toMap(
                        lp -> lp.getLesson().getId(),
                        LessonProgress::getStatus,
                        (existing, replacement) -> replacement // Оставляем последний статус
                ));

        List<SubLesson> lessons = blockLessons.stream()
                .map(lesson -> new SubLesson(
                        lesson.getId(),
                        lesson.getTitle(),
                        block.getCourse().getImageUrl(),
                        lessonProgressMap.get(lesson.getId()),// Если записи нет, вернется null
                        lesson.getOrder()
                ))
                .sorted(Comparator.comparingInt(SubLesson::getOrder))
                .toList();

        return CustomBlockDto.builder()
                .blockTitle(block.getTitle())
                .lessons(lessons)
                .build();
    }

    public CustomLessonDto getLessonDetails(Long lessonId) {
        User user = userRepository.findByEmailWithCourses(getCurrentUserEmail())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));

        boolean hasAccess = user.getCourses().stream()
                .flatMap(course -> course.getBlocks().stream())
                .anyMatch(block -> block.getLessons().contains(lesson));

        if (!hasAccess) {
            throw new NotFoundException("Урок не найден или не принадлежит пользователю");
        }

        return CustomLessonDto.builder()
                .lessonTitle(lesson.getTitle())
                .videoUrl(lesson.getVideoUrl())
                .description(lesson.getDescription())
                .sheetUrl(lesson.getSheetUrl())
                .build();
    }

    // Метод для старта урока
    public void startLesson(Long lessonId) {
        User user = getAuthenticatedUserEntity();

        // Получаем урок по ID
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));

        // Проверяем, существует ли уже прогресс для этого урока
        LessonProgress existingProgress = lessonProgressRepository.findByLessonAndUserWithLock(lesson, user)
                .orElse(null);

        if (existingProgress == null) {
            // Если прогресса нет, создаем новый
            LessonProgress newLessonProgress = new LessonProgress(lesson, user, ProgressStatus.IN_PROGRESS);
            lessonProgressRepository.save(newLessonProgress);
        } else {
            // Если прогресс уже существует, проверяем, что он не завершен
            if (existingProgress.getStatus() != ProgressStatus.COMPLETED) {
                existingProgress.setStatus(ProgressStatus.IN_PROGRESS);
                lessonProgressRepository.save(existingProgress);
            }
            // Если прогресс уже COMPLETED, не меняем его статус
        }

        // Создаем или обновляем прогресс блока
        updateBlockProgress(lesson.getBlock(), user);
    }


    // Метод для завершения урока
    public void completeLesson(Long lessonId) {
        User user = getAuthenticatedUserEntity();

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));

        LessonProgress lessonProgress = lessonProgressRepository.findByLessonAndUser(lesson, user)
                .orElseThrow(() -> new NotFoundException("Прогресс для этого урока не найден"));

        lessonProgress.setStatus(ProgressStatus.COMPLETED);

        lessonProgressRepository.save(lessonProgress);

        // После завершения урока, обновляем статус блока
        updateBlockProgress(lesson.getBlock(), user);
    }

    // Метод для обновления статуса блока
    private void updateBlockProgress(Block block, User user) {
        // Получаем все уроки блока
        List<Lesson> lessons = block.getLessons();

        // Запрашиваем прогресс всех уроков блока одним запросом
        List<LessonProgress> lessonProgressList = lessonProgressRepository.findByLessonInAndUser(lessons, user);

        // Создаем мапу для быстрого доступа к статусу прогресса по уроку
        Map<Lesson, ProgressStatus> progressMap = lessonProgressList.stream()
                .collect(Collectors.toMap(LessonProgress::getLesson, LessonProgress::getStatus));

        // Подсчитываем количество завершенных уроков
        long completedLessonsCount = lessons.stream()
                .filter(lesson -> progressMap.getOrDefault(lesson, ProgressStatus.IN_PROGRESS) == ProgressStatus.COMPLETED)
                .count();

        boolean isBlockCompleted = completedLessonsCount == lessons.size();

        // Проверяем, есть ли уже прогресс для блока
        BlockProgress blockProgress = blockProgressRepository.findByBlockAndUser(block, user)
                .orElse(null);

        if (blockProgress == null) {
            blockProgress = new BlockProgress(block, user, ProgressStatus.IN_PROGRESS);
            blockProgressRepository.save(blockProgress);
        } else {
            blockProgress.setStatus(isBlockCompleted ? ProgressStatus.COMPLETED : ProgressStatus.IN_PROGRESS);
            blockProgressRepository.save(blockProgress);
        }
    }


    private String generateFileName(String title, String originalFileName) {
        return title.replaceAll("\\s+", "_") + "_" + originalFileName;
    }

    private String extractFileNameFromUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    private User getAuthenticatedUserEntity() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new NotFoundException("Не удалось получить аутентифицированного пользователя");
    }

    private boolean isValidName(String name) {
        return name != null && name.matches("^[А-ЯЁ][а-яё]{0,19}$");
    }

    private UserCourse mapToCourseResponse(Course course, User user) {
        // Получаем все блоки для курса с их прогрессом пользователя за один запрос
        List<Block> blocks = blockRepository.findBlocksWithLessonsByCourseId(course.getId());

        // Получаем прогресс пользователя по всем блокам курса за один запрос
        List<BlockProgress> blockProgressList = blockProgressRepository.findProgressByUserAndBlocks(user, blocks);

        // Рассчитываем количество завершенных блоков
        int completedBlocksCount = (int) blocks.stream()
                .filter(block -> blockProgressList.stream()
                        .anyMatch(bp -> bp.getBlock().getId().equals(block.getId()) &&
                                bp.getStatus() == ProgressStatus.COMPLETED))
                .count();

        int totalBlocksCount = blocks.size();  // Общее количество блоков

        return UserCourse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .imageUrl(course.getImageUrl())
                .completedBlocks(completedBlocksCount)  // Количество пройденных блоков
                .blocksCount(totalBlocksCount)  // Общее количество блоков
                .build();
    }

}