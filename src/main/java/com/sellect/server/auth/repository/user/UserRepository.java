package com.sellect.server.auth.repository.user;

import com.sellect.server.auth.domain.User;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByUuid(String uuid);

    Optional<User> findById(Long id);
}
