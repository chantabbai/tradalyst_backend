package com.tradepro.security;

import com.tradepro.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final UserService userService;

    public JwtAuthenticationFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            logger.debug("Auth header: {}", authHeader);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                if (userService.validateToken(token)) {
                    String userEmail = userService.getEmailFromToken(token);
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userEmail, null, null);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Successfully authenticated user: {}", userEmail);
                    
                    // Handle token refresh if needed
                    if (userService.shouldTokenBeRefreshed(token)) {
                        String newToken = userService.refreshToken(userEmail);
                        response.setHeader("Authorization", "Bearer " + newToken);
                        response.setHeader("Access-Control-Expose-Headers", "Authorization");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
} 