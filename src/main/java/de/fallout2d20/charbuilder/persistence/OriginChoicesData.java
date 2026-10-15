package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/** Save-file form of the origin choices; {@code tagSkill} is a skill id. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OriginChoicesData(@Nullable String tagSkill, boolean noPincer, @Nullable String purpose) {}
