package com.vectoredu.backend.service.authservice;

import com.vectoredu.backend.model.update.PasswordResetToken;
import com.vectoredu.backend.model.User;
import com.vectoredu.backend.repository.update.PasswordResetTokenRepository;
import com.vectoredu.backend.repository.UserRepository;
import com.vectoredu.backend.util.exception.NotFoundException;
import com.vectoredu.backend.util.exception.ValidationException;
import com.vectoredu.backend.util.validators.PasswordValidator;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordService {
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordValidator passwordValidator;

    /**
     * Запрос на изменение пароля аутентифицированным пользователем.
     */
    public void requestPasswordChange(String oldPassword, String newPassword) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ValidationException("Неверный старый пароль");
        }
        validateNewPassword(newPassword);

        // Создаем токен подтверждения смены пароля
        createAndSendPasswordResetToken(user, newPassword, "users/confirm-password-change");
    }

    /**
     * Запрос на восстановление пароля неаутентифицированным пользователем.
     */
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь с таким email не найден"));

        // Создаем токен для сброса пароля
        createAndSendPasswordResetToken(user, null, "auth/reset-password");
    }

    /**
     * Подтверждение смены пароля по токену.
     */
    public void confirmPasswordChange(String token) {
        PasswordResetToken passwordResetToken = validatePasswordResetToken(token);
        User user = passwordResetToken.getUser();
        validateNewPassword(passwordResetToken.getNewPassword());

        updatePassword(user, passwordResetToken.getNewPassword());
        passwordResetTokenRepository.delete(passwordResetToken);

        sendPasswordChangedNotification(user);
    }

    /**
     * Подтверждение восстановления пароля.
     */
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken passwordResetToken = validatePasswordResetToken(token);
        User user = passwordResetToken.getUser();

        validateNewPassword(newPassword);
        updatePassword(user, newPassword);
        passwordResetTokenRepository.delete(passwordResetToken);

        sendPasswordChangedNotification(user);
    }

    private void createAndSendPasswordResetToken(User user, String newPassword, String endpoint) {
        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken(user, newPassword, token, LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(passwordResetToken);

        String resetLink = "https://localhost:8080/" + endpoint + "?token=" + token;
        sendPasswordResetEmail(user, resetLink);
    }

    private void validateNewPassword(String newPassword) {
        if (!passwordValidator.isValid(newPassword, null)) {
            throw new ValidationException("Пароль должен содержать хотя бы одну заглавную букву, одну цифру, быть не короче 8 и не длиннее 20 символов");
        }
    }

    private void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private PasswordResetToken validatePasswordResetToken(String token) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Неверный или истекший токен"));

        if (passwordResetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Срок действия токена истек");
        }

        return passwordResetToken;
    }

    private void sendPasswordResetEmail(User user, String resetLink) {
        String subject = "Восстановление пароля";
        String htmlMessage = "<p>Для восстановления пароля перейдите по ссылке:</p>"
                + "<p><a href=\"" + resetLink + "\">" + resetLink + "</a></p>";

        sendEmail(user, subject, htmlMessage);
    }

    private void sendPasswordChangedNotification(User user) {
        sendEmail(user, "Пароль успешно изменен", "Ваш пароль был успешно изменен.");
    }

    private void sendEmail(User user, String subject, String htmlMessage) {
        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            log.error("Ошибка при отправке email", e);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        }
        throw new NotFoundException("Не удалось получить аутентифицированного пользователя");
    }
}



