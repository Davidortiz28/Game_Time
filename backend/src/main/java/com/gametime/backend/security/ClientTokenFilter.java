package com.gametime.backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica al cliente del jugador con un token compartido. El token solo concede el rol CLIENT,
 * que permite acciones de jugador: nunca crea, inicia ni termina sesiones.
 */
final class ClientTokenFilter extends OncePerRequestFilter {

    static final String HEADER = "X-GameTime-Client-Token";

    private final byte[] expectedDigest;

    ClientTokenFilter(String configuredToken) {
        this.expectedDigest = configuredToken == null || configuredToken.isBlank()
                ? null
                : sha256(configuredToken.trim());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (expectedDigest != null && presented != null
                && MessageDigest.isEqual(expectedDigest, sha256(presented.trim()))) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new PreAuthenticatedAuthenticationToken(
                    "gametime-client", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))));
            SecurityContextHolder.setContext(context);
        }
        filterChain.doFilter(request, response);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
