package de.fallout2d20.charbuilder.model.character.derived;

import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.Vitals;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Builds new (not imported) characters for tests; attributes are given as effective values. */
final class TestCharacterBuilder {
  private String origin = "survivor";
  private int level = 1;
  private final Map<Attribute, Integer> allocation = new EnumMap<>(Attribute.class);
  private final List<TraitSelection> traits = new ArrayList<>();
  private final List<PerkSelection> perks = new ArrayList<>();

  TestCharacterBuilder origin(String origin) {
    this.origin = origin;
    return this;
  }

  TestCharacterBuilder level(int level) {
    this.level = level;
    return this;
  }

  TestCharacterBuilder attribute(Attribute attribute, int value) {
    allocation.put(attribute, value - Character.ATTRIBUTE_BASE);
    return this;
  }

  TestCharacterBuilder trait(String name) {
    traits.add(new TraitSelection(CatalogRef.of(CatalogRef.toId(name)), false, List.of()));
    return this;
  }

  TestCharacterBuilder perk(String name, int rank) {
    perks.add(new PerkSelection(CatalogRef.of(CatalogRef.toId(name)), rank, 0, List.of(), List.of()));
    return this;
  }

  Character build() {
    return new Character("Test", CatalogRef.of(origin), level, 0, false,
        allocation, Map.of(), traits, perks, Progression.NONE, Vitals.FULL, null, null);
  }
}
