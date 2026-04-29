package com.tp2.sync;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleRecord(
        long id,
        String productCode,
        int quantity,
        BigDecimal unitPrice,
        LocalDateTime soldAt,
        String office
) {
    public String toMessage() {
        return id + "|" + productCode + "|" + quantity + "|" + unitPrice + "|" + soldAt + "|" + office;
    }

    public static SaleRecord fromMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid message format: " + message);
        }
        return new SaleRecord(
                Long.parseLong(parts[0]),
                parts[1],
                Integer.parseInt(parts[2]),
                new BigDecimal(parts[3]),
                LocalDateTime.parse(parts[4]),
                parts[5]
        );
    }
}
