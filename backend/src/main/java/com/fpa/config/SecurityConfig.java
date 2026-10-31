package com.fpa.config;

import com.fpa.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/org-login.html",
                    "/org-dashboard.html",
                    "/admin-dashboard.html",
                    "/researcher-dashboard.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/public/dashboard").permitAll()
                .requestMatchers("/api/users/register").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/databases/mine/**").hasRole("DATA_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/databases/**")
                    .hasAnyRole("ADMIN", "DATA_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/databases/**")
                    .hasAnyRole("ADMIN", "DATA_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/databases/**")
                    .hasAnyRole("ADMIN", "DATA_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/databases/**")
                    .hasAnyRole("ADMIN", "DATA_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/analytics/contributions")
                    .hasRole("DATA_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/analytics/contributions/**")
                    .hasAnyRole("ADMIN", "DATA_OWNER")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/analytics/contributions/**")
                    .hasAnyRole("ADMIN", "DATA_OWNER")
                .requestMatchers("/api/analytics/results/runs/**")
                    .hasAnyRole("ADMIN", "RESEARCHER")
                .requestMatchers("/api/analytics/results/global")
                    .hasAnyRole("ADMIN", "RESEARCHER")
                .requestMatchers("/api/analytics/results/*")
                    .hasAnyRole("ADMIN", "DATA_OWNER", "RESEARCHER")
                .requestMatchers("/api/analytics/privacy-status/**").hasAnyRole("ADMIN", "DATA_OWNER")
                .requestMatchers("/api/datasets/**").hasAnyRole("ADMIN", "DATA_OWNER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, exception) ->
                    response.setStatus(HttpStatus.FORBIDDEN.value()))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}