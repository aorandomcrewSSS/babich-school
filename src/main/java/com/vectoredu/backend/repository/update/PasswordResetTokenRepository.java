package com.vectoredu.backend.repository.update;

import com.vectoredu.backend.model.update.PasswordResetToken;
import com.vectoredu.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByToken(String token);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
