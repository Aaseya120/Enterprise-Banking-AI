package com.bank.report.controller;

import com.bank.report.application.ReportInsightService;
import com.bank.report.dto.GenerateReportRequest;
import com.bank.report.dto.GeneratedReportResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/insights")
public class ReportInsightController {

    private final ReportInsightService reportInsightService;

    public ReportInsightController(ReportInsightService reportInsightService) {
        this.reportInsightService = reportInsightService;
    }

    @PostMapping("/generate")
    public ResponseEntity<GeneratedReportResponse> generate(@Valid @RequestBody GenerateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportInsightService.generate(request));
    }

    @GetMapping("/{reportId}")
    public GeneratedReportResponse get(@PathVariable String reportId) {
        return reportInsightService.get(reportId);
    }

    @GetMapping(params = "subjectRef")
    public Page<GeneratedReportResponse> getForSubject(@RequestParam String subjectRef, Pageable pageable) {
        return reportInsightService.getForSubject(subjectRef, pageable);
    }
}
