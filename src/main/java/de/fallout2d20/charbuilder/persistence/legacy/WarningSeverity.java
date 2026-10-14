package de.fallout2d20.charbuilder.persistence.legacy;

/** Severity of an import warning. */
public enum WarningSeverity {
  /** Something was adapted or kept for later; no data was lost. */
  INFO,
  /** Data could not be interpreted and was only kept in the legacy payload, or was defaulted. */
  WARNING
}
