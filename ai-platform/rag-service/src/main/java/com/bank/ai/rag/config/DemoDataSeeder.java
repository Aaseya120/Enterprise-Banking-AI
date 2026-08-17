package com.bank.ai.rag.config;

import com.bank.ai.rag.model.IngestDocumentRequest;
import com.bank.ai.rag.service.DocumentIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;

/**
 * Loads a couple of sample knowledge-base documents (Personal Loan Policy,
 * KYC SOP) into the vector store at startup, purely so the end-to-end RAG
 * flow (section 53) can be exercised without a real document-ingestion
 * pipeline wired up yet.
 */
@Configuration
@Profile("!test")
public class DemoDataSeeder {

    @Bean
    public CommandLineRunner seedDemoDocuments(DocumentIngestionService ingestionService) {
        return args -> {
            ingestionService.ingest(new IngestDocumentRequest(
                    "DOC-LOAN-POLICY-V3",
                    "LOAN_POLICY",
                    "Personal Loan Policy v3",
                    "Personal loans require the applicant to be at least 21 years old, have a minimum "
                            + "credit score of 650, and provide proof of income for the last 3 months. "
                            + "Maximum loan tenure is 5 years. Required documents: government ID, proof of "
                            + "address, and last 3 salary slips or income tax returns for self-employed applicants.",
                    Map.of("roles", List.of("CUSTOMER", "BANK_STAFF", "ANALYST"), "department", "RETAIL_BANKING")
            ));

            ingestionService.ingest(new IngestDocumentRequest(
                    "DOC-KYC-SOP-V2",
                    "SOP",
                    "KYC Verification SOP",
                    "KYC verification requires two forms of government-issued identification and one proof "
                            + "of address dated within the last 3 months. Staff must verify document authenticity "
                            + "using the document scanner before approving any account opening.",
                    Map.of("roles", List.of("BANK_STAFF"), "department", "COMPLIANCE")
            ));
        };
    }
}
