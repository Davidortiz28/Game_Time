package com.gametime.backend.security;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public AdminUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!"admin".equals(username)) {
            throw new UsernameNotFoundException("Administrator account not found");
        }

        try {
            return jdbcTemplate.queryForObject(
                    "SELECT username, password_hash FROM admin_account WHERE singleton_id = 1",
                    (resultSet, rowNumber) -> User.withUsername(resultSet.getString("username"))
                            .password(resultSet.getString("password_hash"))
                            .roles("ADMIN")
                            .build());
        } catch (EmptyResultDataAccessException exception) {
            throw new UsernameNotFoundException("Administrator account has not been configured", exception);
        }
    }
}
