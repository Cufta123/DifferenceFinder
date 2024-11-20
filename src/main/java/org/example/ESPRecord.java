package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ESPRecord(String serialNumber, double amount, double serviceFee, double suplierMargin) {

    public ESPRecord {
        suplierMargin = roundToTwoDecimalPoints(suplierMargin);
    }

    private static double roundToTwoDecimalPoints(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public double getTotalAmount() {
        return amount + serviceFee;
    }
}