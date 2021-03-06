package de.tuberlin.esi.common.util;


import io.fabric8.kubernetes.api.model.Quantity;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public class QuantityUtil {
    public static Quantity getQuantityFromBytes(BigDecimal bytes) throws ArithmeticException {
        return getQuantityFromBytes(bytes, false);
    }

    public static Quantity getQuantityFromBytes(BigDecimal bytes, boolean base2) throws ArithmeticException {

        var abs = bytes.abs();

        var base2units = List.of(
                Unit.u("Ki", 2, 10),
                Unit.u("Mi", 2, 20),
                Unit.u("Gi", 2, 30),
                Unit.u("Ti", 2, 40),
                Unit.u("Pi", 2, 50),
                Unit.u("Ei", 2, 60));

        var units = List.of(Unit.u("n", 10, -9), Unit.u("u", 10, -6), Unit.u("m", 10, -3),
                Unit.u("k", 10, 3), Unit.u("M", 10, 6), Unit.u("G", 10, 9), Unit.u("T", 10, 12), Unit.u("P", 10, 15),
                Unit.u("E", 10, 18));

        if (base2) {
            for (int i = 1; i < base2units.size(); i++) {
                var unit = base2units.get(i);
                var prev = base2units.get(i - 1);
                BigDecimal pow = BigDecimal.valueOf(unit.getBase()).pow(unit.getExponent(), MathContext.DECIMAL64);
                if (abs.compareTo(pow) < 0) {
                    BigDecimal prevPow = BigDecimal.valueOf(prev.getBase())
                                                   .pow(prev.getExponent(), MathContext.DECIMAL64);
                    return new Quantity(bytes.divide(prevPow, 1, RoundingMode.FLOOR).stripTrailingZeros()
                                             .toPlainString() + prev.getFormat());
                }
            }
        }

        for (int i = 1; i < units.size(); i++) {
            var unit = units.get(i);
            var prev = units.get(i - 1);
            BigDecimal pow = BigDecimal.valueOf(unit.getBase()).pow(unit.getExponent(), MathContext.DECIMAL64);
            if (abs.compareTo(pow) < 0) {
                BigDecimal prevPow = BigDecimal.valueOf(prev.getBase()).pow(prev.getExponent(), MathContext.DECIMAL64);
                return new Quantity(bytes.divide(prevPow, 1, RoundingMode.FLOOR).stripTrailingZeros()
                                         .toPlainString() + prev.getFormat());
            }

        }

        throw new RuntimeException("Something went Wrong during conversion");

    }

    @RequiredArgsConstructor
    @Value
    static class Unit {
        String format;
        int base;
        int exponent;

        public static Unit u(String format, int base, int exponent) {
            return new Unit(format, base, exponent);
        }
    }
}
