package com.cryptodash.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PerformancePointDto(Instant timestamp, BigDecimal totalUsdt) {}
