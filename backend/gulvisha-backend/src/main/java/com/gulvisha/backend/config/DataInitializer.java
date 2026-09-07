package com.gulvisha.backend.config;

import com.gulvisha.backend.crm.Client;
import com.gulvisha.backend.crm.ClientRepository;
import com.gulvisha.backend.crm.Lead;
import com.gulvisha.backend.crm.LeadRepository;
import com.gulvisha.backend.project.Project;
import com.gulvisha.backend.project.ProjectRepository;
import com.gulvisha.backend.resource.Resource;
import com.gulvisha.backend.resource.ResourceRepository;
import com.gulvisha.backend.task.Task;
import com.gulvisha.backend.task.TaskRepository;
import com.gulvisha.backend.customfield.CustomFieldDefinition;
import com.gulvisha.backend.customfield.CustomFieldDefinitionRepository;
import com.gulvisha.backend.organization.Organization;
import com.gulvisha.backend.organization.OrganizationRepository;
import com.gulvisha.backend.pipeline.PipelineStage;
import com.gulvisha.backend.pipeline.PipelineStageRepository;
import com.gulvisha.backend.security.Role;
import com.gulvisha.backend.service.Service;
import com.gulvisha.backend.service.ServiceRepository;
import com.gulvisha.backend.user.User;
import com.gulvisha.backend.user.UserRepository;
import com.gulvisha.backend.workflow.Workflow;
import com.gulvisha.backend.workflow.WorkflowRepository;
import com.gulvisha.backend.workflow.WorkflowStep;
import com.gulvisha.backend.workflow.WorkflowStepRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Configuration
public class DataInitializer {

        @Bean
    CommandLineRunner initData(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               ServiceRepository serviceRepository,
                               PipelineStageRepository pipelineStageRepository,
                               CustomFieldDefinitionRepository customFieldRepository,
                               LeadRepository leadRepository,
                               ClientRepository clientRepository,
                               ProjectRepository projectRepository,
                               TaskRepository taskRepository,
                               ResourceRepository resourceRepository,
                               WorkflowRepository workflowRepository,
                               WorkflowStepRepository workflowStepRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            Optional<Organization> existingOrg = organizationRepository.findAll().stream().findFirst();
            Organization org;
            if (existingOrg.isEmpty()) {
                org = new Organization("Gulvisha Enterprises");
                org.setIndustry("Business Services / Technology");
                org.setEmail("hello@gulvisha.com");
                org.setDescription("Technology, Outsourcing & AI Solutions for Growing Businesses");
                organizationRepository.save(org);
            } else {
                org = existingOrg.get();
            }

            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User(
                        org.getId(),
                        "admin",
                        "admin@gulvisha.com",
                        passwordEncoder.encode("change-me"),
                        "Platform Admin",
                        Role.PLATFORM_ADMIN
                );
                userRepository.save(admin);
            }

                        if (serviceRepository.findAllByOrganizationIdOrderByNameAsc(org.getId()).isEmpty()) {
                List<Service> services = List.of(
                        createService(org.getId(), "BPO & Outsourcing", "Business process outsourcing services", "Outsourcing"),
                        createService(org.getId(), "Back-Office Support", "Back-office operational support", "Outsourcing"),
                        createService(org.getId(), "Data Processing", "Data entry and processing services", "Outsourcing"),
                        createService(org.getId(), "Customer Support", "Customer support services", "Outsourcing"),
                        createService(org.getId(), "Virtual Assistance", "Virtual assistant services", "Outsourcing"),
                        createService(org.getId(), "Software Development", "Custom software development", "Technology"),
                        createService(org.getId(), "Web Development", "Web application development", "Technology"),
                        createService(org.getId(), "API Development", "REST API development", "Technology"),
                        createService(org.getId(), "AI Automation", "AI-powered business automation", "AI & Automation"),
                        createService(org.getId(), "AI Agent Development", "AI agent development services", "AI & Automation")
                );
                serviceRepository.saveAll(services);
            }

            // Seed pipeline stages
            if (pipelineStageRepository.findAllByOrganizationIdOrderBySortOrder(org.getId()).isEmpty()) {
                List<PipelineStage> stages = List.of(
                    new PipelineStage(org.getId(), "NEW", 1, true),
                    new PipelineStage(org.getId(), "CONTACTED", 2, false),
                    new PipelineStage(org.getId(), "QUALIFIED", 3, false),
                    new PipelineStage(org.getId(), "PROPOSAL_SENT", 4, false),
                    new PipelineStage(org.getId(), "NEGOTIATION", 5, false),
                    new PipelineStage(org.getId(), "WON", 6, false),
                    new PipelineStage(org.getId(), "LOST", 7, false)
                );
                pipelineStageRepository.saveAll(stages);
            }

            // Seed custom fields for enquiries
            if (customFieldRepository.findAllByOrganizationIdAndEntityTypeOrderByCreatedAt(org.getId(), "ENQUIRY").isEmpty()) {
                List<CustomFieldDefinition> fields = List.of(
                    new CustomFieldDefinition(org.getId(), "preferredTech", "ENQUIRY", "TEXT", null, false),
                    new CustomFieldDefinition(org.getId(), "companySize", "ENQUIRY", "SELECT", "1-10,11-50,51-200,200+", false),
                    new CustomFieldDefinition(org.getId(), "projectBudget", "ENQUIRY", "NUMBER", null, false)
                );
                customFieldRepository.saveAll(fields);
            }

