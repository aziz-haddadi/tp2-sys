package com.tp2.sync;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleRecord(
        LocalDate date,
        String region,
        String product,
        int qty,
        BigDecimal cost,
        BigDecimal amt,
        BigDecimal tax,
        BigDecimal total
) {
    public String toMessage() {
        return date + "|" + region + "|" + product + "|" + qty + "|" + cost + "|" + amt + "|" + tax + "|" + total;
    }

    public static SaleRecord fromMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length != 8) {
            throw new IllegalArgumentException("Invalid message format: " + message);
        }
        return new SaleRecord(
                LocalDate.parse(parts[0]),
                parts[1],
                parts[2],
                Integer.parseInt(parts[3]),
                new BigDecimal(parts[4]),
                new BigDecimal(parts[5]),
                new BigDecimal(parts[6]),
                new BigDecimal(parts[7])
        );
    }
}
