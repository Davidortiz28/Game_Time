package com.gametime.backend.security;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

final class LoginThrottleFilter extends OncePerRequestFilter {

    private final LoginAttemptTracker attempts;

    LoginThrottleFilter(LoginAttemptTracker attempts) {
        this.attempts = attempts;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        boolean loginSubmission = "POST".equals(request.getMethod())
                && request.getServletPath().equals("/login");
        long remainingDelay = attempts.remainingDelayMillis();

        if (loginSubmission && remainingDelay > 0) {
            long retryAfterSeconds = Math.max(1, (remainingDelay + 999) / 1_000);
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            response.sendRedirect(request.getContextPath() + "/login?throttled");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
