package com.sellect.server.auth.repository.user;


import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.domain.UserAuth;
import com.sellect.server.auth.repository.entity.UserAuthEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserAuthRepositoryImpl implements UserAuthRepository {

    private final UserAuthJpaRepository userAuthJpaRepository;

    @Override
    public UserAuth save(UserAuth userAuth) {
        User user = userAuth.getUser();
        UserAuthEntity userAuthEntity = UserAuthEntity.from(user, userAuth);
        UserAuthEntity saved = userAuthJpaRepository.save(userAuthEntity);
        return saved.toModel();
    }

    @Override
    public Optional<UserAuth> findByEmail(String email) {
        UserAuthEntity userAuthEntity = userAuthJpaRepository.findByEmail(email)
            // todo: User not found exception
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return Optional.of(userAuthEntity.toModel());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userAuthJpaRepository.existsByEmailAndDeleteAtIsNull(email);
    }
}
