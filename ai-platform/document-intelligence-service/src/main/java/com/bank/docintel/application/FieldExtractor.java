package com.bank.docintel.application;

import com.bank.docintel.domain.BankDocumentType;

public interface FieldExtractor {

    BankDocumentType supports();

    ExtractionResult extract(String text);
}
