package com.doping.prescription.seedcheck;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the {@code check} expressions of seed questions exactly, so a question's answer can be verified
 * independently of its option texts.
 *
 * <p>Grammar: numbers (integer or decimal with a dot), {@code + - * / ^}, parentheses, unary minus and the functions
 * {@code C(n,k)}, {@code P(n,k)}, {@code fact(n)}, {@code sqrt(x)} (exact only) and {@code log(b,x)} (exact integer
 * result only). {@code ^} is right-associative and binds tighter than unary minus, as in mathematics:
 * {@code -2^2 = -4}, so a negative base needs parentheses, {@code (-2)^3}.
 */
public final class RationalExpression {

    private static final int MAX_LOG_EXPONENT = 64;

    private final String text;
    private int position;

    private RationalExpression(String text) {
        this.text = text.replace(" ", "");
    }

    public static Rational evaluate(String expression) {
        RationalExpression parser = new RationalExpression(expression);
        Rational value = parser.expression();
        if (parser.position != parser.text.length()) {
            throw parser.error("unexpected character");
        }
        return value;
    }

    /**
     * Reads an option text such as {@code 1/3}, {@code −12} or {@code 2,4} (Turkish decimal comma, Unicode minus).
     *
     * @throws IllegalArgumentException if the text is not numeric
     */
    public static Rational parseOption(String optionText) {
        String normalized = optionText.strip().replace('−', '-').replace(',', '.');
        if (!normalized.matches("-?[0-9.]+(/[0-9.]+)?")) {
            throw new IllegalArgumentException("Option is not a number: " + optionText);
        }
        return evaluate(normalized);
    }

    private Rational expression() {
        Rational value = term();
        while (peek('+') || peek('-')) {
            char operator = next();
            Rational right = term();
            value = operator == '+' ? value.add(right) : value.subtract(right);
        }
        return value;
    }

    private Rational term() {
        Rational value = unary();
        while (peek('*') || peek('/')) {
            char operator = next();
            Rational right = unary();
            value = operator == '*' ? value.multiply(right) : value.divide(right);
        }
        return value;
    }

    private Rational unary() {
        if (peek('-')) {
            next();
            return unary().negate();
        }
        return power();
    }

    private Rational power() {
        Rational base = primary();
        if (peek('^')) {
            next();
            return base.pow(unary());
        }
        return base;
    }

    private Rational primary() {
        if (peek('(')) {
            next();
            Rational value = expression();
            expect(')');
            return value;
        }
        if (position < text.length() && Character.isLetter(text.charAt(position))) {
            return function();
        }
        return number();
    }

    private Rational function() {
        int start = position;
        while (position < text.length() && Character.isLetter(text.charAt(position))) {
            position++;
        }
        String name = text.substring(start, position);
        List<Rational> args = arguments();
        return switch (name) {
            case "C" -> combinations(integer(args, 0, 2), integer(args, 1, 2));
            case "P" -> permutations(integer(args, 0, 2), integer(args, 1, 2));
            case "fact" -> factorial(integer(args, 0, 1));
            case "sqrt" -> single(args).sqrt();
            case "log" -> log(argument(args, 0, 2), argument(args, 1, 2));
            default -> throw error("unknown function " + name);
        };
    }

    private List<Rational> arguments() {
        expect('(');
        List<Rational> args = new ArrayList<>();
        args.add(expression());
        while (peek(',')) {
            next();
            args.add(expression());
        }
        expect(')');
        return args;
    }

    private Rational number() {
        int start = position;
        while (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
            position++;
        }
        if (start == position) {
            throw error("number expected");
        }
        return Rational.ofDecimal(text.substring(start, position));
    }

    private static Rational combinations(BigInteger n, BigInteger k) {
        return permutations(n, k).divide(factorial(k));
    }

    private static Rational permutations(BigInteger n, BigInteger k) {
        if (k.signum() < 0 || k.compareTo(n) > 0) {
            throw new ArithmeticException("P(" + n + "," + k + ") is undefined");
        }
        BigInteger result = BigInteger.ONE;
        for (BigInteger i = n.subtract(k).add(BigInteger.ONE); i.compareTo(n) <= 0; i = i.add(BigInteger.ONE)) {
            result = result.multiply(i);
        }
        return new Rational(result, BigInteger.ONE);
    }

    private static Rational factorial(BigInteger n) {
        return permutations(n, n);
    }

    private static Rational log(Rational base, Rational value) {
        for (int exponent = -MAX_LOG_EXPONENT; exponent <= MAX_LOG_EXPONENT; exponent++) {
            Rational candidate = Rational.of(exponent);
            if (base.pow(candidate).equals(value)) {
                return candidate;
            }
        }
        throw new ArithmeticException("log(" + base + "," + value + ") is not an integer");
    }

    private BigInteger integer(List<Rational> args, int index, int expectedCount) {
        Rational argument = argument(args, index, expectedCount);
        if (!argument.isInteger()) {
            throw error("integer argument expected, got " + argument);
        }
        return argument.numerator();
    }

    private Rational argument(List<Rational> args, int index, int expectedCount) {
        if (args.size() != expectedCount) {
            throw error(expectedCount + " argument(s) expected");
        }
        return args.get(index);
    }

    private Rational single(List<Rational> args) {
        return argument(args, 0, 1);
    }

    private boolean peek(char character) {
        return position < text.length() && text.charAt(position) == character;
    }

    private char next() {
        return text.charAt(position++);
    }

    private void expect(char character) {
        if (!peek(character)) {
            throw error("'" + character + "' expected");
        }
        position++;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + position + " in " + text);
    }
}
