package com.bank.report.dto;

import com.bank.report.domain.AccountMetrics;
import com.bank.report.domain.GeneratedReport;
import com.bank.report.domain.ReportType;

import java.time.Instant;

public record GeneratedReportResponse(
        String reportId,
        ReportType reportType,
        String subjectRef,
        AccountMetrics metrics,
        String narrativeSummary,
        String modelProvider,
        boolean grounded,
        Instant createdAt
) {
    public static GeneratedReportResponse of(GeneratedReport report, AccountMetrics metrics) {
        return new GeneratedReportResponse(report.getReportId(), report.getReportType(), report.getSubjectRef(),
                metrics, report.getNarrativeSummary(), report.getModelProvider(), report.isGrounded(),
                report.getCreatedAt());
    }
}
