package com.vectoredu.backend.service.authservice;

import com.vectoredu.backend.dto.authDto.request.LoginUserDto;
import com.vectoredu.backend.dto.authDto.request.RegisterUserDto;
import com.vectoredu.backend.dto.authDto.request.VerifyUserDto;
import com.vectoredu.backend.dto.authDto.response.LoginResponse;
import com.vectoredu.backend.model.enums.Role;
import com.vectoredu.backend.model.User;
import com.vectoredu.backend.repository.UserRepository;
import com.vectoredu.backend.util.exception.*;
import com.vectoredu.backend.util.validators.EmailValidator;
import com.vectoredu.backend.util.validators.PasswordValidator;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final EmailValidator emailValidator;
    private final PasswordValidator passwordValidator;

    // Регистрация пользователя
    public User signup(RegisterUserDto input) {
        validateSignupInput(input);
        checkUserExistence(input);
        User userToCreate = createUser(input);
        sendVerificationEmail(userToCreate);
        return saveUser(userToCreate);
    }

    // Аутентификация пользователя
    public LoginResponse authenticate(LoginUserDto input) {
        User user = findUserByEmail(input.getEmail().replace(" ", ""));
        checkUserEnabled(user);
        try {
            authenticateUser(input);
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Данные введены некорректно");
        }

        String jwtToken = generateJwtToken(user);
        String refreshToken = generateRefreshToken(user);

        return createLoginResponse(jwtToken, refreshToken);
    }

    // Обновление токена доступа
    public String refreshAccessToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        User user = findUserByEmail(email);
        validateRefreshToken(refreshToken, user);
        return jwtService.generateToken(user);
    }

    // Верификация пользователя
    public void verifyUser(VerifyUserDto input) {
        User user = findUserByEmail(input.getEmail());
        validateVerificationCode(user, input);
        enableUser(user);
    }

    // Повторная отправка кода подтверждения
    public void resendVerificationCode(String email) {
        User user = findUserByEmail(email);
        checkUserAlreadyVerified(user);
        updateUserVerificationCode(user);
        sendVerificationEmail(user);
    }

    @PostConstruct
    public void initAdmin() {
        String adminEmail = "admin@example.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .firstName("Максим")
                    .lastName("Бабич")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin1234")) // Обязательно сменить пароль!
                    .enabled(true)
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Администратор создан: {}", adminEmail);
        } else {
            log.info("Администратор уже существует.");
        }
    }

    // Валидация ввода при регистрации
    private void validateSignupInput(RegisterUserDto input) {
        if(input.getFirstName().isBlank()){
            throw new ValidationException("Поле с именем не может быть пустым");
        }

        if(input.getLastName().isBlank()){
            throw new ValidationException("Поле с фамилией не может быть пустым");
        }

        if (!emailValidator.isValid(input.getEmail(), null)) {
            throw new ValidationException("Не верный формат email");
        }
        if (!passwordValidator.isValid(input.getPassword(), null)) {
            throw new ValidationException("Пароль должен содержать хотя бы одну заглавную букву, одну цифру, быть не короче 8 и не длиннее 20 символов");
        }

        if (!isValidName(input.getFirstName())) {
            throw new ValidationException("Некорректное имя: должно содержать только русские буквы, начинаться с заглавной буквы и быть не длиннее 20 символов");
        }

        if (!isValidName(input.getLastName())) {
            throw new ValidationException("Некорректная фамилия: должна содержать только русские буквы, начинаться с заглавной буквы и быть не длиннее 20 символов");
        }
    }

    // Проверка существования пользователя
    private void checkUserExistence(RegisterUserDto input) {
        Optional<User> userOptional = userRepository.findByEmail(input.getEmail());
        userOptional.ifPresent(existingUser -> {
            if (!existingUser.isEnabled()) {
                // Удаление существующего не верифицированного пользователя
                userRepository.delete(existingUser);
                userRepository.flush();
            } else {
                throw new KnownUseCaseException("Пользователь с такой почтой уже зарегистрирован");
            }
        });
    }

    private void handleExistingUser(User existingUser, RegisterUserDto input) {
        if (existingUser.getEmail().equals(input.getEmail())) {
            throw new KnownUseCaseException("Пользователь с такой почтой уже зарегистрирован");
        }
    }

    private User createUser(RegisterUserDto input) {
        String encodedPassword = passwordEncoder.encode(input.getPassword());
        return User.builder()
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .email(input.getEmail())
                .password(encodedPassword)
                .verificationCode(generateVerificationCode())
                .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15))
                .enabled(false)
                .role(Role.USER)
                .build();
    }

    private User saveUser(User userToCreate) {
        return userRepository.save(userToCreate);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private void checkUserEnabled(User user) {
        if (!user.isEnabled()) {
            throw new VerificationException("Пользователь не верифицирован");
        }
    }

    private void authenticateUser(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.getEmail().replace(" ", ""), input.getPassword())
        );
    }

    private String generateJwtToken(User user) {
        return jwtService.generateToken(user);
    }

    private String generateRefreshToken(User user) {
        return jwtService.generateRefreshToken(user);
    }

    private LoginResponse createLoginResponse(String jwtToken, String refreshToken) {
        return new LoginResponse(jwtToken, jwtService.getExpirationTime(), refreshToken, jwtService.getRefreshExpirationTime());
    }

    private void validateRefreshToken(String refreshToken, User user) {
        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    private void validateVerificationCode(User user, VerifyUserDto input) {

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Время подтверждения истекло");
        }
        if (!user.getVerificationCode().equals(input.getVerificationCode())) {
            throw new ValidationException("Неверный код подтверждения");
        }

    }

    private boolean isValidName(String name) {
        return name != null && name.matches("^[А-ЯЁ][а-яё]{0,19}$");
    }

    private void enableUser(User user) {
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
    }

    private void checkUserAlreadyVerified(User user) {
        if (user.isEnabled()) {
            throw new KnownUseCaseException("Аккаунт уже подтвержден");
        }
    }

    private void updateUserVerificationCode(User user) {
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
    }

    private void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = "VERIFICATION CODE " + user.getVerificationCode();
        String htmlMessage = generateVerificationEmailContent(verificationCode);
        sendEmail(user, subject, htmlMessage);
    }

    private void sendEmail(User user, String subject, String htmlMessage) {
        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            log.error("Ошибка при отправке email", e);
        }
    }

    private String generateVerificationEmailContent(String verificationCode) {
        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    public String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
