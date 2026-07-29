package com.batuhan.heurislink.integration;

import com.batuhan.heurislink.messaging.UrlClickProducer;
import com.batuhan.heurislink.repository.ShortUrlRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
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

        org.junit.jupiter.api.Assertions.assertEquals(1, recordCount);
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
}