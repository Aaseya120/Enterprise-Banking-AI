package com.bank.knowledge;

import com.bank.knowledge.application.KnowledgeDocumentService;
import com.bank.knowledge.domain.DocumentClassification;
import com.bank.knowledge.domain.DocumentStatus;
import com.bank.knowledge.domain.DocumentType;
import com.bank.knowledge.dto.CreateDocumentRequest;
import com.bank.knowledge.dto.DocumentResponse;
import com.bank.knowledge.dto.PublishNewVersionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = KnowledgeServiceApplication.class)
class KnowledgeDocumentServiceTest {

    @Autowired
    private KnowledgeDocumentService documentService;

    @Test
    void creatingADocumentStartsAtVersionOneInDraft() {
        DocumentResponse response = documentService.create(new CreateDocumentRequest(
                "Personal Loan Policy", DocumentType.POLICY, "jane.doe", "RETAIL_BANKING",
                DocumentClassification.INTERNAL, List.of("BANK_STAFF", "ANALYST"),
                "Personal loans require a credit score of 650+", "s3://docs/loan-policy-v1", "jane.doe",
                null, null));

        assertThat(response.currentVersion()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(response.accessRoles()).containsExactlyInAnyOrder("BANK_STAFF", "ANALYST");
    }

    @Test
    void publishingANewVersionIncrementsVersionAndKeepsHistory() {
        DocumentResponse created = documentService.create(new CreateDocumentRequest(
                "KYC SOP", DocumentType.SOP, "compliance.lead", "COMPLIANCE",
                DocumentClassification.CONFIDENTIAL, List.of("COMPLIANCE_OFFICER"),
                "v1 content", null, "compliance.lead", null, null));
        documentService.publish(created.documentId());

        DocumentResponse updated = documentService.publishNewVersion(created.documentId(),
                new PublishNewVersionRequest("KYC SOP v2", "v2 content", null, "compliance.lead"));

        assertThat(updated.currentVersion()).isEqualTo(2);
        assertThat(updated.status()).isEqualTo(DocumentStatus.ACTIVE);

        var history = documentService.getVersionHistory(created.documentId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).versionNumber()).isEqualTo(2); // ordered desc

        var currentContent = documentService.getCurrentContent(created.documentId());
        assertThat(currentContent.content()).isEqualTo("v2 content");
    }

    @Test
    void retireChangesStatus() {
        DocumentResponse created = documentService.create(new CreateDocumentRequest(
                "Old FAQ", DocumentType.FAQ, "owner", "RETAIL_BANKING",
                DocumentClassification.PUBLIC, List.of(), "stale content", null, "owner", null, null));

        DocumentResponse retired = documentService.retire(created.documentId());
        assertThat(retired.status()).isEqualTo(DocumentStatus.RETIRED);
    }
}
