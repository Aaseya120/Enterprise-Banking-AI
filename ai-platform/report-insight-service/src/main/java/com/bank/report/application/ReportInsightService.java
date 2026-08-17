package com.bank.report.application;

import com.bank.ai.gateway.model.AiResponse;
import com.bank.common.exception.BusinessException;
import com.bank.report.domain.AccountMetrics;
import com.bank.report.domain.GeneratedReport;
import com.bank.report.domain.GeneratedReportRepository;
import com.bank.report.domain.ReportType;
import com.bank.report.dto.GenerateReportRequest;
import com.bank.report.dto.GeneratedReportResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ties together the full section-26 flow: MetricsAggregationService
 * (Database -> Aggregation -> Metrics) then AiInsightService (Metrics ->
 * AI -> Insight), and persists both the metrics that were sent and the
 * narrative that came back, so a reviewer can always see exactly what the
 * model was and wasn't shown.
 */
@Service
public class ReportInsightService {

    private final MetricsAggregationService metricsAggregationService;
    private final AiInsightService aiInsightService;
    private final GeneratedReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    public ReportInsightService(MetricsAggregationService metricsAggregationService,
                                 AiInsightService aiInsightService,
                                 GeneratedReportRepository reportRepository,
                                 ObjectMapper objectMapper) {
        this.metricsAggregationService = metricsAggregationService;
        this.aiInsightService = aiInsightService;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GeneratedReportResponse generate(GenerateReportRequest request) {
        if (request.reportType() != ReportType.ACCOUNT_ACTIVITY_SUMMARY) {
            throw BusinessException.validation(
                    "Only ACCOUNT_ACTIVITY_SUMMARY is implemented; " + request.reportType()
                            + " is defined but has no aggregation logic yet");
        }

        AccountMetrics metrics = metricsAggregationService.aggregate(request.subjectRef(), request.periodDays());
        AiResponse aiResponse = aiInsightService.summarize(metrics);

        GeneratedReport report = new GeneratedReport(
                request.reportType(), request.subjectRef(), toJson(metrics),
                aiResponse.content(), aiResponse.provider().name(), aiResponse.grounded());

        return GeneratedReportResponse.of(reportRepository.save(report), metrics);
    }

    @Transactional(readOnly = true)
    public GeneratedReportResponse get(String reportId) {
        GeneratedReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> BusinessException.notFound("Report not found: " + reportId));
        return GeneratedReportResponse.of(report, fromJson(report.getMetricsJson()));
    }

    @Transactional(readOnly = true)
    public Page<GeneratedReportResponse> getForSubject(String subjectRef, Pageable pageable) {
        return reportRepository.findBySubjectRefOrderByCreatedAtDesc(subjectRef, pageable)
                .map(r -> GeneratedReportResponse.of(r, fromJson(r.getMetricsJson())));
    }

    private String toJson(AccountMetrics metrics) {
        try {
            return objectMapper.writeValueAsString(metrics);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize metrics", e);
        }
    }

    private AccountMetrics fromJson(String json) {
        try {
            return objectMapper.readValue(json, AccountMetrics.class);
        } catch (Exception e) {
            return null;
        }
    }
}
