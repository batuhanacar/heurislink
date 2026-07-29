package com.batuhan.heurislink.integration;

import com.batuhan.heurislink.messaging.UrlClickProducer;
import com.batuhan.heurislink.repository.ShortUrlRepository;
import com.batuhan.heurislink.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(
        properties = {
                "spring.cache.type=none",
                "spring.kafka.listener.auto-startup=false"
        }
)
@AutoConfigureMockMvc
class ShortUrlIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private UrlClickProducer urlClickProducer;

    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        shortUrlRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRedirectShortUrl() throws Exception {
        String responseBody = mockMvc.perform(
                        post("/urls")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "originalUrl": "https://www.google.com"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://www.google.com"))
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.expiresAt").value((Object) null))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = jsonMapper.readTree(responseBody);
        String shortCode = responseJson.get("shortCode").asText();

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://www.google.com"
                ));
    }

    @Test
    void shouldPersistCreatedShortUrl() throws Exception {
        mockMvc.perform(
                        post("/urls")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "originalUrl": "https://spring.io"
                                        }
                                        """)
                )
                .andExpect(status().isCreated());

        long recordCount = shortUrlRepository.count();

        assertEquals(1, recordCount);
    }

    @Test
    void shouldReturnNotFoundForUnknownShortCode() throws Exception {
        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL not found for code: unknown"));
    }

    @Test
    void shouldDeactivateAndActivateShortUrl() throws Exception {
        String responseBody = mockMvc.perform(
                        post("/urls")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "originalUrl": "https://example.com"
                                    }
                                    """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = jsonMapper.readTree(responseBody);
        String shortCode = responseJson.get("shortCode").asText();

        mockMvc.perform(
                        patch("/urls/" + shortCode + "/deactivate")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error").value("Gone"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL is inactive for code: " + shortCode));

        mockMvc.perform(
                        patch("/urls/" + shortCode + "/activate")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://example.com"
                ));
    }

    @Test
    void shouldUpdateShortUrl() throws Exception {
        String responseBody = mockMvc.perform(
                        post("/urls")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "originalUrl": "https://old-example.com"
                                    }
                                    """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = jsonMapper.readTree(responseBody);
        String shortCode = responseJson.get("shortCode").asText();

        mockMvc.perform(
                        patch("/urls/" + shortCode)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "originalUrl": "https://new-example.com",
                                      "expiresAt": "2030-01-01T10:00:00"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://new-example.com"))
                .andExpect(jsonPath("$.shortCode")
                        .value(shortCode))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2030-01-01T10:00:00"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://new-example.com"
                ));
    }

    @Test
    void shouldDeleteShortUrl() throws Exception {
        String responseBody = mockMvc.perform(
                        post("/urls")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "originalUrl": "https://example.com"
                                    }
                                    """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = jsonMapper.readTree(responseBody);
        String shortCode = responseJson.get("shortCode").asText();

        mockMvc.perform(delete("/urls/" + shortCode))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}