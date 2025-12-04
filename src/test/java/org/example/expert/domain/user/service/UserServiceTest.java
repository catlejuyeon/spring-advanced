package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 조회 성공")
    void getUser_Success() {
        // given
        long userId = 1L;
        User user = new User("test@test.com", "encodedPassword", UserRole.USER);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserResponse result = userService.getUser(userId);

        // then
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("사용자 조회 실패 - 존재하지 않는 사용자")
    void getUser_UserNotFound() {
        // given
        long userId = 999L;
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.getUser(userId));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_Success() {
        // given
        long userId = 1L;
        String oldPassword = "OldPass123";
        String newPassword = "NewPass456";
        String encodedOldPassword = "encodedOldPassword";
        String encodedNewPassword = "encodedNewPassword";

        User user = new User("test@test.com", encodedOldPassword, UserRole.USER);
        UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, newPassword);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(oldPassword, encodedOldPassword)).willReturn(true);
        given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);

        // when
        userService.changePassword(userId, request);

        // then
        verify(passwordEncoder).matches(oldPassword, encodedOldPassword);
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 존재하지 않는 사용자")
    void changePassword_UserNotFound() {
        // given
        long userId = 999L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("OldPass123", "NewPass456");
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(userId, request));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 잘못된 기존 비밀번호")
    void changePassword_WrongOldPassword() {
        // given
        long userId = 1L;
        String oldPassword = "WrongPassword";
        String newPassword = "NewPass456";
        String encodedPassword = "encodedPassword";

        User user = new User("test@test.com", encodedPassword, UserRole.USER);
        UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, newPassword);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(oldPassword, encodedPassword)).willReturn(false);

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(userId, request));
        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호가 기존 비밀번호와 동일")
    void changePassword_SameAsOldPassword() {
        // given
        long userId = 1L;
        String password = "SamePass123";
        String encodedPassword = "encodedPassword";

        User user = new User("test@test.com", encodedPassword, UserRole.USER);
        UserChangePasswordRequest request = new UserChangePasswordRequest(password, password);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, encodedPassword)).willReturn(true);

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(userId, request));
        assertEquals("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.", exception.getMessage());
    }
}
