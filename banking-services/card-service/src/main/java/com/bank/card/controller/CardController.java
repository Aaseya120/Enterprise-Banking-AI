package com.bank.card.controller;

import com.bank.card.application.CardService;
import com.bank.card.dto.CardResponse;
import com.bank.card.dto.IssueCardRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<IssueCardRequest.Response> issue(@Valid @RequestBody IssueCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.issueCard(request));
    }

    @GetMapping("/{cardId}")
    public CardResponse get(@PathVariable String cardId) {
        return cardService.getCard(cardId);
    }

    @GetMapping(params = "accountId")
    public List<CardResponse> getForAccount(@RequestParam String accountId) {
        return cardService.getCardsForAccount(accountId);
    }

    @PostMapping("/{cardId}/activate")
    public CardResponse activate(@PathVariable String cardId) {
        return cardService.activate(cardId);
    }

    @PostMapping("/{cardId}/freeze")
    public CardResponse freeze(@PathVariable String cardId) {
        return cardService.freeze(cardId);
    }

    @PostMapping("/{cardId}/unfreeze")
    public CardResponse unfreeze(@PathVariable String cardId) {
        return cardService.unfreeze(cardId);
    }

    @PostMapping("/{cardId}/block")
    public CardResponse block(@PathVariable String cardId) {
        return cardService.block(cardId);
    }

    @PostMapping("/{cardId}/close")
    public CardResponse close(@PathVariable String cardId) {
        return cardService.close(cardId);
    }
}
