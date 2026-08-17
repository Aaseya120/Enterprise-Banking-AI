package com.bank.docintel.application;

import com.bank.common.exception.BusinessException;
import com.bank.docintel.domain.BankDocumentType;
import com.bank.docintel.domain.ProcessedDocument;
import com.bank.docintel.domain.ProcessedDocumentRepository;
import com.bank.docintel.domain.ReviewStatus;
import com.bank.docintel.dto.ProcessDocumentRequest;
import com.bank.docintel.dto.ProcessedDocumentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements the tail of the pipeline in plan section 22: Classification ->
 * Extraction -> (implicit) Validation, via confidence-based routing.
 * "Low-confidence results should be routed for human review" is a real
 * rule here, not a comment: any result below autoApproveThreshold is
 * created with status PENDING_REVIEW and excluded from being treated as
 * final until a human calls /review.
 */
@Service
public class DocumentIntelligenceService {

    private final DocumentClassifier classifier;
    private final Map<BankDocumentType, FieldExtractor> extractorsByType;
    private final ProcessedDocumentRepository repository;
    private final ObjectMapper objectMapper;
    private final double autoApproveThreshold;

    public DocumentIntelligenceService(DocumentClassifier classifier,
                                        List<FieldExtractor> extractors,
                                        ProcessedDocumentRepository repository,
                                        ObjectMapper objectMapper,
                                        @Value("${bank.docintel.auto-approve-threshold:0.75}") double autoApproveThreshold) {
        this.classifier = classifier;
        this.extractorsByType = extractors.stream()
                .collect(Collectors.toMap(FieldExtractor::supports, e -> e));
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.autoApproveThreshold = autoApproveThreshold;
    }

    @Transactional
    public ProcessedDocumentResponse process(ProcessDocumentRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            throw BusinessException.validation("text is required (this service accepts already-OCR'd text; see pom.xml scope note)");
        }

        BankDocumentType type = request.documentTypeHint() != null
                ? request.documentTypeHint() : classifier.classify(request.text());

        FieldExtractor extractor = extractorsByType.get(type);
        ExtractionResult result = extractor != null
                ? extractor.extract(request.text())
                : new ExtractionResult(Map.of(), 0.0);

        String fieldsJson = toJson(result.fields());
        ProcessedDocument document = new ProcessedDocument(
                request.sourceRef(), type, fieldsJson, result.confidence(), autoApproveThreshold);

        return ProcessedDocumentResponse.from(repository.save(document), result.fields());
    }

    @Transactional(readOnly = true)
    public ProcessedDocumentResponse get(String id) {
        ProcessedDocument document = findOrThrow(id);
        return ProcessedDocumentResponse.from(document, fromJson(document.getExtractedFieldsJson()));
    }

    @Transactional(readOnly = true)
    public Page<ProcessedDocumentResponse> getReviewQueue(Pageable pageable) {
        return repository.findByReviewStatus(ReviewStatus.PENDING_REVIEW, pageable)
                .map(d -> ProcessedDocumentResponse.from(d, fromJson(d.getExtractedFieldsJson())));
    }

    @Transactional
    public ProcessedDocumentResponse submitReview(String id, boolean approved, String reviewedBy, String notes) {
        ProcessedDocument document = findOrThrow(id);
        if (document.getReviewStatus() != ReviewStatus.PENDING_REVIEW) {
            throw BusinessException.ruleViolation("Document is not pending review: " + document.getReviewStatus());
        }
        document.review(approved, reviewedBy, notes);
        return ProcessedDocumentResponse.from(repository.save(document), fromJson(document.getExtractedFieldsJson()));
    }

    private ProcessedDocument findOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Processed document not found: " + id));
    }

    private String toJson(Map<String, String> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize extracted fields", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
