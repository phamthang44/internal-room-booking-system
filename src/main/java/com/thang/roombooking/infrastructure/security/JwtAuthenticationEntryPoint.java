package com.thang.roombooking.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResult<?> errorResponse = ApiResult.error(AuthErrorCode.UNAUTHENTICATED);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
