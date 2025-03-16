package com.vectoredu.backend.service.authservice;

import com.vectoredu.backend.model.update.EmailUpdateToken;
import com.vectoredu.backend.model.User;
import com.vectoredu.backend.repository.update.EmailUpdateTokenRepository;
import com.vectoredu.backend.repository.UserRepository;
import com.vectoredu.backend.util.exception.KnownUseCaseException;
import com.vectoredu.backend.util.exception.NotFoundException;
import com.vectoredu.backend.util.exception.ValidationException;
import com.vectoredu.backend.util.validators.EmailValidator;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmailUpdateService {
    private final UserRepository userRepository;
    private final EmailUpdateTokenRepository emailUpdateTokenRepository;
    private final EmailService emailService;
    private final EmailValidator emailValidator;

    public void requestEmailUpdate(String newEmail) {
        User user = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(()-> new NotFoundException("Пользователь не аутентифицирован"));

        validateNewEmail(newEmail);

        // Удаляем старый токен, если он есть
        emailUpdateTokenRepository.deleteByUser(user);

        // Создаем новый токен
        String token = UUID.randomUUID().toString();
        EmailUpdateToken emailUpdateToken = new EmailUpdateToken(user, newEmail, token, LocalDateTime.now().plusMinutes(15));
        emailUpdateTokenRepository.save(emailUpdateToken);

        // Отправляем письмо с подтверждением
        sendEmailUpdateConfirmation(newEmail, token);
    }

    public void confirmEmailUpdate(String token) {
        EmailUpdateToken emailUpdateToken = emailUpdateTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("Неверный или истекший токен"));

        if (emailUpdateToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Срок действия токена истек");
        }

        // Обновляем email пользователя
        User user = emailUpdateToken.getUser();
        user.setEmail(emailUpdateToken.getNewEmail());
        userRepository.save(user);

        // Удаляем использованный токен
        emailUpdateTokenRepository.delete(emailUpdateToken);

        // Уведомляем пользователя о смене email
        sendEmailChangedNotification(user);
    }

    private void validateNewEmail(String newEmail) {
        if (!emailValidator.isValid(newEmail, null)) {
            throw new ValidationException("Некорректный формат email");
        }

        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new KnownUseCaseException("Этот email уже используется");
        }
    }

    private void sendEmailUpdateConfirmation(String newEmail, String token) {
        String subject = "Подтверждение смены email";
        String confirmationLink = "https://localhost:8080/auth/confirm-email-update?token=" + token;
        String message = generateEmailUpdateContent(confirmationLink);

        try {
            emailService.sendVerificationEmail(newEmail, subject, message);
        } catch (MessagingException e) {
            log.error("Ошибка при отправке email", e);
        }
    }

    private void sendEmailChangedNotification(User user) {
        String subject = "Email успешно изменен";
        String message = "Ваш email был изменен на: " + user.getEmail();

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, message);
        } catch (MessagingException e) {
            log.error("Ошибка при отправке уведомления", e);
        }
    }

    private String generateEmailUpdateContent(String confirmationLink) {
        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Подтверждение смены email</h2>"
                + "<p style=\"font-size: 16px;\">Для завершения смены email, перейдите по следующей ссылке:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Ссылка для подтверждения email</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + confirmationLink + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new NotFoundException("Не удалось получить аутентифицированного пользователя");
    }
}

