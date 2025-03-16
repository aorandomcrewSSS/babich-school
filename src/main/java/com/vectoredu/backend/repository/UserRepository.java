package com.vectoredu.backend.repository;

import com.vectoredu.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationCode(String verificationCode);


    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.courses WHERE u.email = :email")
    Optional<User> findByEmailWithCourses(@Param("email") String email);

}
