package de.fallout2d20.charbuilder.persistence.legacy;

/**
 * One finding of a legacy import.
 *
 * @param severity how serious the finding is
 * @param field legacy field the finding refers to, e.g. {@code perks}
 * @param message human-readable description
 */
public record ImportWarning(WarningSeverity severity, String field, String message) {}
