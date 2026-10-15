package de.fallout2d20.charbuilder.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/**
 * Where a catalog entry is described: the book and the printed page on which the entry starts (not
 * the PDF page).
 *
 * @param book the rulebook
 * @param page printed page number, at least 1
 */
public record Source(
    @JsonProperty(required = true) @NonNull Book book,
    @JsonProperty(required = true) int page) {

  public Source {
    if (book == null || page < 1) {
      throw new IllegalArgumentException("Source needs a book and a printed page of at least 1");
    }
  }

  /** Short citation, e.g. {@code CRB p. 56}. */
  @Override
  public @NonNull String toString() {
    return book.name() + " p. " + page;
  }
}
