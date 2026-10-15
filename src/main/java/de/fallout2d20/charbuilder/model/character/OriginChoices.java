package de.fallout2d20.charbuilder.model.character;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Choices a character makes for its origin during creation.
 *
 * @param tagSkill the additional Tag skill the origin grants (e.g. Vault Kid, CRB p. 57)
 * @param noPincer Mister Handy without a pincer attachment (CRB p. 55)
 * @param purpose purpose of a Protectron (WOA p. 21)
 */
public record OriginChoices(@Nullable Skill tagSkill, boolean noPincer, @Nullable String purpose) {

  public static final @NonNull OriginChoices NONE = new OriginChoices(null, false, null);

  public @NonNull OriginChoices withTagSkill(@Nullable Skill skill) {
    return new OriginChoices(skill, noPincer, purpose);
  }

  public @NonNull OriginChoices withNoPincer(boolean value) {
    return new OriginChoices(tagSkill, value, purpose);
  }

  public @NonNull OriginChoices withPurpose(@Nullable String value) {
    return new OriginChoices(tagSkill, noPincer, value);
  }
}
