package org.example.expert.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                            @NonNull HttpServletResponse response,
                            @NonNull Object handler) {
        String uri = request.getRequestURI();

        // 어드민 경로만 로깅
        if (uri.startsWith("/admin/")) {
            Long userId = (Long) request.getAttribute("userId");
            String email = (String) request.getAttribute("email");
            String requestTime = LocalDateTime.now().format(FORMATTER);

            log.info("✅ [ADMIN_ACCESS] userId={}, email={}, uri={}, time={}",
                    userId, email, uri, requestTime);
        }

        // JwtFilter에서 이미 권한 체크를 완료했으므로 항상 true 반환
        return true;
    }
}