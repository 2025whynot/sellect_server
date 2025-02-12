package com.sellect.server.auth.repository.user;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public User findByUuid(String uuid) {
        // todo: Exception 처리 업데이트
        UserEntity byUuid = userJpaRepository.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(uuid));
        return User.builder().uuid(byUuid.getUuid()).build();
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = UserEntity.from(user);
        UserEntity savedUser = userJpaRepository.save(userEntity);
        return savedUser.toModel();
    }

    @Override
    public User findById(Long id) {
        UserEntity userEntity = userJpaRepository.findById(id).orElseThrow();
        return userEntity.toModel();
    }
}
