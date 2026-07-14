package dev.sasser.refrigerantcontrol.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SealNumberTest {

	@Test
	void shouldPreserveSealNumberExactly() {
		SealNumber sealNumber = SealNumber.of("LACRE-001");

		assertEquals("LACRE-001", sealNumber.value());
	}

	@Test
	void shouldRejectNullSealNumber() {
		assertThrows(NullPointerException.class, () -> SealNumber.of(null));
	}

	@Test
	void shouldRejectBlankSealNumber() {
		assertThrows(IllegalArgumentException.class, () -> SealNumber.of("   "));
	}

	@Test
	void shouldCompareByExactValue() {
		SealNumber first = SealNumber.of("LACRE-001");
		SealNumber same = SealNumber.of("LACRE-001");
		SealNumber different = SealNumber.of("lacre-001");

		assertEquals(first, same);
		assertEquals(first.hashCode(), same.hashCode());
		assertNotEquals(first, different);
	}
}
