package dev.sasser.refrigerantcontrol.web.support;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrazilianDecimalParserTest {

	private final BrazilianDecimalParser parser = new BrazilianDecimalParser();

	@ParameterizedTest
	@ValueSource(strings = {"15", "15,14", "15.14", "0"})
	void shouldParseIntegerCommaDotAndZeroValues(String input) {
		BigDecimal expected = new BigDecimal(input.replace(',', '.'));

		assertEquals(expected, parser.parse(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"15,140", "15.140", "0,000"})
	void shouldPreserveEnteredDecimalScale(String input) {
		BigDecimal parsed = parser.parse(input);

		assertEquals(new BigDecimal(input.replace(',', '.')), parsed);
		assertEquals(3, parsed.scale());
	}

	@Test
	void shouldRemoveSurroundingWhitespaceFromWeightText() {
		assertEquals(new BigDecimal("15.140"), parser.parse(" \t15,140\r\n"));
	}

	@Test
	void shouldParseNegativeValueForDomainValidation() {
		assertEquals(new BigDecimal("-15.140"), parser.parse("-15,140"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1.234", "1,234"})
	void shouldInterpretOneSeparatorAsDecimal(String input) {
		assertEquals(new BigDecimal("1.234"), parser.parse(input));
	}

	@Test
	void shouldNotRoundAcceptedValue() {
		BigDecimal parsed = parser.parse("15,1497");

		assertEquals(new BigDecimal("15.1497"), parsed);
		assertEquals(4, parsed.scale());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"   ",
			"letters",
			"1e3",
			"+15,14",
			"15,1,4",
			"15.1.4",
			"1.234,56",
			"1,234.56",
			"1 234,56",
			",14",
			".14",
			"15,",
			"15."})
	void shouldRejectUnsupportedDecimalFormats(String input) {
		assertThrows(IllegalArgumentException.class, () -> parser.parse(input));
	}
}
