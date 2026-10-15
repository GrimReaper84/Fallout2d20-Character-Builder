package de.fallout2d20.charbuilder.catalog;

import org.jspecify.annotations.NonNull;

/**
 * The rulebooks this application supports. Catalog entries cite them by their constant name
 * ({@code "CRB"}); content of other books is not supported.
 */
public enum Book {
  CRB("Core Rulebook"),
  WOA("Winter of Atom"),
  WG("Wanderer's Guide");

  private final @NonNull String title;

  Book(@NonNull String title) {
    this.title = title;
  }

  public @NonNull String title() {
    return title;
  }
}
