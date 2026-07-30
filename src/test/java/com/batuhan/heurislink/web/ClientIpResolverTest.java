package com.batuhan.heurislink.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver clientIpResolver = new ClientIpResolver();

    @Test
    void shouldUseRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.24.0.10");
        request.addHeader("X-Real-IP", "203.0.113.25");

        String clientIp = clientIpResolver.resolve(request);

        assertThat(clientIp).isEqualTo("203.0.113.25");
    }

    @Test
    void shouldFallbackToRemoteAddressWhenHeaderIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String clientIp = clientIpResolver.resolve(request);

        assertThat(clientIp).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldFallbackToRemoteAddressWhenHeaderIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "   ");

        String clientIp = clientIpResolver.resolve(request);

        assertThat(clientIp).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldTrimRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", " 203.0.113.25 ");

        String clientIp = clientIpResolver.resolve(request);

        assertThat(clientIp).isEqualTo("203.0.113.25");
    }
}