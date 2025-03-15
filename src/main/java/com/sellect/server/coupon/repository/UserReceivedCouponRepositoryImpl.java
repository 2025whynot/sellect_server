package com.sellect.server.coupon.repository;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.entity.CouponEntity;
import com.sellect.server.coupon.repository.entity.UserReceivedCouponEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserReceivedCouponRepositoryImpl implements UserReceivedCouponRepository {

    private final UserReceivedCouponJpaRepository userReceivedCouponJpaRepository;

    public UserReceivedCouponEntity save(UserReceivedCoupon userReceivedCoupon) {
        return userReceivedCouponJpaRepository.save(
            UserReceivedCouponEntity.from(userReceivedCoupon));
    }

    @Override
    public List<UserReceivedCoupon> findByUser(User user, PageRequest pageRequest) {
        List<UserReceivedCouponEntity> receivedCouponEntityList = userReceivedCouponJpaRepository.findAllByUser(
            UserEntity.from(user), pageRequest);

        return receivedCouponEntityList.stream()
            .map(UserReceivedCouponEntity::toModel)
            .toList();
    }

    @Override
    public List<UserReceivedCoupon> findAllByUserAndIsUsed(User user, boolean isUsed) {
        List<UserReceivedCouponEntity> receivedCouponEntityList = userReceivedCouponJpaRepository.findAllByUserAndIsUsed(
            UserEntity.from(user), isUsed);

        return receivedCouponEntityList.stream()
            .map(UserReceivedCouponEntity::toModel)
            .toList();
    }

    @Override
    public List<UserReceivedCoupon> findByUserAndIsUsed(User user, PageRequest pageRequest,
        Boolean isUsed) {
        List<UserReceivedCouponEntity> receivedCouponEntityList = userReceivedCouponJpaRepository.findByUserAndIsUsed(
            UserEntity.from(user), pageRequest, isUsed);

        return receivedCouponEntityList.stream()
            .map(UserReceivedCouponEntity::toModel)
            .toList();
    }

    @Override
    public Optional<UserReceivedCoupon> findByUserAndCoupon(User user, Coupon coupon) {
        Optional<UserReceivedCouponEntity> receivedCouponEntity = userReceivedCouponJpaRepository.findByUserAndCoupon(
            UserEntity.from(user), CouponEntity.from(coupon));
        return receivedCouponEntity.map(UserReceivedCouponEntity::toModel);
    }

    @Override
    public Boolean existsByUserAndCoupon(User user, Coupon coupon) {
        return userReceivedCouponJpaRepository.existsByUserAndCoupon(UserEntity.from(user),
            CouponEntity.from(coupon));
    }

    @Override
    public Optional<UserReceivedCoupon> findById(Long id) {
        return userReceivedCouponJpaRepository.findById(id)
            .map(UserReceivedCouponEntity::toModel);
    }

    @Override
    public Optional<UserReceivedCoupon> findWithWriteLockById(Long id) {
        return userReceivedCouponJpaRepository.findWithWriteLockById(id)
            .map(UserReceivedCouponEntity::toModel);
    }

}
