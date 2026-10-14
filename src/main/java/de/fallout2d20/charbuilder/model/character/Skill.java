package de.fallout2d20.charbuilder.model.character;

import java.util.Optional;

/**
 * The 17 skills and their default S.P.E.C.I.A.L. attribute.
 *
 * <p>CRB p. 44: skills are ranked from 0 to 6, every skill starts at rank 0, and each skill has a
 * default attribute (the skills summary table). Supplements do not add skills, so they are modeled
 * as an enum instead of a catalog.
 */
public enum Skill {
  ATHLETICS("athletics", "Athletics", Attribute.STRENGTH),
  BARTER("barter", "Barter", Attribute.CHARISMA),
  BIG_GUNS("big-guns", "Big Guns", Attribute.ENDURANCE),
  ENERGY_WEAPONS("energy-weapons", "Energy Weapons", Attribute.PERCEPTION),
  EXPLOSIVES("explosives", "Explosives", Attribute.PERCEPTION),
  LOCKPICK("lockpick", "Lockpick", Attribute.PERCEPTION),
  MEDICINE("medicine", "Medicine", Attribute.INTELLIGENCE),
  MELEE_WEAPONS("melee-weapons", "Melee Weapons", Attribute.STRENGTH),
  PILOT("pilot", "Pilot", Attribute.PERCEPTION),
  REPAIR("repair", "Repair", Attribute.INTELLIGENCE),
  SCIENCE("science", "Science", Attribute.INTELLIGENCE),
  SMALL_GUNS("small-guns", "Small Guns", Attribute.AGILITY),
  SNEAK("sneak", "Sneak", Attribute.AGILITY),
  SPEECH("speech", "Speech", Attribute.CHARISMA),
  SURVIVAL("survival", "Survival", Attribute.ENDURANCE),
  THROWING("throwing", "Throwing", Attribute.AGILITY),
  UNARMED("unarmed", "Unarmed", Attribute.STRENGTH);

  private final String id;
  private final String displayName;
  private final Attribute governingAttribute;

  Skill(String id, String displayName, Attribute governingAttribute) {
    this.id = id;
    this.displayName = displayName;
    this.governingAttribute = governingAttribute;
  }

  /** Stable identifier used in save files, e.g. {@code "small-guns"}. */
  public String id() {
    return id;
  }

  /** Name as printed in the Core Rulebook; legacy saves use it as key. */
  public String displayName() {
    return displayName;
  }

  public Attribute governingAttribute() {
    return governingAttribute;
  }

  public static Optional<Skill> fromId(String id) {
    for (Skill skill : values()) {
      if (skill.id.equals(id)) {
        return Optional.of(skill);
      }
    }
    return Optional.empty();
  }

  public static Optional<Skill> fromDisplayName(String displayName) {
    for (Skill skill : values()) {
      if (skill.displayName.equals(displayName)) {
        return Optional.of(skill);
      }
    }
    return Optional.empty();
  }
}
