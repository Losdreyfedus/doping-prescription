package com.doping.prescription.seedcheck;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** An exact fraction, always reduced and with a positive denominator. */
public record Rational(BigInteger numerator, BigInteger denominator) {

    public static final Rational ZERO = of(0);
    public static final Rational ONE = of(1);

    public Rational {
        Objects.requireNonNull(numerator, "numerator");
        Objects.requireNonNull(denominator, "denominator");
        if (denominator.signum() == 0) {
            throw new ArithmeticException("division by zero");
        }
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger gcd = numerator.gcd(denominator);
        if (!gcd.equals(BigInteger.ONE) && gcd.signum() != 0) {
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        }
    }

    public static Rational of(long value) {
        return new Rational(BigInteger.valueOf(value), BigInteger.ONE);
    }

    public static Rational of(long numerator, long denominator) {
        return new Rational(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    static Rational ofDecimal(String text) {
        BigDecimal decimal = new BigDecimal(text);
        if (decimal.scale() >= 0) {
            return new Rational(decimal.unscaledValue(), BigInteger.TEN.pow(decimal.scale()));
        }
        return new Rational(decimal.unscaledValue().multiply(BigInteger.TEN.pow(-decimal.scale())), BigInteger.ONE);
    }

    public Rational add(Rational other) {
        return new Rational(
                numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public Rational subtract(Rational other) {
        return add(other.negate());
    }

    public Rational multiply(Rational other) {
        return new Rational(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }

    public Rational divide(Rational other) {
        return new Rational(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
    }

    public Rational negate() {
        return new Rational(numerator.negate(), denominator);
    }

    public boolean isInteger() {
        return denominator.equals(BigInteger.ONE);
    }

    /** Integer exponent only; negative exponents invert. */
    public Rational pow(Rational exponent) {
        if (!exponent.isInteger()) {
            throw new ArithmeticException("non-integer exponent " + exponent);
        }
        int power = exponent.numerator.intValueExact();
        Rational base = power < 0 ? ONE.divide(this) : this;
        int magnitude = Math.abs(power);
        return new Rational(base.numerator.pow(magnitude), base.denominator.pow(magnitude));
    }

    /** Exact square root; fails unless numerator and denominator are perfect squares. */
    public Rational sqrt() {
        if (numerator.signum() < 0) {
            throw new ArithmeticException("square root of a negative number");
        }
        BigInteger rootNumerator = numerator.sqrt();
        BigInteger rootDenominator = denominator.sqrt();
        if (!rootNumerator.pow(2).equals(numerator) || !rootDenominator.pow(2).equals(denominator)) {
            throw new ArithmeticException("square root of " + this + " is not rational");
        }
        return new Rational(rootNumerator, rootDenominator);
    }

    @Override
    public String toString() {
        return isInteger() ? numerator.toString() : numerator + "/" + denominator;
    }
}
