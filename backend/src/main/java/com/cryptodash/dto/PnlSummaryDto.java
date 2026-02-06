package com.cryptodash.dto;

import java.math.BigDecimal;

public record PnlSummaryDto(BigDecimal realisedUsdt, BigDecimal unrealisedUsdt, BigDecimal totalUsdt) {}
