package com.sellect.server.auth.repository.user;

import com.sellect.server.auth.domain.UserAuth;

public interface UserAuthRepository {
    void save(UserAuth userAuth);

    UserAuth findByEmail(String email);
}
