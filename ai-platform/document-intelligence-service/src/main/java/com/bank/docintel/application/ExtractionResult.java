package com.bank.docintel.application;

import java.util.Map;

/**
 * fields: the structured data pulled from the document text.
 * confidence: 0.0-1.0, based on how many expected fields for this document
 * type were actually found (plan section 22: "Include confidence scores").
 */
public record ExtractionResult(Map<String, String> fields, double confidence) {
}
