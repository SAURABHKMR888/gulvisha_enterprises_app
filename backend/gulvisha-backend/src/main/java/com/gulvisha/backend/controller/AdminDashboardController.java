package com.gulvisha.backend.controller;

import com.gulvisha.backend.dto.AdminDashboardResponse;
import com.gulvisha.backend.service.QuoteRequestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final QuoteRequestService quoteRequestService;

    public AdminDashboardController(QuoteRequestService quoteRequestService) {
        this.quoteRequestService = quoteRequestService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return quoteRequestService.getDashboardSummary();
    }
}
