package com.sellect.server.auth.repository.user;

import com.sellect.server.auth.repository.entity.UserAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserAuthJpaRepository extends JpaRepository<UserAuthEntity, Long> {
}
