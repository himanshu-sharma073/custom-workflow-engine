package com.example.hostapp.config;

import com.example.workflow.auth.UserContextProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class HostUserContextConfig {

    @Bean
    public UserContextProvider userContextProvider() {
        return new UserContextProvider() {
            @Override
            public String getCurrentUser() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                return auth == null ? "anonymous" : auth.getName();
            }

            @Override
            public List<String> getUserRoles() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || auth.getAuthorities() == null) {
                    return List.of();
                }
                return auth.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", "")).toList();
            }

            @Override
            public List<String> getUserGroups() {
                String raw = System.getProperty("demo.user.groups", "OPS,FINANCE");
                return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            }
        };
    }
}
