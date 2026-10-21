package com.doping.prescription.seedcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The checker verifies the seed questions, so it is tested itself: a wrong checker would bless wrong answers. */
class RationalExpressionTest {

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                // precedence and associativity
                "2+3*4 | 14",
                "(2+3)*4 | 20",
                "10-4-3 | 3",
                "12/4/3 | 1",
                "2^3^2 | 512",
                "-2^2 | -4",
                "(-2)^3 | -8",
                "(-2)^3-(-2)^2 | -12",
                // exact fractions, reduced
                "1/2+2/3-1/6 | 1",
                "3/(3+5) | 3/8",
                "(2/3)/(4/9) | 3/2",
                "1+1/(1+1/2) | 5/3",
                "1/(1/4-1/6) | 12",
                "6/36 | 1/6",
                // powers with negative and zero exponents
                "(1/2)^(-3) | 8",
                "2^-2 | 1/4",
                "7^0 | 1",
                "2^5*2^3/2^6 | 4",
                // decimals
                "0.25+0.5*1.5 | 1",
                "0.125+0.375 | 1/2",
                "210/60 | 7/2",
                "100*(1+20/100)*(1-20/100) | 96",
                // counting
                "C(5,2) | 10",
                "C(7,2) | 21",
                "P(7,2) | 42",
                "C(4,0) | 1",
                "fact(5) | 120",
                "fact(0) | 1",
                "fact(6)/(fact(3)*fact(2)) | 60",
                // exact roots and logarithms
                "sqrt(48/3)+sqrt(27/3) | 7",
                "sqrt(72/2) | 6",
                "sqrt(9/4) | 3/2",
                "log(2,32)-1 | 4",
                "log(3,1/9) | -2"
            })
    void evaluatesExactly(String expression, String expected) {
        assertThat(RationalExpression.evaluate(expression).toString()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "option [{0}] = {1}")
    @CsvSource(
            delimiter = '|',
            value = {"1/3 | 1/3", "−12 | -12", "2,4 | 12/5", "0,375 | 3/8", " 96 | 96", "1,125 | 9/8"})
    void parsesOptionTexts(String option, String expected) {
        assertThat(RationalExpression.parseOption(option).toString()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "rejects option [{0}]")
    @CsvSource(
            delimiter = '|',
            value = {"2√3", "{1, 2}", "x + 1", "tek"})
    void rejectsNonNumericOptions(String option) {
        assertThatThrownBy(() -> RationalExpression.parseOption(option)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "rejects {0}")
    @CsvSource(
            delimiter = '|',
            // the messages contain single quotes, so they cannot be the CSV quote character
            quoteCharacter = '"',
            value = {
                "1/0 | division by zero",
                "sqrt(2) | not rational",
                "log(2,3) | not an integer",
                "2^(1/2) | non-integer exponent",
                "C(2,3) | undefined",
                "fact(5/2) | integer argument expected",
                "2+ | number expected",
                "(1+2 | ')' expected",
                "max(1,2) | unknown function",
                "3)+4 | unexpected character"
            })
    void rejectsInvalidExpressions(String expression, String reason) {
        assertThatThrownBy(() -> RationalExpression.evaluate(expression)).hasMessageContaining(reason);
    }
}
