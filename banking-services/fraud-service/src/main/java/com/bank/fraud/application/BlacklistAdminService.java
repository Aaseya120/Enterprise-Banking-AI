package com.bank.fraud.application;

import com.bank.common.exception.BusinessException;
import com.bank.fraud.domain.BlacklistEntry;
import com.bank.fraud.domain.BlacklistEntryRepository;
import com.bank.fraud.dto.AddBlacklistEntryRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlacklistAdminService {

    private final BlacklistEntryRepository blacklistEntryRepository;

    public BlacklistAdminService(BlacklistEntryRepository blacklistEntryRepository) {
        this.blacklistEntryRepository = blacklistEntryRepository;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','ADMIN')")
    public BlacklistEntry add(AddBlacklistEntryRequest request) {
        if (blacklistEntryRepository.existsByEntityRef(request.entityRef())) {
            throw BusinessException.ruleViolation("Entity already blacklisted: " + request.entityRef());
        }
        return blacklistEntryRepository.save(new BlacklistEntry(request.entityRef(), request.reason()));
    }

    @Transactional(readOnly = true)
    public java.util.List<BlacklistEntry> list() {
        return blacklistEntryRepository.findAll();
    }
}
