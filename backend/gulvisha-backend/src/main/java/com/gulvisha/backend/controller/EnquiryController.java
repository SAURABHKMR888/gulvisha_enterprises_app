package com.gulvisha.backend.controller;

import com.gulvisha.backend.dto.EnquiryRequest;
import com.gulvisha.backend.dto.QuoteRequestResponse;
import com.gulvisha.backend.service.QuoteRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private final QuoteRequestService quoteRequestService;

    public EnquiryController(QuoteRequestService quoteRequestService) {
        this.quoteRequestService = quoteRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteRequestResponse create(@Valid @RequestBody EnquiryRequest request) {
        return quoteRequestService.submitEnquiry(request);
    }
}