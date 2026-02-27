package com.cryptodash.controller;

import com.cryptodash.dto.TransactionDto;
import com.cryptodash.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 100;

    private final TransactionRepository transactionRepository;

    public HistoryController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<TransactionDto> getHistory(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        int limit = Math.min(Math.max(1, size), MAX_SIZE);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(TransactionDto::from)
                .collect(Collectors.toList());
    }
}
