package com.gulvisha.backend;

import com.gulvisha.backend.controller.QuoteRequestController;
import com.gulvisha.backend.dto.QuoteRequestResponse;
import com.gulvisha.backend.security.JwtService;
import com.gulvisha.backend.service.AuthService;
import com.gulvisha.backend.service.QuoteRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuoteRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuoteRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuoteRequestService quoteRequestService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void acceptsAValidQuoteRequest() throws Exception {
        given(quoteRequestService.submit(any())).willReturn(new QuoteRequestResponse(
                UUID.fromString("3f62b7fd-7f6c-4cae-bbbb-a59d7e71bea5"),
                Instant.parse("2026-01-01T00:00:00Z"),
                "Thanks — your quote request has been received. We'll be in touch shortly."
        ));

        mockMvc.perform(post("/api/quote-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Asha Patel",
                                  "email": "asha@example.com",
                                  "company": "Northstar Ltd",
                                  "service": "AI & Automation",
                                  "details": "We need help automating our support triage."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("3f62b7fd-7f6c-4cae-bbbb-a59d7e71bea5"))
                .andExpect(jsonPath("$.message").value("Thanks — your quote request has been received. We'll be in touch shortly."));
    }

    @Test
    void rejectsAnIncompleteQuoteRequest() throws Exception {
        mockMvc.perform(post("/api/quote-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }
}
