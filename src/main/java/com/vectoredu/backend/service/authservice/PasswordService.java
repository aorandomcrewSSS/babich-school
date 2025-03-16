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

        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByUser(user)
                .orElse(null);
        // Удаляем старый токен (если существует)

        if (passwordResetToken != null) {
            passwordResetTokenRepository.delete(passwordResetToken);
            passwordResetTokenRepository.flush();
        }

        // Создаем токен подтверждения смены пароля
        createAndSendPasswordResetToken(user, newPassword, "users/confirm-password-change");
    }

    /**
     * Запрос на восстановление пароля неаутентифицированным пользователем.
     */
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь с таким email не найден"));


        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByUser(user)
                .orElse(null);
        // Удаляем старый токен (если существует)

        if (passwordResetToken != null) {
            passwordResetTokenRepository.delete(passwordResetToken);
            passwordResetTokenRepository.flush();

        }

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

        sendEmailChangedNotification(user);
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

        sendEmailChangedNotification(user);
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
        String htmlMessage = generatePasswordResetContent(resetLink);

        sendEmail(user, subject, htmlMessage);
    }

    private void sendEmailChangedNotification(User user) {
        String subject = "Пароль успешно изменен";
        String message = "Ваш пароль был успешно изменен.";

        sendEmail(user, subject, message);
    }

    private String generatePasswordResetContent(String resetLink) {
        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Восстановление пароля</h2>"
                + "<p style=\"font-size: 16px;\">Для восстановления пароля перейдите по следующей ссылке:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Ссылка для восстановления пароля</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + resetLink + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
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



