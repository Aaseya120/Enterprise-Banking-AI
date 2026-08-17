package com.bank.fraud.controller;

import com.bank.fraud.application.BlacklistAdminService;
import com.bank.fraud.domain.BlacklistEntry;
import com.bank.fraud.dto.AddBlacklistEntryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud/blacklist")
public class BlacklistController {

    private final BlacklistAdminService blacklistAdminService;

    public BlacklistController(BlacklistAdminService blacklistAdminService) {
        this.blacklistAdminService = blacklistAdminService;
    }

    @PostMapping
    public ResponseEntity<BlacklistEntry> add(@Valid @RequestBody AddBlacklistEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blacklistAdminService.add(request));
    }

    @GetMapping
    public List<BlacklistEntry> list() {
        return blacklistAdminService.list();
    }
}
