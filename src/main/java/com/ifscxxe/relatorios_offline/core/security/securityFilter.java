package com.ifscxxe.relatorios_offline.core.security;

import com.ifscxxe.relatorios_offline.core.providers.JWTprovider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class securityFilter extends OncePerRequestFilter {

    private final JWTprovider jwtProvider;

    public securityFilter(JWTprovider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path != null && path.startsWith("/api/")) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtProvider.isTokenValid(token)) {
                    var authentication = jwtProvider.getAuthentication(token);
                    if (authentication instanceof org.springframework.security.authentication.AbstractAuthenticationToken authToken) {
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    }
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
