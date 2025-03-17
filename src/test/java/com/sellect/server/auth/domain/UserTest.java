package com.sellect.server.auth.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.sellect.server.auth.repository.entity.Role;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

    @Nested
    @DisplayName("checkRole() 메서드는")
    class CheckRole{

        @Test
        @DisplayName("Role.User인 경우 성공적으로 통과한한ㄷ. ")
        void checkRollWhenRoleIsUser() {
            //given
            User user = User.builder()
                .id(1L)
//                .uuid("uuid")
                .nickname("nickname")
                .role(Role.USER)
                .build();
            //when
            user.checkRole(Role.USER);
        }

        @Test
        @DisplayName("Role.Seller인 경우 성공적으로 통과한한ㄷ. ")
        void checkRollWhenRoleIsSeller() {
            //given
            User user = User.builder()
                .id(1L)
//                .uuid("uuid")
                .nickname("nickname")
                .role(Role.SELLER)
                .build();
            //when
            user.checkRole(Role.SELLER);
        }
    }

    @Test
    @DisplayName("user일때 checkRole(Role.SELLER)를 호출하면 CommonException이 발생한다.")
    void test3() {
        User user = User.builder()
            .id(1L)
//            .uuid("uuid")
            .nickname("nickname")
            .role(Role.USER)
            .build();
        //when
        CommonException commonException = assertThrows(CommonException.class,
            () -> user.checkRole(Role.SELLER));

        //then
        assertEquals(BError.NOT_SELLER.getCode(), commonException.getCode());
    }

    @Test
    @DisplayName("seller일때 checkRole(Role.User)를 호출하면 CommonException이 발생한다.")
    void test4() {
        User user = User.builder()
            .id(1L)
//            .uuid("uuid")
            .nickname("nickname")
            .role(Role.SELLER)
            .build();
        //when
        CommonException commonException = assertThrows(CommonException.class,
            () -> user.checkRole(Role.USER));

        //then
        assertEquals(BError.NOT_USER.getCode(), commonException.getCode());
    }
}