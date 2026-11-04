package de.fallout2d20.charbuilder.model.scavenging;

import org.jspecify.annotations.NonNull;

/** Why a roll was made; shown beside the find, so results beyond the book's minimum stay visible. */
public enum FindOrigin {
  /** A minimum roll of a successful search, or looting an NPC. */
  BASIC(""),
  /** 1 AP spent (CRB p. 196). */
  AP("AP"),
  /** Outside the AP economy: Can Do!, Pharma Farma, a GM gift. */
  FREE("free"),
  /** The Roll to maximum button. */
  TO_MAXIMUM("to maximum");

  private final @NonNull String label;

  FindOrigin(@NonNull String label) {
    this.label = label;
  }

  public @NonNull String label() {
    return label;
  }
}
