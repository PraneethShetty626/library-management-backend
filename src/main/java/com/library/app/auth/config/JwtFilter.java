package com.library.app.auth.config;

import com.library.app.auth.service.AuthLibraryUserService;
import com.library.app.auth.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JWTService jwtService;

    @Autowired
    private ApplicationContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        logger.debug("🔹 Incoming request: {} {}", request.getMethod(), request.getRequestURI());

        // Step 1: Extract token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            logger.debug("✅ Bearer token found: {}", token);

            try {
                username = jwtService.extractUserName(token);
                logger.debug("✅ Username extracted from token: {}", username);
            } catch (Exception e) {
                logger.error("❌ Failed to extract username from token: {}", e.getMessage());
            }
        } else {
            logger.warn("⚠️ No Bearer token found in Authorization header");
        }

        // Step 2: Validate and authenticate
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthLibraryUserService userService = context.getBean(AuthLibraryUserService.class);
                UserDetails userDetails = userService.loadUserByUsername(username);
                logger.debug("✅ Loaded user details for: {}", username);

                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.info("✅ Token validated successfully for user: {}", username);
                } else {
                    logger.warn("❌ Token validation failed for user: {}", username);
                }

            } catch (Exception e) {
                logger.error("❌ Authentication error for user '{}': {}", username, e.getMessage(), e);
            }
        } else if (username == null) {
            logger.debug("🔸 Skipping authentication setup — username is null or already authenticated");
        }

        // Step 3: Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
