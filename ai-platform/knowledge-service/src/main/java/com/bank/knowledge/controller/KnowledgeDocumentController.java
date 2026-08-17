package com.bank.knowledge.controller;

import com.bank.knowledge.application.KnowledgeDocumentService;
import com.bank.knowledge.domain.DocumentStatus;
import com.bank.knowledge.domain.DocumentType;
import com.bank.knowledge.dto.CreateDocumentRequest;
import com.bank.knowledge.dto.DocumentResponse;
import com.bank.knowledge.dto.PublishNewVersionRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.create(request));
    }

    @GetMapping("/{documentId}")
    public DocumentResponse get(@PathVariable String documentId) {
        return documentService.getDocument(documentId);
    }

    @GetMapping("/{documentId}/content")
    public DocumentResponse.VersionResponse getCurrentContent(@PathVariable String documentId) {
        return documentService.getCurrentContent(documentId);
    }

    @GetMapping("/{documentId}/versions")
    public List<DocumentResponse.VersionResponse> getVersionHistory(@PathVariable String documentId) {
        return documentService.getVersionHistory(documentId);
    }

    @PostMapping("/{documentId}/versions")
    public DocumentResponse publishNewVersion(@PathVariable String documentId,
                                               @Valid @RequestBody PublishNewVersionRequest request) {
        return documentService.publishNewVersion(documentId, request);
    }

    @PostMapping("/{documentId}/publish")
    public DocumentResponse publish(@PathVariable String documentId) {
        return documentService.publish(documentId);
    }

    @PostMapping("/{documentId}/retire")
    public DocumentResponse retire(@PathVariable String documentId) {
        return documentService.retire(documentId);
    }

    @GetMapping(params = "type")
    public Page<DocumentResponse> getByType(@RequestParam DocumentType type, Pageable pageable) {
        return documentService.getByType(type, pageable);
    }

    @GetMapping(params = "status")
    public Page<DocumentResponse> getByStatus(@RequestParam DocumentStatus status, Pageable pageable) {
        return documentService.getByStatus(status, pageable);
    }
}
