package com.gulvisha.backend;

import com.gulvisha.backend.config.SecurityConfig;
import com.gulvisha.backend.controller.AdminQuoteRequestController;
import com.gulvisha.backend.dto.AdminQuoteRequestResponse;
import com.gulvisha.backend.security.JwtService;
import com.gulvisha.backend.service.QuoteRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminQuoteRequestController.class)
@Import({SecurityConfig.class, JwtService.class})
@TestPropertySource(properties = {
        "ADMIN_USERS=admin:change-me:ADMIN,viewer:view-me:VIEWER",
        "app.jwt.secret=test-secret-key-for-tests-min-32-chars",
        "app.jwt.expiration=86400000"
})
class AdminQuoteRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private QuoteRequestService quoteRequestService;

    private String adminToken() {
        return jwtService.generateToken("admin", List.of("enquiry:view", "enquiry:update"));
    }

    private String viewerToken() {
        return jwtService.generateToken("viewer", List.of("enquiry:view"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/admin/quote-requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsEnquiriesForAnAuthenticatedAdmin() throws Exception {
        given(quoteRequestService.findAll(any())).willReturn(Page.empty());

        mockMvc.perform(get("/api/admin/quote-requests")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void allowsViewerToReadEnquiries() throws Exception {
        given(quoteRequestService.findAll(any())).willReturn(Page.empty());

        mockMvc.perform(get("/api/admin/quote-requests")
                        .header("Authorization", bearer(viewerToken())))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsViewerFromUpdatingEnquiries() throws Exception {
        UUID enquiryId = UUID.randomUUID();

        mockMvc.perform(patch("/api/admin/quote-requests/{id}/status", enquiryId)
                        .header("Authorization", bearer(viewerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusUpdateRequest("CLOSED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void filtersEnquiriesByStatusForAnAuthenticatedAdmin() throws Exception {
        given(quoteRequestService.findAll(any(), eq("NEW"))).willReturn(Page.empty());

        mockMvc.perform(get("/api/admin/quote-requests")
                        .param("status", "NEW")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void searchesEnquiriesForAnAuthenticatedAdmin() throws Exception {
        given(quoteRequestService.search(any(), eq("NEW"), eq("jane"))).willReturn(Page.empty());

        mockMvc.perform(get("/api/admin/quote-requests")
                        .param("status", "NEW")
                        .param("search", "jane")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
    }

            @Test
            void filtersEnquiriesByDateForAnAuthenticatedAdmin() throws Exception {
            given(quoteRequestService.filter(any(), eq("NEW"), eq("jane"),
                eq(LocalDate.of(2026, 9, 1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)),
                eq(LocalDate.of(2026, 9, 6).atStartOfDay().toInstant(java.time.ZoneOffset.UTC))))
                .willReturn(Page.empty());

            mockMvc.perform(get("/api/admin/quote-requests")
                    .param("status", "NEW")
                    .param("search", "jane")
                    .param("from", "2026-09-01")
                    .param("to", "2026-09-05")
                    .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
            }

    @Test
    void updatesAnEnquiryStatusForAnAuthenticatedAdmin() throws Exception {
        UUID enquiryId = UUID.randomUUID();
        given(quoteRequestService.updateStatus(enquiryId, "QUALIFIED"))
                .willReturn(new AdminQuoteRequestResponse(
                        enquiryId,
                        "Jane Doe",
                        "jane@example.com",
                        "Acme Co",
                        "AI & Automation",
                        "555-0100",
                        "AI & Automation",
                        "INR 50,000",
                        "4-6 weeks",
                        "WEBSITE",
                        "Email",
                        "Need a workflow assistant",
                        "QUALIFIED",
                        "Follow up next week",
                        Instant.now()
                ));

        mockMvc.perform(patch("/api/admin/quote-requests/{id}/status", enquiryId)
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusUpdateRequest("QUALIFIED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUALIFIED"));
    }

            @Test
            void updatesInternalNotesForAnAuthenticatedAdmin() throws Exception {
            UUID enquiryId = UUID.randomUUID();
            given(quoteRequestService.updateInternalNotes(enquiryId, "Call on Monday"))
                .willReturn(new AdminQuoteRequestResponse(
                    enquiryId,
                    "Jane Doe",
                    "jane@example.com",
                    "Acme Co",
                    "AI & Automation",
                    "555-0100",
                    "AI & Automation",
                    "INR 50,000",
                    "4-6 weeks",
                    "WEBSITE",
                    "Email",
                    "Need a workflow assistant",
                    "NEW",
                    "Call on Monday",
                    Instant.now()
                ));

            mockMvc.perform(patch("/api/admin/quote-requests/{id}/notes", enquiryId)
                    .header("Authorization", bearer(adminToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new NotesUpdateRequest("Call on Monday"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalNotes").value("Call on Monday"));
            }

            @Test
            void updatesMultipleEnquiryStatusesForAnAuthenticatedAdmin() throws Exception {
            UUID firstId = UUID.randomUUID();
            UUID secondId = UUID.randomUUID();
            given(quoteRequestService.updateStatuses(List.of(firstId, secondId), "CLOSED"))
                .willReturn(List.of());

            mockMvc.perform(patch("/api/admin/quote-requests/bulk-status")
                    .header("Authorization", bearer(adminToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new BulkStatusUpdateRequest(
                        List.of(firstId, secondId), "CLOSED"))))
                .andExpect(status().isOk());
            }

    private record StatusUpdateRequest(String status) {}
            private record NotesUpdateRequest(String internalNotes) {}
            private record BulkStatusUpdateRequest(List<UUID> ids, String status) {}
}