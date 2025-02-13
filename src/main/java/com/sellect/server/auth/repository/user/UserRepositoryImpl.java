package com.sellect.server.auth.repository.user;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByUuid(String uuid) {
        // todo: Exception 처리 업데이트
        Optional<UserEntity> userEntity = userJpaRepository.findByUuid(uuid);
        Optional<User> user = Optional.of(userEntity.get().toModel());
        return user;
    }

    @Override
    public User save(User user) {
        UserEntity savedUser = userJpaRepository.save(UserEntity.from(user));
        return savedUser.toModel();
    }

    @Override
    public Optional<User> findById(Long id) {
        // todo: exception
        UserEntity userEntity = userJpaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저입니다."));
        return Optional.of(userEntity.toModel());
    }
}
