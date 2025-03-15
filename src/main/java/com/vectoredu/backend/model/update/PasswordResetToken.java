package com.vectoredu.backend.model.update;

import com.vectoredu.backend.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "Password_reset")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String newPassword; // Добавляем поле для нового пароля
    private String token;
    private LocalDateTime expiresAt;

    public PasswordResetToken(User user, String newPassword, String token, LocalDateTime expiresAt) {
        this.user = user;
        this.newPassword = newPassword;
        this.token = token;
        this.expiresAt = expiresAt;
    }
}

