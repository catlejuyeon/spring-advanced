package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        // given
        SignupRequest request = new SignupRequest("test@test.com", "Test1234", "USER");
        String encodedPassword = "encodedPassword";
        String bearerToken = "Bearer mockToken";

        User savedUser = new User("test@test.com", encodedPassword, UserRole.USER);

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn(encodedPassword);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtUtil.createToken(any(), anyString(), any(UserRole.class))).willReturn(bearerToken);

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertNotNull(response);
        assertEquals(bearerToken, response.getBearerToken());
        verify(userRepository).existsByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_EmailAlreadyExists() {
        // given
        SignupRequest request = new SignupRequest("duplicate@test.com", "Test1234", "USER");
        given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> authService.signup(request));
        assertEquals("이미 존재하는 이메일입니다.", exception.getMessage());

        // Early Return으로 인해 passwordEncoder.encode가 호출되지 않아야 함
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void signin_Success() {
        // given
        String email = "test@test.com";
        String password = "Test1234";
        String encodedPassword = "encodedPassword";
        String bearerToken = "Bearer mockToken";

        SigninRequest request = new SigninRequest(email, password);
        User user = new User(email, encodedPassword, UserRole.USER);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, encodedPassword)).willReturn(true);
        given(jwtUtil.createToken(any(), anyString(), any(UserRole.class))).willReturn(bearerToken);

        // when
        SigninResponse response = authService.signin(request);

        // then
        assertNotNull(response);
        assertEquals(bearerToken, response.getBearerToken());
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, encodedPassword);
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void signin_UserNotFound() {
        // given
        SigninRequest request = new SigninRequest("notexist@test.com", "Test1234");
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> authService.signin(request));
        assertEquals("가입되지 않은 유저입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void signin_WrongPassword() {
        // given
        String email = "test@test.com";
        String wrongPassword = "WrongPass";
        String encodedPassword = "encodedPassword";

        SigninRequest request = new SigninRequest(email, wrongPassword);
        User user = new User(email, encodedPassword, UserRole.USER);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(wrongPassword, encodedPassword)).willReturn(false);

        // when & then
        AuthException exception = assertThrows(AuthException.class,
                () -> authService.signin(request));
        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
    }
}