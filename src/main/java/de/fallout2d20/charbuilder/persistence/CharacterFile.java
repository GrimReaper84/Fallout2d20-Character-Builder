package de.fallout2d20.charbuilder.persistence;

import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.persistence.legacy.ImportReport;
import org.jspecify.annotations.Nullable;

/**
 * A loaded character.
 *
 * @param character the character
 * @param importReport the report of a legacy import, {@code null} for a file in the current format;
 *     a legacy file must not be overwritten
 */
public record CharacterFile(Character character, @Nullable ImportReport importReport) {}
