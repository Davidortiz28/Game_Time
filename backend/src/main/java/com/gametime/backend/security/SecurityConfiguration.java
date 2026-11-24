package com.gametime.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }

    @Bean
    DaoAuthenticationProvider adminAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider adminAuthenticationProvider,
            LoginAttemptTracker attempts) throws Exception {
        http
                .authenticationProvider(adminAuthenticationProvider)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/error", "/favicon.ico").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            attempts.recordSuccess();
                            LOGGER.info("GameTime administrator signed in");
                            response.sendRedirect(request.getContextPath() + "/");
                        })
                        .failureHandler((request, response, exception) -> {
                            attempts.recordFailure();
                            LOGGER.warn("Failed GameTime administrator sign-in from {}", request.getRemoteAddr());
                            response.sendRedirect(request.getContextPath() + "/login?error");
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("GAMETIME_ADMIN")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession()));

        http.addFilterBefore(new LoginThrottleFilter(attempts), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new AdminAbsoluteSessionTimeoutFilter(), SecurityContextHolderFilter.class);
        return http.build();
    }
}
