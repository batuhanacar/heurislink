package com.batuhan.heurislink.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String REAL_IP_HEADER = "X-Real-IP";

    public String resolve(HttpServletRequest request) {
        String realIp = request.getHeader(REAL_IP_HEADER);

        if (realIp == null || realIp.isBlank()) {
            return request.getRemoteAddr();
        }

        return realIp.trim();
    }
}