package com.gametime.backend.security;

import java.io.IOException;
import java.time.Duration;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

final class AdminAbsoluteSessionTimeoutFilter extends OncePerRequestFilter {

    private static final String AUTHENTICATED_AT = AdminAbsoluteSessionTimeoutFilter.class.getName() + ".authenticatedAt";
    private static final long MAX_SESSION_AGE_MILLIS = Duration.ofHours(8).toMillis();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (session != null && isGameTimeAdmin(authentication)) {
            Long authenticatedAt = (Long) session.getAttribute(AUTHENTICATED_AT);
            long now = System.currentTimeMillis();

            if (authenticatedAt == null) {
                session.setAttribute(AUTHENTICATED_AT, now);
            } else if (now - authenticatedAt >= MAX_SESSION_AGE_MILLIS) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/login?expired");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isGameTimeAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
