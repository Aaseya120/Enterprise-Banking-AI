package com.bank.report.dto;

import com.bank.report.domain.ReportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateReportRequest(
        @NotNull ReportType reportType,
        @NotBlank String subjectRef,
        @Min(1) @Max(365) int periodDays
) {
}
