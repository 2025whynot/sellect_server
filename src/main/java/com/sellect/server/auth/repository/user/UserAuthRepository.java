package com.sellect.server.auth.repository.user;

import com.sellect.server.auth.domain.UserAuth;
import java.util.Optional;

public interface UserAuthRepository {
    UserAuth save(UserAuth userAuth);

    Optional<UserAuth> findByEmail(String email);
    boolean existsByEmail(String email);
}
