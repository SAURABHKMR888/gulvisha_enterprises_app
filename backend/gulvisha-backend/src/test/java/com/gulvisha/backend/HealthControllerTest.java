package com.gulvisha.backend;

import com.gulvisha.backend.controller.HealthController;
import com.gulvisha.backend.security.JwtService;
import com.gulvisha.backend.service.AuthService;
import com.gulvisha.backend.service.QuoteRequestService;
import com.gulvisha.backend.service.ServiceRepository;
import com.gulvisha.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuoteRequestService quoteRequestService;

    @MockitoBean
    private AuthService authService;

        @MockitoBean
    private JwtService jwtService;

        @MockitoBean
    private ServiceRepository serviceRepository;

    @MockitoBean
    private UserRepository userRepository;

        @MockitoBean
    private com.gulvisha.backend.organization.OrganizationRepository organizationRepository;

    @MockitoBean
    private com.gulvisha.backend.pipeline.PipelineStageRepository pipelineStageRepository;

    @MockitoBean
    private com.gulvisha.backend.customfield.CustomFieldDefinitionRepository customFieldRepository;

    @MockitoBean
    private com.gulvisha.backend.crm.LeadRepository leadRepository;

    @MockitoBean
    private com.gulvisha.backend.crm.ClientRepository clientRepository;

    @MockitoBean
    private com.gulvisha.backend.crm.LeadService leadService;

    @MockitoBean
    private com.gulvisha.backend.crm.ClientService clientService;

    @MockitoBean
    private com.gulvisha.backend.project.ProjectService projectService;

    @MockitoBean
    private com.gulvisha.backend.task.TaskService taskService;

    @MockitoBean
    private com.gulvisha.backend.resource.ResourceService resourceService;

    @MockitoBean
    private com.gulvisha.backend.portal.PortalService portalService;

    @MockitoBean
    private com.gulvisha.backend.workflow.WorkflowRepository workflowRepository;

    @MockitoBean
    private com.gulvisha.backend.workflow.WorkflowStepRepository workflowStepRepository;

    @MockitoBean
    private com.gulvisha.backend.workflow.WorkflowExecutionRepository workflowExecutionRepository;

    @MockitoBean
    private com.gulvisha.backend.workflow.WorkflowEngine workflowEngine;

    @MockitoBean
    private com.gulvisha.backend.user.UserService userService;

    // --- Phase 13/14 AI beans required by the @WebMvcTest slice ---

    @MockitoBean
    private com.gulvisha.backend.agent.AgentService agentService;

    @MockitoBean
    private com.gulvisha.backend.agent.AgentToolRegistry agentToolRegistry;

    @MockitoBean
    private com.gulvisha.backend.agent.guardrail.AgentGuardrailService agentGuardrailService;

    @MockitoBean
    private com.gulvisha.backend.agent.guardrail.AgentApprovalService agentApprovalService;

    @MockitoBean
    private com.gulvisha.backend.agent.guardrail.AgentResumeService agentResumeService;

    @MockitoBean
    private com.gulvisha.backend.agent.guardrail.OrchestrationResumeBridge orchestrationResumeBridge;

    @MockitoBean
    private com.gulvisha.backend.agent.orchestration.OrchestrationService orchestrationService;

    // --- Phase 12 AI beans required by the @WebMvcTest slice ---

    @MockitoBean
    private com.gulvisha.backend.ai.service.AiService aiService;

    @MockitoBean
    private com.gulvisha.backend.ai.service.RagService ragService;

    @Test
    void returnsApplicationHealth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Gulvisha Enterprises"));
    }
}
