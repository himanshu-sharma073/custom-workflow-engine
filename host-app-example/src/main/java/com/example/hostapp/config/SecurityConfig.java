package com.example.hostapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public InMemoryUserDetailsManager users() {
        UserDetails manager = User.withUsername("manager1").password("{noop}password").roles("MANAGER").build();
        UserDetails analyst = User.withUsername("user123").password("{noop}password").roles("ANALYST").build();
        UserDetails committee = User.withUsername("committee1").password("{noop}password").roles("COMMITTEE_MEMBER").build();
        UserDetails chairman = User.withUsername("chairman1").password("{noop}password").roles("CHAIRMAN").build();
        UserDetails reviewer = User.withUsername("reviewer1").password("{noop}password").roles("REVIEWER").build();
        UserDetails legal = User.withUsername("legal1").password("{noop}password").roles("LEGAL").build();
        UserDetails admin = User.withUsername("admin1").password("{noop}password").roles("ADMIN").build();
        return new InMemoryUserDetailsManager(manager, analyst, committee, chairman, reviewer, legal, admin);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs",
                    "/v3/api-docs/**")
                .permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
