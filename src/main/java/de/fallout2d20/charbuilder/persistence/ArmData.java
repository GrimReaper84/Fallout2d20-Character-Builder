package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of one Mister Handy arm: an attachment by its enum name, a free weapon's name, or
 * neither for an open arm.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArmData(@Nullable String attachment, @Nullable String weapon) {}
