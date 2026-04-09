package com.thang.roombooking.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import com.thang.roombooking.common.exception.errorcode.BaseErrorCode;
import com.thang.roombooking.common.exception.TokenExpiredException;
import com.thang.roombooking.service.TokenBlacklistService;
import com.thang.roombooking.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        // 0. GUARD: a valid JWT must have exactly 2 dots (header.payload.signature).
        //    If it's malformed (e.g. a UUID refresh token sent by mistake), skip JWT
        //    processing entirely and pass the request through. Public endpoints (permitAll)
        //    will still be served; protected endpoints will be rejected by Spring Security
        //    because no Authentication was set in the SecurityContext.
        if (token.chars().filter(c -> c == '.').count() != 2) {
            log.warn("Authorization header contains a non-JWT token (missing part delimiters). Skipping JWT auth.");
            filterChain.doFilter(request, response);
            return;
        }

        // 1. CHECK BLACKLIST — only reached for structurally valid JWT strings
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.warn("Blacklisted token attempt detected.");
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, AuthErrorCode.TOKEN_INVALID);
            return;
        }

        // 2. EXTRACT USERNAME — parse & verify the JWT signature
        final String username;
        try {
            username = tokenService.extractUsername(token);
        } catch (TokenExpiredException ex) {
            log.warn("JWT token expired: {}", ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, AuthErrorCode.TOKEN_EXPIRED);
            return;
        } catch (Exception ex) {
            log.warn("JWT token is invalid: {}", ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, AuthErrorCode.TOKEN_INVALID);
            return;
        }

        // 3. SET AUTHENTICATION CONTEXT
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityUserDetails userDetails = (SecurityUserDetails) userDetailsService.loadUserByUsername(username);

            if (tokenService.isValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, BaseErrorCode errorCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResult<?> apiResult = ApiResult.error(errorCode);
        String jsonResponse = objectMapper.writeValueAsString(apiResult);
        response.getWriter().write(jsonResponse);
    }
}
