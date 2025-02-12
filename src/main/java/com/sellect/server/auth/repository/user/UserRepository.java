package com.sellect.server.auth.repository.user;

import com.sellect.server.auth.domain.User;

public interface UserRepository {
    User findByUuid(String uuid);
    User save(User user);

    User findById(Long id);
}
