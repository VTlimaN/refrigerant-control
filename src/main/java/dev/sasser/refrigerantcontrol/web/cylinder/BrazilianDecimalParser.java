package dev.sasser.refrigerantcontrol.web.cylinder;

import java.math.BigDecimal;
import java.util.regex.Pattern;

final class BrazilianDecimalParser {

	private static final Pattern DECIMAL_PATTERN = Pattern.compile("-?[0-9]+([,.][0-9]+)?");

	BigDecimal parse(String input) {
		if (input == null) {
			throw new IllegalArgumentException("decimal input must not be null");
		}

		String normalizedInput = input.strip();
		if (!DECIMAL_PATTERN.matcher(normalizedInput).matches()) {
			throw new IllegalArgumentException("invalid decimal input");
		}

		return new BigDecimal(normalizedInput.replace(',', '.'));
	}
}
