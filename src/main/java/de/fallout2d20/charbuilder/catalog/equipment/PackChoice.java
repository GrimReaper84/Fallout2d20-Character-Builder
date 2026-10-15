package de.fallout2d20.charbuilder.catalog.equipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * A "pick one of" part of an equipment pack, e.g. the melee weapon of the Settler pack (CRB p. 79).
 *
 * @param label what is chosen, e.g. {@code Melee weapon}
 * @param options the items to choose from, at least two, as short item lines
 */
public record PackChoice(
    @JsonProperty(required = true) @NonNull String label,
    @JsonProperty(required = true) @NonNull List<String> options) {

  public PackChoice {
    if (label == null || label.isBlank() || options == null || options.size() < 2
        || options.stream().anyMatch(String::isBlank)
        || new HashSet<>(options).size() != options.size()) {
      throw new IllegalArgumentException("Pack choice needs a label and two or more distinct options: "
          + label);
    }
    options = List.copyOf(options);
  }
}
