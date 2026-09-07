package com.gulvisha.backend;

import com.gulvisha.backend.config.SecurityConfig;
import com.gulvisha.backend.controller.AdminDashboardController;
import com.gulvisha.backend.dto.AdminDashboardResponse;
import com.gulvisha.backend.security.JwtService;
import com.gulvisha.backend.service.QuoteRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@Import({SecurityConfig.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-for-tests-min-32-chars",
        "app.jwt.expiration=86400000"
})
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private QuoteRequestService quoteRequestService;

    @Test
    void rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsDashboardSummaryForAuthenticatedAdmin() throws Exception {
        given(quoteRequestService.getDashboardSummary())
                .willReturn(new AdminDashboardResponse(12, 4, 7, 3, 2, 1, 0));

        String token = jwtService.generateToken("admin", List.of("enquiry:view"));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnquiries").value(12))
                .andExpect(jsonPath("$.newEnquiries").value(4))
                .andExpect(jsonPath("$.qualifiedLeads").value(7))
                .andExpect(jsonPath("$.inProgressEnquiries").value(3))
                .andExpect(jsonPath("$.closedEnquiries").value(2))
                .andExpect(jsonPath("$.rejectedEnquiries").value(1))
                .andExpect(jsonPath("$.archivedEnquiries").value(0));
    }
}
