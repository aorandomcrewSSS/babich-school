package com.vectoredu.backend.model.update;

import com.vectoredu.backend.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailUpdateToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String newEmail;
    private String token;
    private LocalDateTime expiresAt;

    public EmailUpdateToken(User user, String newEmail, String token, LocalDateTime expiresAt) {
        this.user = user;
        this.newEmail = newEmail;
        this.token = token;
        this.expiresAt = expiresAt;
    }
}

