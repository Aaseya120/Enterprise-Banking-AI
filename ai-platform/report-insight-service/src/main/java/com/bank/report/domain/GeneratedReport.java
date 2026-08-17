package com.bank.report.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "generated_reports")
public class GeneratedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Column(nullable = false)
    private String subjectRef;

    /** JSON-encoded AccountMetrics -- the aggregate that was actually sent to the model. */
    @Lob
    @Column(nullable = false)
    private String metricsJson;

    @Lob
    @Column(nullable = false)
    private String narrativeSummary;

    @Column(nullable = false)
    private String modelProvider;

    @Column(nullable = false)
    private boolean grounded;

    @Column(nullable = false)
    private Instant createdAt;

    protected GeneratedReport() {
        // JPA
    }

    public GeneratedReport(ReportType reportType, String subjectRef, String metricsJson,
                            String narrativeSummary, String modelProvider, boolean grounded) {
        this.reportType = reportType;
        this.subjectRef = subjectRef;
        this.metricsJson = metricsJson;
        this.narrativeSummary = narrativeSummary;
        this.modelProvider = modelProvider;
        this.grounded = grounded;
        this.createdAt = Instant.now();
    }

    public String getReportId() { return reportId; }
    public ReportType getReportType() { return reportType; }
    public String getSubjectRef() { return subjectRef; }
    public String getMetricsJson() { return metricsJson; }
    public String getNarrativeSummary() { return narrativeSummary; }
    public String getModelProvider() { return modelProvider; }
    public boolean isGrounded() { return grounded; }
    public Instant getCreatedAt() { return createdAt; }
}
