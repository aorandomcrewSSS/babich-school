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

    public void requestPasswordChange(String oldPassword, String newPassword) {
        // Получаем текущего аутентифицированного пользователя
        User user = getCurrentUser();

        // Проверяем, что старый пароль верный
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ValidationException("Неверный старый пароль");
        }

        // Валидируем новый пароль
        validateNewPassword(newPassword);

        // Генерируем токен для подтверждения смены пароля
        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken(user, newPassword, token, LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(passwordResetToken);

        // Отправляем email с ссылкой для подтверждения смены пароля
        String resetLink = "https://localhost:8080/auth/confirm-password-change?token=" + token;
        sendPasswordChangeConfirmationEmail(user, resetLink);
    }

    public void confirmPasswordChange(String token) {
        // Проверяем токен
        PasswordResetToken passwordResetToken = validatePasswordResetToken(token);
        User user = passwordResetToken.getUser();

        // Валидируем новый пароль
        validateNewPassword(passwordResetToken.getNewPassword());

        // Обновляем пароль
        updatePassword(user, passwordResetToken.getNewPassword());
        passwordResetTokenRepository.delete(passwordResetToken);

        // Отправляем уведомление о успешной смене пароля
        sendPasswordChangedNotification(user);
    }

    private void validateNewPassword(String newPassword) {
        if (!passwordValidator.isValid(newPassword, null)) {
            throw new ValidationException("Пароль должен содержать хотя бы одну заглавную букву, одну цифру, быть не короче 8 и не длиннее 20 символов");
        }
    }

    private void updatePassword(User user, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
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

    private void sendPasswordChangeConfirmationEmail(User user, String resetLink) {
        String subject = "Подтверждение смены пароля";
        String htmlMessage = generatePasswordChangeConfirmationContent(resetLink);

        sendEmail(user, subject, htmlMessage);
    }

    private void sendPasswordChangedNotification(User user) {
        String subject = "Пароль успешно изменен";
        String message = "Ваш пароль был успешно изменен.";

        sendEmail(user, subject, message);
    }

    private void sendEmail(User user, String subject, String htmlMessage) {
        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            log.error("Ошибка при отправке email", e);
        }
    }

    private String generatePasswordChangeConfirmationContent(String resetLink) {
        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Подтверждение смены пароля</h2>"
                + "<p style=\"font-size: 16px;\">Для завершения смены пароля перейдите по следующей ссылке:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Ссылка для смены пароля</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + resetLink + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
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


