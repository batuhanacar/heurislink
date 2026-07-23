package com.batuhan.heurislink.controller;

import com.batuhan.heurislink.entity.ShortUrl;
import com.batuhan.heurislink.exception.GlobalExceptionHandler;
import com.batuhan.heurislink.exception.ShortUrlNotFoundException;
import com.batuhan.heurislink.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ShortUrlController.class,
        properties = "app.base-url=http://localhost:8080"
)
@Import(GlobalExceptionHandler.class)
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShortUrlService shortUrlService;

    @Test
    void shouldCreateShortUrl() throws Exception {
        ShortUrl shortUrl = new ShortUrl(
                "https://www.google.com",
                "abc123"
        );

        when(shortUrlService.createShortUrl("https://www.google.com"))
                .thenReturn(shortUrl);

        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://www.google.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://www.google.com"))
                .andExpect(jsonPath("$.shortCode")
                        .value("abc123"))
                .andExpect(jsonPath("$.shortUrl")
                        .value("http://localhost:8080/abc123"));
    }

    @Test
    void shouldRedirectToOriginalUrl() throws Exception {
        ShortUrl shortUrl = new ShortUrl(
                "https://www.google.com",
                "abc123"
        );

        when(shortUrlService.getByShortCode("abc123"))
                .thenReturn(shortUrl);

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://www.google.com"
                ));
    }

    @Test
    void shouldReturnBadRequestWhenUrlIsBlank() throws Exception {
        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("URL cannot be blank"));
    }

    @Test
    void shouldReturnBadRequestWhenUrlDoesNotContainProtocol()
            throws Exception {

        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "www.google.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("URL must start with http:// or https://"));
    }

    @Test
    void shouldReturnNotFoundWhenShortCodeDoesNotExist()
            throws Exception {

        when(shortUrlService.getByShortCode("unknown"))
                .thenThrow(new ShortUrlNotFoundException("unknown"));

        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL not found for code: unknown"));
    }
}