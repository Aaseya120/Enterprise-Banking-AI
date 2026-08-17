-- report-insight-service schema, mirrors com.bank.report.domain.GeneratedReport.
CREATE TABLE generated_reports (
    report_id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    report_type        VARCHAR(40)  NOT NULL,
    subject_ref        VARCHAR(255) NOT NULL,
    metrics_json        TEXT        NOT NULL,
    narrative_summary   TEXT        NOT NULL,
    model_provider      VARCHAR(50) NOT NULL,
    grounded            BOOLEAN     NOT NULL,
    created_at          TIMESTAMP   NOT NULL
);

CREATE INDEX idx_generated_reports_subject_created ON generated_reports (subject_ref, created_at DESC);
