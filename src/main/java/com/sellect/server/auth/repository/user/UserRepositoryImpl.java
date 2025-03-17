package com.sellect.server.auth.repository.user;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.UserEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

//    @Override
//    public Optional<User> findByUuid(String uuid) {
//        Optional<UserEntity> userEntity = userJpaRepository.findByUuid(uuid);
//        return userEntity.map(UserEntity::toModel);
//    }

    @Override
    public User save(User user) {
        UserEntity savedUser = userJpaRepository.save(UserEntity.from(user));
        return savedUser.toModel();
    }

    @Override
    public Optional<User> findById(Long id) {
        Optional<UserEntity> userEntity = userJpaRepository.findById(id);
        return userEntity.map(UserEntity::toModel);
    }
}
