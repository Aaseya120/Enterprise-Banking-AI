package com.bank.docintel;

import com.bank.docintel.application.DocumentIntelligenceService;
import com.bank.docintel.domain.BankDocumentType;
import com.bank.docintel.domain.ReviewStatus;
import com.bank.docintel.dto.ProcessDocumentRequest;
import com.bank.docintel.dto.ProcessedDocumentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DocumentIntelligenceServiceApplication.class)
class DocumentIntelligenceServiceTest {

    @Autowired
    private DocumentIntelligenceService service;

    private static final String KYC_TEXT = """
            PASSPORT - IDENTITY VERIFICATION
            Name: Jane Doe
            Document Number: P1234567
            Date of Birth: 1990-05-14
            Expiry Date: 2030-05-14
            """;

    @Test
    void classifiesAndExtractsAWellFormedKycDocumentWithHighConfidence() {
        ProcessedDocumentResponse response = service.process(
                new ProcessDocumentRequest("s3://uploads/passport-1.pdf", KYC_TEXT, null));

        assertThat(response.documentType()).isEqualTo(BankDocumentType.KYC);
        assertThat(response.confidence()).isEqualTo(1.0);
        assertThat(response.reviewStatus()).isEqualTo(ReviewStatus.AUTO_APPROVED);
        assertThat(response.extractedFields()).containsEntry("customerName", "Jane Doe");
        assertThat(response.extractedFields()).containsEntry("documentNumber", "P1234567");
    }

    @Test
    void sparseTextGivesLowConfidenceAndGoesToReviewQueue() {
        ProcessedDocumentResponse response = service.process(
                new ProcessDocumentRequest("s3://uploads/blurry-scan.pdf", "Name: John Smith\npassport", null));

        assertThat(response.reviewStatus()).isEqualTo(ReviewStatus.PENDING_REVIEW);

        var queue = service.getReviewQueue(PageRequest.of(0, 10));
        assertThat(queue.getContent()).anyMatch(d -> d.id().equals(response.id()));
    }

    @Test
    void humanReviewApprovalMovesDocumentOutOfPendingReview() {
        ProcessedDocumentResponse pending = service.process(
                new ProcessDocumentRequest("s3://uploads/unclear.pdf", "passport document", null));
        assertThat(pending.reviewStatus()).isEqualTo(ReviewStatus.PENDING_REVIEW);

        ProcessedDocumentResponse reviewed = service.submitReview(
                pending.id(), true, "staff1", "Manually verified against physical ID");

        assertThat(reviewed.reviewStatus()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    void unclassifiableTextIsMarkedUnknownWithZeroConfidence() {
        ProcessedDocumentResponse response = service.process(
                new ProcessDocumentRequest("s3://uploads/random.txt", "the quick brown fox", null));

        assertThat(response.documentType()).isEqualTo(BankDocumentType.UNKNOWN);
        assertThat(response.confidence()).isEqualTo(0.0);
        assertThat(response.reviewStatus()).isEqualTo(ReviewStatus.PENDING_REVIEW);
    }
}
