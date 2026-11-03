package de.fallout2d20.charbuilder.view.compendium;

import java.util.List;
import org.jspecify.annotations.NonNull;

/** Links of a compendium entry to other entries (H2), e.g. "Ammunition: 10mm". */
record DetailLinks(@NonNull String label, @NonNull List<Reference> references) implements DetailLine {

  DetailLinks {
    references = List.copyOf(references);
  }
}
