package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Collects the detail lines of a compendium entry (H2); empty values and empty link lists are left out. */
final class Details {

  private final @NonNull List<DetailLine> lines = new ArrayList<>();

  /** Adds the value unless it is null, blank or a dash. */
  @NonNull Details value(@NonNull String label, @Nullable Object value) {
    if (value != null) {
      String text = value.toString().strip();
      if (!text.isEmpty() && !text.equals("–")) {
        lines.add(new DetailValue(label, text));
      }
    }
    return this;
  }

  /** Adds the value only when the condition holds, e.g. "Robot: yes". */
  @NonNull Details flag(@NonNull String label, boolean condition, @NonNull String text) {
    return condition ? value(label, text) : this;
  }

  /** Adds the texts as one comma-separated value. */
  @NonNull Details list(@NonNull String label, @NonNull List<String> texts) {
    return value(label, String.join(", ", texts));
  }

  @NonNull Details links(@NonNull String label, @NonNull List<Reference> references) {
    if (!references.isEmpty()) {
      lines.add(new DetailLinks(label, references));
    }
    return this;
  }

  /** The source line, e.g. "CRB p. 61, WG p. 12"; always last. */
  @NonNull List<DetailLine> source(@NonNull List<Source> sources) {
    value("Source", sources.stream().map(Source::toString).collect(Collectors.joining(", ")));
    return lines;
  }

  @NonNull List<DetailLine> lines() {
    return lines;
  }

  /** "12 caps" etc.; null for a missing value. */
  static @Nullable String amount(@Nullable Object value, @NonNull String unit) {
    return value == null ? null : value + unit;
  }
}
