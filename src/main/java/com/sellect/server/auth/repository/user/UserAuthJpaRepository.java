package com.sellect.server.auth.repository.user;

import com.sellect.server.auth.repository.entity.UserAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserAuthJpaRepository extends JpaRepository<UserAuthEntity, Long> {
    boolean existsByEmailAndDeleteAtIsNull(String email);

    Optional<UserAuthEntity> findByEmail(String email);
}
