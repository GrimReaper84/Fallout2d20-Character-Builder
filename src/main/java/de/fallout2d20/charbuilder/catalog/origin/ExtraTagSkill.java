package de.fallout2d20.charbuilder.catalog.origin;

import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * An additional Tag skill an origin grants on top of the three free ones (CRB p. 58), e.g. Vault
 * Kid (CRB p. 57) or The Chain that Binds (CRB p. 51).
 *
 * @param options skills the extra Tag may be chosen from; empty means any skill
 */
public record ExtraTagSkill(@NonNull List<Skill> options) {

  public ExtraTagSkill {
    options = options == null ? List.of() : List.copyOf(options);
  }

  /** Whether the skill may be chosen as this extra Tag skill. */
  public boolean allows(@NonNull Skill skill) {
    return options.isEmpty() || options.contains(skill);
  }
}
