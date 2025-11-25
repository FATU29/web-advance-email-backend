package com.hcmus.awad_email.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;

/**
 * Interceptor to log all incoming API requests with user information
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Store start time for duration calculation
        request.setAttribute(START_TIME_ATTRIBUTE, Instant.now());
        
        // Get user information from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = "anonymous";
        
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = authentication.getPrincipal().toString();
        }
        
        // Get request details
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? uri + "?" + queryString : uri;
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Log the incoming request
        log.info("📥 API Request | Method: {} | URL: {} | User: {} | IP: {} | UserAgent: {}", 
                method, fullUrl, userId, clientIp, userAgent);
        
        // Log request headers (optional - can be verbose)
        if (log.isDebugEnabled()) {
            logRequestHeaders(request);
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) {
        // This method is called after the controller method but before the view is rendered
        // Not used in REST APIs, but can be useful for MVC applications
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        // Calculate request duration
        Instant startTime = (Instant) request.getAttribute(START_TIME_ATTRIBUTE);
        Duration duration = Duration.between(startTime, Instant.now());
        
        // Get user information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = "anonymous";
        
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = authentication.getPrincipal().toString();
        }
        
        // Get request details
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        // Determine log level based on status code
        if (status >= 500) {
            log.error("📤 API Response | Method: {} | URL: {} | User: {} | Status: {} | Duration: {}ms", 
                    method, uri, userId, status, duration.toMillis());
        } else if (status >= 400) {
            log.warn("📤 API Response | Method: {} | URL: {} | User: {} | Status: {} | Duration: {}ms", 
                    method, uri, userId, status, duration.toMillis());
        } else {
            log.info("📤 API Response | Method: {} | URL: {} | User: {} | Status: {} | Duration: {}ms", 
                    method, uri, userId, status, duration.toMillis());
        }
        
        // Log exception if present
        if (ex != null) {
            log.error("❌ Exception occurred during request processing: {}", ex.getMessage(), ex);
        }
    }
    
    /**
     * Get the real client IP address, considering proxies
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // If multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    
    /**
     * Log all request headers (for debugging)
     */
    private void logRequestHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder("\n📋 Request Headers:\n");
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            
            // Mask sensitive headers
            if ("Authorization".equalsIgnoreCase(headerName) && headerValue != null) {
                headerValue = headerValue.substring(0, Math.min(20, headerValue.length())) + "...";
            }
            
            headers.append("  ").append(headerName).append(": ").append(headerValue).append("\n");
        }
        
        log.debug(headers.toString());
    }
}

