package com.bank.knowledge.application;

import com.bank.common.exception.BusinessException;
import com.bank.knowledge.client.RagIngestionClient;
import com.bank.knowledge.domain.*;
import com.bank.knowledge.dto.CreateDocumentRequest;
import com.bank.knowledge.dto.DocumentResponse;
import com.bank.knowledge.dto.PublishNewVersionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the knowledge document lifecycle (DRAFT → ACTIVE → RETIRED).
 *
 * <p>rag-service integration (gap filled): when a document transitions to
 * ACTIVE (via {@link #publish} or {@link #publishNewVersion}), its current
 * version content is fetched from DocumentVersionRepository and pushed to
 * rag-service's vector store via RagIngestionClient. When a document is
 * RETIRED its chunks are removed from the vector store so they no longer
 * appear in RAG search results.
 *
 * <p>Failure policy: rag-service ingestion failures are logged but do NOT
 * roll back the publish/retire transaction. The document status change
 * happens regardless — RAG availability is a search-quality concern, not
 * a knowledge-service correctness invariant. Operators can re-ingest
 * manually by calling rag-service's POST /api/v1/rag/documents directly.
 */
@Service
@SuppressWarnings("null")
public class KnowledgeDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final RagIngestionClient ragClient;

    public KnowledgeDocumentService(KnowledgeDocumentRepository documentRepository,
                                     DocumentVersionRepository versionRepository,
                                     RagIngestionClient ragClient) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.ragClient = ragClient;
    }

    @Transactional
    public DocumentResponse create(CreateDocumentRequest request) {
        String rolesCsv = request.accessRoles() == null ? "" : String.join(",", request.accessRoles());
        KnowledgeDocument document = new KnowledgeDocument(
                request.title(), request.documentType(), request.owner(), request.department(),
                request.classification(), rolesCsv, request.effectiveDate(), request.expirationDate());
        document = documentRepository.save(document);

        versionRepository.save(new DocumentVersion(
                document.getDocumentId(), 1, request.title(), request.content(),
                request.storageLocation() != null ? request.storageLocation() : "inline", request.createdBy()));

        return DocumentResponse.from(document);
    }

    /**
     * Publishes the document (DRAFT → ACTIVE) and ingests its current version
     * content into rag-service. Ingestion failure does not roll back publish.
     */
    @Transactional
    public DocumentResponse publish(String documentId) {
        KnowledgeDocument document = findOrThrow(documentId);
        document.publish();
        document = documentRepository.save(document);

        ingestCurrentVersion(document);

        return DocumentResponse.from(document);
    }

    /**
     * Retires the document (ACTIVE → RETIRED) and removes its chunks from
     * the rag-service vector store so they no longer surface in RAG queries.
     * Removal failure does not roll back retire.
     */
    @Transactional
    public DocumentResponse retire(String documentId) {
        KnowledgeDocument document = findOrThrow(documentId);
        document.retire();
        document = documentRepository.save(document);

        ragClient.delete(documentId);

        return DocumentResponse.from(document);
    }

    /**
     * Publishes a new version (bumps version counter, transitions to ACTIVE)
     * and re-ingests the full updated content into rag-service, replacing the
     * previous version's chunks.
     */
    @Transactional
    public DocumentResponse publishNewVersion(String documentId, PublishNewVersionRequest request) {
        KnowledgeDocument document = findOrThrow(documentId);
        int newVersionNumber = document.publishNewVersion(request.title());

        DocumentVersion version = new DocumentVersion(
                documentId, newVersionNumber, request.title(), request.content(),
                request.storageLocation() != null ? request.storageLocation() : "inline", request.createdBy());
        versionRepository.save(version);

        document = documentRepository.save(document);

        // Re-ingest: rag-service's ingest is upsert-by-documentId (it replaces
        // existing chunks for the same documentId), so no explicit delete first.
        ingestContent(document, version.getContent());

        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(String documentId) {
        return DocumentResponse.from(findOrThrow(documentId));
    }

    @Transactional(readOnly = true)
    public DocumentResponse.VersionResponse getCurrentContent(String documentId) {
        KnowledgeDocument document = findOrThrow(documentId);
        DocumentVersion version = versionRepository
                .findByDocumentIdAndVersionNumber(documentId, document.getCurrentVersion())
                .orElseThrow(() -> BusinessException.notFound("Current version content missing for " + documentId));
        return DocumentResponse.VersionResponse.from(version);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse.VersionResponse> getVersionHistory(String documentId) {
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(DocumentResponse.VersionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getByType(DocumentType type, Pageable pageable) {
        return documentRepository.findByDocumentType(type, pageable).map(DocumentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getByStatus(DocumentStatus status, Pageable pageable) {
        return documentRepository.findByStatus(status, pageable).map(DocumentResponse::from);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void ingestCurrentVersion(KnowledgeDocument document) {
        versionRepository
                .findByDocumentIdAndVersionNumber(document.getDocumentId(), document.getCurrentVersion())
                .ifPresent(v -> ingestContent(document, v.getContent()));
    }

    private void ingestContent(KnowledgeDocument document, String content) {
        // Runs AFTER the @Transactional method has already saved the document;
        // ingestion failure does not roll back that save.
        ragClient.ingest(
                document.getDocumentId(),
                document.getDocumentType().name(),
                document.getTitle(),
                content,
                document.getAccessRoles()
        );
    }

    private KnowledgeDocument findOrThrow(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.notFound("Document not found: " + documentId));
    }
}
