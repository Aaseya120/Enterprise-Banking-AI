package com.bank.fraud.controller;

import com.bank.fraud.application.FraudRuleEngine;
import com.bank.fraud.dto.FraudEvaluationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud")
public class FraudEvaluationController {

    private final FraudRuleEngine fraudRuleEngine;

    public FraudEvaluationController(FraudRuleEngine fraudRuleEngine) {
        this.fraudRuleEngine = fraudRuleEngine;
    }

    @PostMapping("/evaluate")
    public FraudEvaluationRequest.Response evaluate(@Valid @RequestBody FraudEvaluationRequest request) {
        return fraudRuleEngine.evaluate(request);
    }
}
