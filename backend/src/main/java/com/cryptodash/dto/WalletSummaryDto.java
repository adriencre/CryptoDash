package com.cryptodash.dto;

import java.util.List;

public record WalletSummaryDto(List<WalletPositionDto> positions) {}
