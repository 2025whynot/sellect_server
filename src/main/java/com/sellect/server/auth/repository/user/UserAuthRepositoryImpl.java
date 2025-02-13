package com.sellect.server.auth.repository.user;


import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.auth.repository.entity.UserAuthEntity;
import com.sellect.server.auth.repository.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserAuthRepositoryImpl implements UserAuthRepository {

    private final UserAuthJpaRepository userAuthJpaRepository;

    @Override
    public UserAuth save(UserAuth userAuth) {
        User user = userAuth.getUser();
        UserEntity userEntity = UserEntity.from(user);
        UserAuthEntity userAuthEntity = UserAuthEntity.from(userEntity, userAuth);
        UserAuthEntity saved = userAuthJpaRepository.save(userAuthEntity);
        return saved.toModel();
    }

    @Override
    public UserAuth findByEmail(String email) {
        UserAuthEntity byEmail = userAuthJpaRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("존재하지 않는 정보"));
        return byEmail.toModel();
    }

    @Override
    public boolean existsByEmail(String email) {
        return userAuthJpaRepository.existsByEmailAndDeleteAtIsNull(email);
    }
}
