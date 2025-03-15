package com.vectoredu.backend.repository.update;

import com.vectoredu.backend.model.update.EmailUpdateToken;
import com.vectoredu.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailUpdateTokenRepository extends JpaRepository<EmailUpdateToken, Long> {
    Optional<EmailUpdateToken> findByToken(String token);
    void deleteByUser(User user);
}
