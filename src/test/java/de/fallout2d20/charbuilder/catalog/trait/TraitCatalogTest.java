package de.fallout2d20.charbuilder.catalog.trait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TraitCatalogTest {

  /** CRB p. 56 (Survivor traits) and WG p. 15 (Tribal traits), in book order. */
  @Test
  void loadsBothTraitListsInBookOrder() {
    assertEquals(List.of("Educated", "Fast Shot", "Gifted", "Heavy Handed", "Small Frame"),
        names(TraitCatalog.inList("survivor")));
    assertEquals(List.of("Mother Wasteland", "Nomad", "Rite of Passage", "Tools of the Old World",
        "The Chosen One"), names(TraitCatalog.inList("tribal")));
    assertEquals(10, TraitCatalog.all().size());
    for (TraitDefinition trait : TraitCatalog.inList("survivor")) {
      assertEquals(List.of(new Source(Book.CRB, 56)), trait.sources(), trait.id());
    }
    for (TraitDefinition trait : TraitCatalog.inList("tribal")) {
      assertEquals(List.of(new Source(Book.WG, 15)), trait.sources(), trait.id());
    }
  }

  @Test
  void effectFieldsMatchTheBooks() {
    assertEquals(1, trait("educated").extraTagSkills());
    assertEquals(2, trait("gifted").attributeChoices());
    assertEquals(1, trait("gifted").luckPointPenalty());
    assertEquals(1, trait("heavy-handed").meleeBonusDice());
    assertEquals(5, trait("small-frame").carryWeightPerStrength());
    assertEquals(List.of(Skill.SCIENCE), trait("nomad").forbiddenTagSkills());
    assertEquals(Set.of("educated"), idsWhere(trait -> trait.extraTagSkills() > 0));
    assertEquals(Set.of("gifted"), idsWhere(trait -> trait.attributeChoices() > 0 || trait.luckPointPenalty() > 0));
    assertEquals(Set.of("heavy-handed"), idsWhere(trait -> trait.meleeBonusDice() > 0));
    assertEquals(Set.of("small-frame"), idsWhere(trait -> trait.carryWeightPerStrength() != null));
    assertEquals(Set.of("nomad"), idsWhere(trait -> !trait.forbiddenTagSkills().isEmpty()));
  }

  /** Every trait list an origin offers exists in the trait catalog, and vice versa. */
  @Test
  void traitListsMatchTheOrigins() {
    Set<String> originLists = OriginCatalog.all().stream().map(OriginData::traitLists)
        .flatMap(List::stream).collect(Collectors.toSet());
    Set<String> traitLists = TraitCatalog.all().stream().map(TraitDefinition::list)
        .collect(Collectors.toSet());
    assertEquals(originLists, traitLists);
  }

  @Test
  void unknownIdsAreEmpty() {
    assertTrue(TraitCatalog.byId("good-natured").isEmpty());
  }

  private static TraitDefinition trait(String id) {
    return TraitCatalog.byId(id).orElseThrow();
  }

  private static List<String> names(List<TraitDefinition> traits) {
    return traits.stream().map(TraitDefinition::name).toList();
  }

  private static Set<String> idsWhere(Predicate<TraitDefinition> condition) {
    return TraitCatalog.all().stream().filter(condition).map(TraitDefinition::id)
        .collect(Collectors.toSet());
  }
}
