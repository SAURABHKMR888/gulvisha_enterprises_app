package com.gulvisha.backend.dto;

public record AdminDashboardResponse(
        long totalEnquiries,
        long newEnquiries,
        long qualifiedLeads,
        long inProgressEnquiries,
        long closedEnquiries,
        long rejectedEnquiries,
        long archivedEnquiries
) {
}
