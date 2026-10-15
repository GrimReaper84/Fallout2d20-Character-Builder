package de.fallout2d20.charbuilder.catalog.origin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OriginCatalogTest {

  /** Printed start pages, checked against the books (CRB p. 51–57, WOA p. 18–24, WG p. 10–15). */
  private static final Map<String, List<Source>> EXPECTED_SOURCES = Map.ofEntries(
      Map.entry("brotherhood-initiate", List.of(new Source(Book.CRB, 51))),
      Map.entry("ghoul", List.of(new Source(Book.CRB, 52))),
      Map.entry("super-mutant", List.of(new Source(Book.CRB, 53))),
      Map.entry("mister-handy", List.of(new Source(Book.CRB, 54))),
      Map.entry("survivor", List.of(new Source(Book.CRB, 56))),
      Map.entry("vault-dweller", List.of(new Source(Book.CRB, 57))),
      Map.entry("generation-3-synth", List.of(new Source(Book.WOA, 18))),
      Map.entry("protectron", List.of(new Source(Book.WOA, 20))),
      Map.entry("child-of-atom", List.of(new Source(Book.WOA, 24), new Source(Book.WG, 13))),
      Map.entry("assaultron", List.of(new Source(Book.WG, 10))),
      Map.entry("brotherhood-outcast", List.of(new Source(Book.WG, 12))),
      Map.entry("nightkin", List.of(new Source(Book.WG, 14))),
      Map.entry("tribal", List.of(new Source(Book.WG, 15))));

  @Test
  void loadsThirteenOriginsWithTheirBookPages() {
    List<OriginData> origins = OriginCatalog.all();
    assertEquals(13, origins.size());
    Map<String, List<Source>> actual = origins.stream()
        .collect(Collectors.toMap(OriginData::id, OriginData::sources));
    assertEquals(EXPECTED_SOURCES, actual);
  }

  @Test
  void survivorAndTribalOfferTraitListsInsteadOfOneTrait() {
    assertNull(OriginCatalog.byId("survivor").orElseThrow().traitName());
    assertNull(OriginCatalog.byId("tribal").orElseThrow().traitName());
    assertEquals("The Chain that Breaks", OriginCatalog.byId("brotherhood-outcast").orElseThrow().traitName());
  }

  @Test
  void unknownIdsAreEmpty() {
    assertTrue(OriginCatalog.byId("legacy-ncr").isEmpty());
  }

  /** Child of Atom is described in WOA and WG and is available with either book. */
  @Test
  void availabilityFollowsTheEnabledBooks() {
    assertEquals(6, OriginCatalog.available(EnumSet.of(Book.CRB)).size());
    List<OriginData> withWoa = OriginCatalog.available(EnumSet.of(Book.CRB, Book.WOA));
    List<OriginData> withWg = OriginCatalog.available(EnumSet.of(Book.CRB, Book.WG));
    assertEquals(9, withWoa.size());
    assertEquals(11, withWg.size());
    assertTrue(ids(withWoa).contains("child-of-atom"));
    assertTrue(ids(withWg).contains("child-of-atom"));
    assertEquals(13, OriginCatalog.available(EnumSet.allOf(Book.class)).size());
  }

  /** The effects the derived statistics use (see derived_stats plan, section 4). */
  @Test
  void effectFieldsMatchTheBooks() {
    Map<String, Integer> fixedCarry = OriginCatalog.all().stream()
        .filter(origin -> origin.fixedCarryWeight() != null)
        .collect(Collectors.toMap(OriginData::id, OriginData::fixedCarryWeight));
    assertEquals(Map.of("mister-handy", 150, "protectron", 225, "assaultron", 150), fixedCarry);
    assertEquals(Set.of("mister-handy", "protectron", "assaultron"), idsWhere(OriginData::robot));
    assertEquals(Set.of("super-mutant", "mister-handy", "generation-3-synth", "protectron",
        "assaultron", "nightkin"), idsWhere(OriginData::poisonImmune));
    Set<String> radiationImmune = idsWhere(OriginData::radiationImmune);
    assertTrue(radiationImmune.containsAll(idsWhere(OriginData::poisonImmune)));
    assertTrue(radiationImmune.contains("ghoul"));
    assertEquals(7, radiationImmune.size());
    assertEquals(Set.of("child-of-atom"), idsWhere(origin -> origin.baseRadiationDr() > 0));
    assertEquals(1, OriginCatalog.byId("child-of-atom").orElseThrow().baseRadiationDr());
  }

  /** Creation rules: CRB p. 51–58, WOA p. 19–25, WG p. 12–15. */
  @Test
  void creationFieldsMatchTheBooks() {
    Map<Attribute, Integer> mutantModifiers = Map.of(Attribute.STRENGTH, 2, Attribute.ENDURANCE, 2);
    for (String id : List.of("super-mutant", "nightkin")) {
      OriginData mutant = origin(id);
      assertEquals(mutantModifiers, mutant.attributeModifiers(), id);
      assertEquals(12, mutant.attributeMaximum(Attribute.STRENGTH), id);
      assertEquals(12, mutant.attributeMaximum(Attribute.ENDURANCE), id);
      assertEquals(4, mutant.skillRankCap(), id);
    }
    assertEquals(6, origin("super-mutant").attributeMaximum(Attribute.INTELLIGENCE));
    assertEquals(6, origin("super-mutant").attributeMaximum(Attribute.CHARISMA));
    assertEquals(8, origin("nightkin").attributeMaximum(Attribute.INTELLIGENCE));
    assertEquals(8, origin("nightkin").attributeMaximum(Attribute.CHARISMA));
    assertEquals(Set.of("super-mutant", "nightkin"),
        idsWhere(origin -> !origin.attributeModifiers().isEmpty() || origin.skillRankCap() != null));

    List<Skill> scienceTags = List.of(Skill.ENERGY_WEAPONS, Skill.SCIENCE, Skill.REPAIR);
    assertEquals(scienceTags, origin("brotherhood-initiate").extraTagSkill().options());
    assertEquals(scienceTags, origin("brotherhood-outcast").extraTagSkill().options());
    assertEquals(List.of(), origin("vault-dweller").extraTagSkill().options());
    assertEquals(List.of(), origin("generation-3-synth").extraTagSkill().options());
    assertEquals(Set.of("brotherhood-initiate", "brotherhood-outcast", "vault-dweller",
        "generation-3-synth"), idsWhere(origin -> origin.extraTagSkill() != null));
    assertEquals(Skill.SURVIVAL, origin("ghoul").fixedTagSkill());
    assertEquals(Set.of("ghoul"), idsWhere(origin -> origin.fixedTagSkill() != null));

    assertEquals(List.of("survivor"), origin("survivor").traitLists());
    assertEquals(List.of("tribal", "survivor"), origin("tribal").traitLists());
    assertEquals(Set.of("survivor", "tribal"), idsWhere(origin -> !origin.traitLists().isEmpty()));
    assertEquals(Set.of("child-of-atom"), idsWhere(origin -> origin.extraPerks() == 1));
    assertEquals(List.of("Fire Brigadier", "Law Enforcer", "Construction Worker", "Medical Responder"),
        origin("protectron").purposeSuggestions());
    assertEquals(Set.of("protectron"), idsWhere(origin -> !origin.purposeSuggestions().isEmpty()));
  }

  private static OriginData origin(String id) {
    return OriginCatalog.byId(id).orElseThrow();
  }

  private static Set<String> ids(List<OriginData> origins) {
    return origins.stream().map(OriginData::id).collect(Collectors.toSet());
  }

  private static Set<String> idsWhere(Predicate<OriginData> condition) {
    return OriginCatalog.all().stream().filter(condition).map(OriginData::id)
        .collect(Collectors.toSet());
  }
}
