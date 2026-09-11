package com.gulvisha.backend.controller;

import com.gulvisha.backend.dto.QuoteRequest;
import com.gulvisha.backend.dto.QuoteRequestResponse;
import com.gulvisha.backend.service.QuoteRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/quote-requests")
public class QuoteRequestController {

    private final QuoteRequestService quoteRequestService;

    public QuoteRequestController(QuoteRequestService quoteRequestService) {
        this.quoteRequestService = quoteRequestService;
    }

        @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteRequestResponse create(@Valid @RequestBody QuoteRequest request,
                                       @RequestParam(name = "tenant", required = false) String tenant,
                                       @RequestParam(name = "slug", required = false) String slug) {
        return quoteRequestService.submit(request, firstNonBlank(tenant, slug));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}