            // Seed sample clients
            if (clientRepository.findAllByOrganizationIdOrderByName(org.getId()).isEmpty()) {
                Client sampleClient = new Client(
                        org.getId(),
                        "Acme Technologies",
                        "John",
                        "Doe",
                        "john.doe@acmetech.example",
                        "+1-555-0100",
                        "Software",
                        "100 Sample Street, Springfield"
                );
                sampleClient.setWebsite("https://acmetech.example");
                clientRepository.save(sampleClient);
            }

            // Seed sample leads
            if (leadRepository.findAllByOrganizationIdOrderByCreatedAtDesc(org.getId()).isEmpty()) {
                Lead lead1 = new Lead(
                        org.getId(), "Priya", "Sharma", "priya.sharma@example.com",
                        "+91-98765-43210", "Sharma Retail Pvt Ltd", "BPO & Outsourcing"
                );
                lead1.setSource("WEBSITE");
                lead1.setLeadScore(75);
                lead1.setStatus("NEW");
                lead1.setNotes("Interested in customer support outsourcing for 10 agents.");

                Lead lead2 = new Lead(
                        org.getId(), "Marcus", "Webb", "marcus.webb@example.com",
                        "+44-7700-900123", "Webb Consulting Ltd", "AI Automation"
                );
                lead2.setSource("LINKEDIN");
                lead2.setLeadScore(60);
                lead2.setStatus("CONTACTED");
                lead2.setNotes("Wants an AI chatbot for lead qualification.");

                leadRepository.saveAll(List.of(lead1, lead2));
            }

            // Seed sample project (linked to first client if available)
            if (projectRepository.findAllByOrganizationIdOrderByCreatedAtDesc(org.getId()).isEmpty()) {
                UUID clientId = clientRepository.findAllByOrganizationIdOrderByName(org.getId()).stream()
                        .findFirst()
                        .map(Client::getId)
                        .orElse(null);

                Project sampleProject = new Project(
                        org.getId(),
                        clientId,
                        "Customer Support Outsourcing Pilot",
                        "Pilot project for outsourced customer support operations.",
                        "BPO & Outsourcing"
                );
                sampleProject.setStatus("ACTIVE");
                sampleProject.setManager("admin");
                projectRepository.save(sampleProject);

                // Seed tasks for the project
                Task task1 = new Task(org.getId(), sampleProject.getId(),
                        "Prepare support SOPs", "Draft standard operating procedures for the support team.");
                task1.setPriority("HIGH");
                task1.setStatus("IN_PROGRESS");
                task1.setAssignedTo("Priya");

                Task task2 = new Task(org.getId(), sampleProject.getId(),
                        "Set up support email inbox", "Create and configure the shared support mailbox.");
                task2.setPriority("MEDIUM");
                task2.setStatus("TO_DO");
                taskRepository.saveAll(List.of(task1, task2));

                // Seed resources
                Resource resource1 = new Resource(org.getId(), "Ravi Kumar", "Customer Support Agent");
                resource1.setSkills("Email support, Chat support, CRM tools");
                resource1.setExperienceYears(3);
                resource1.setAvailability("Full-time");
                resource1.setStatus("ASSIGNED");
                resource1.setAssignedProjectId(sampleProject.getId());

                Resource resource2 = new Resource(org.getId(), "Anita Roy", "Data Entry Operator");
                resource2.setSkills("Data entry, Data validation, Excel");
                resource2.setExperienceYears(2);
                resource2.setAvailability("Part-time");
                resourceRepository.saveAll(List.of(resource1, resource2));
            }

            // Seed client portal user (CLIENT role, linked to first client)
            if (userRepository.findByUsername("client").isEmpty()) {
                clientRepository.findAllByOrganizationIdOrderByName(org.getId()).stream()
                        .findFirst()
                        .ifPresent(firstClient -> {
                            User portalUser = new User(
                                    org.getId(),
                                    "client",
                                    "client@acme.com",
                                    passwordEncoder.encode("client123"),
                                    "ACME Corp Contact",
                                    Role.CLIENT
                            );
                            portalUser.setClientId(firstClient.getId());
                            userRepository.save(portalUser);
                        });
            }

            // Seed default workflow: new enquiry -> create lead + add note
            if (workflowRepository.findAllByOrganizationId(org.getId()).isEmpty()) {
                Workflow workflow = new Workflow(org.getId(), "Enquiry follow-up");
                workflow.setDescription("Creates a lead and records a note for every new enquiry.");
                workflow.setTriggerType("ENQUIRY_CREATED");
                workflow.setEnabled(true);
                workflowRepository.save(workflow);
                workflowStepRepository.saveAll(List.of(
                        new WorkflowStep(org.getId(), workflow.getId(), "Create lead", "CREATE_LEAD", null, 1),
                        new WorkflowStep(org.getId(), workflow.getId(), "Add welcome note", "ADD_NOTE",
                                "Auto-created from new enquiry", 2)
                ));
            }
        };
    }

    private Service createService(UUID orgId, String name, String description, String category) {
        Service service = new Service(orgId, name);
        service.setDescription(description);
        service.setCategory(category);
        return service;
    }
}
