package org.example.expert.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminAccessLoggingAspect {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Around("execution(* org.example.expert.domain.comment.controller.CommentAdminController.deleteComment(..)) || " +
            "execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))")
    public Object logAdminAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 요청 정보 수집
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            log.warn("ServletRequestAttributes를 찾을 수 없습니다.");
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();

        Long userId = (Long) request.getAttribute("userId");
        String uri = request.getRequestURI();
        String requestTime = LocalDateTime.now().format(FORMATTER);

        // 2. RequestBody 로깅
        String requestBody = extractRequestBody(joinPoint.getArgs());

        log.info("📩 [ADMIN_API_REQUEST] userId={}, uri={}, time={}, requestBody={}",
                userId, uri, requestTime, requestBody);

        // 3. 메서드 실행
        long startTime = System.currentTimeMillis();
        Object result = null;

        try {
            result = joinPoint.proceed();

            // 4. ResponseBody 로깅
            String responseBody = serializeResponse(result);
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("📬 [ADMIN_API_RESPONSE] userId={}, uri={}, executionTime={}ms, responseBody={}",
                    userId, uri, executionTime, responseBody);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ [ADMIN_API_ERROR] userId={}, uri={}, executionTime={}ms, error={}",
                    userId, uri, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * RequestBody 추출
     */
    private String extractRequestBody(Object[] args) {
        try {
            for (Object arg : args) {
                // @RequestBody나 DTO 객체 찾기 (기본 타입 제외)
                if (arg != null && !isSimpleType(arg)) {
                    return objectMapper.writeValueAsString(arg);
                }
            }
        } catch (Exception e) {
            log.warn("RequestBody 요청 실패: {}", e.getMessage());
        }
        return "Not Available";
    }

    /**
     * ResponseBody 직렬화
     */
    private String serializeResponse(Object response) {
        try {
            if (response == null) {
                return "null";
            }
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("ResponseBody 응답 실패: {}", e.getMessage());
            return "Not Available";
        }
    }

    /**
     * 기본 타입 체크 (로깅 제외 대상)
     */
    private boolean isSimpleType(Object obj) {
        return obj instanceof String ||
                obj instanceof Number ||
                obj instanceof Boolean ||
                obj instanceof Character ||
                obj.getClass().isPrimitive();
    }
}