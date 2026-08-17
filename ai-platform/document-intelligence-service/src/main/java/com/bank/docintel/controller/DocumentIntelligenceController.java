package com.bank.docintel.controller;

import com.bank.docintel.application.DocumentIntelligenceService;
import com.bank.docintel.dto.ProcessDocumentRequest;
import com.bank.docintel.dto.ProcessedDocumentResponse;
import com.bank.docintel.dto.ReviewDecisionRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentIntelligenceController {

    private final DocumentIntelligenceService service;

    public DocumentIntelligenceController(DocumentIntelligenceService service) {
        this.service = service;
    }

    @PostMapping("/process")
    public ResponseEntity<ProcessedDocumentResponse> process(@Valid @RequestBody ProcessDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.process(request));
    }

    @GetMapping("/{id}")
    public ProcessedDocumentResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping("/review-queue")
    public Page<ProcessedDocumentResponse> getReviewQueue(Pageable pageable) {
        return service.getReviewQueue(pageable);
    }

    @PostMapping("/{id}/review")
    public ProcessedDocumentResponse review(@PathVariable String id, @Valid @RequestBody ReviewDecisionRequest request) {
        return service.submitReview(id, request.approved(), request.reviewedBy(), request.notes());
    }
}
