package de.fallout2d20.charbuilder.persistence.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Reads numbers of legacy saves, which store them as numbers or as text. */
final class LegacyNumbers {

  private LegacyNumbers() {}

  /** A whole number from a number or a numeric text, also written with decimals (16.0); {@code null} otherwise. */
  static @Nullable Integer integer(@Nullable JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    try {
      return (node.isNumber() ? node.decimalValue() : new BigDecimal(node.asText().strip())).intValueExact();
    } catch (NumberFormatException | ArithmeticException _) {
      return null;
    }
  }
}
