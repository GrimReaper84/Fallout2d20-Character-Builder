package de.fallout2d20.charbuilder.catalog.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import de.fallout2d20.charbuilder.catalog.origin.OriginCatalog;
import de.fallout2d20.charbuilder.model.character.Skill;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EquipmentCatalogTest {

  private static final Set<Book> ALL_BOOKS = EnumSet.allOf(Book.class);
  private static final List<Source> CRB = List.of(new Source(Book.CRB, 81));
  private static final List<ArmAttachment> ANY_ARM = List.of(ArmAttachment.values());

  @Test
  void loadsCoreAndWinterOfAtomPacksForKnownOrigins() {
    List<EquipmentPackData> packs = EquipmentCatalog.packs();
    assertEquals(26, packs.size());
    assertEquals(16, EquipmentCatalog.packs().stream().filter(pack -> pack.isAvailableWith(Set.of(Book.CRB))).count());
    for (EquipmentPackData pack : packs) {
      assertEquals(1, pack.sources().size(), pack.id());
      for (String origin : pack.origins()) {
        assertTrue(OriginCatalog.byId(origin).isPresent(), pack.id() + ": " + origin);
      }
      for (String origin : pack.originsWithGmPermission()) {
        assertTrue(OriginCatalog.byId(origin).isPresent(), pack.id() + ": " + origin);
      }
    }
  }

  /** CRB pp. 76–80 per origin; CRB p. 57: ghouls may also take the Vault Dweller packs. */
  @Test
  void offersThePacksOfEachOrigin() {
    assertEquals(List.of("brotherhood-of-steel-initiate", "brotherhood-of-steel-scribe"),
        packIds("brotherhood-initiate"));
    assertEquals(List.of("miss-nanny", "mister-farmhand", "mister-gutsy", "mister-handy", "nurse-handy"),
        packIds("mister-handy"));
    assertEquals(List.of("brute", "skirmisher"), packIds("super-mutant"));
    assertEquals(List.of("vault-tec-resident", "vault-tec-security"), packIds("vault-dweller"));
    List<String> wastelander = List.of("mercenary", "raider", "settler", "trader", "wanderer");
    assertEquals(wastelander, packIds("survivor"));
    assertEquals(List.of("vault-tec-resident", "vault-tec-security", "mercenary", "raider", "settler",
        "trader", "wanderer"), packIds("ghoul"));
    assertTrue(pack("vault-tec-resident").needsGmPermission("ghoul"));
    assertFalse(pack("settler").needsGmPermission("ghoul"));
    assertEquals(List.of(), EquipmentCatalog.packsFor("survivor", Set.of(Book.WOA)));
  }

  /** WOA pp. 20, 22–24, 26: two packs per Gen 3 Synth and Child of Atom, six for a Protectron. */
  @Test
  void offersTheWinterOfAtomPacks() {
    assertEquals(List.of("synth-infiltrator", "synth-seeker"), packIds("generation-3-synth"));
    assertEquals(List.of("protectron", "protectron-fire-brigadier", "protectron-medic", "utility-protectron",
        "nukatron", "protectron-x"), packIds("protectron"));
    assertEquals(List.of("missionary", "zealot"), packIds("child-of-atom"));
    assertEquals(List.of(), EquipmentCatalog.packsFor("protectron", Set.of(Book.CRB)), "needs WOA");
    assertEquals(20, pack("protectron").caps());
    assertEquals(10, pack("protectron-fire-brigadier").caps());
    assertEquals(10, pack("missionary").caps());
    assertTrue(pack("synth-infiltrator").trinket());
    assertEquals(4, pack("protectron-x").choices().size());
    assertFalse(pack("utility-protectron").items().contains("Standard plating"), "WOA p. 23 lists no plating");
  }

  /** CRB pp. 76–80: caps and personal trinkets per pack. */
  @Test
  void capsAndTrinketsFollowTheBook() {
    Map<String, Integer> caps = EquipmentCatalog.packs().stream()
        .collect(Collectors.toMap(EquipmentPackData::id, EquipmentPackData::caps));
    assertEquals(Map.ofEntries(Map.entry("brotherhood-of-steel-initiate", 0),
        Map.entry("brotherhood-of-steel-scribe", 0), Map.entry("miss-nanny", 10),
        Map.entry("mister-farmhand", 25), Map.entry("mister-gutsy", 10), Map.entry("mister-handy", 10),
        Map.entry("nurse-handy", 10), Map.entry("brute", 5), Map.entry("skirmisher", 5),
        Map.entry("vault-tec-resident", 10), Map.entry("vault-tec-security", 0),
        Map.entry("mercenary", 15), Map.entry("raider", 15), Map.entry("settler", 45),
        Map.entry("trader", 50), Map.entry("wanderer", 30)), caps.entrySet().stream()
            .filter(entry -> pack(entry.getKey()).isAvailableWith(Set.of(Book.CRB)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    assertEquals(Set.of("brute", "skirmisher", "settler", "trader", "wanderer", "synth-infiltrator"),
        EquipmentCatalog.packs().stream().filter(EquipmentPackData::trinket)
            .map(EquipmentPackData::id).collect(Collectors.toSet()));
  }

  @Test
  void packChoicesFollowTheBook() {
    assertEquals(List.of("Switchblade", "Pipe wrench", "Rolling pin", "Knuckles"),
        pack("settler").choices().getFirst().options());
    assertEquals(3, pack("mercenary").choices().size());
    assertEquals(3, pack("raider").choices().size());
    assertEquals(List.of(), pack("vault-tec-security").choices());
  }

  /** CRB p. 77: arms per Mister Handy pack; only Mister Handy packs have arms. */
  @Test
  void misterHandyPacksDefineTheirArms() {
    assertEquals(List.of(List.of(ArmAttachment.AUTO_PISTOL_10MM), List.of(ArmAttachment.BUZZ_SAW),
        List.of(ArmAttachment.LASER_EMITTER)), armOptions("mister-gutsy"));
    assertEquals(List.of(List.of(ArmAttachment.PINCER), List.of(ArmAttachment.FLAMER), ANY_ARM),
        armOptions("miss-nanny"));
    assertEquals(List.of(List.of(ArmAttachment.PINCER, ArmAttachment.BUZZ_SAW),
        List.of(ArmAttachment.BUZZ_SAW), ANY_ARM), armOptions("nurse-handy"));
    for (EquipmentPackData pack : EquipmentCatalog.packs()) {
      assertEquals(pack.origins().equals(List.of("mister-handy")), !pack.arms().isEmpty(), pack.id());
    }
  }

  /** CRB p. 81: one row per skill; three rows offer a choice; Barter grants 2d20 caps. */
  @Test
  void tagSkillItemsFollowTheBook() {
    assertEquals(Set.of(Skill.EXPLOSIVES, Skill.MELEE_WEAPONS, Skill.THROWING),
        Arrays.stream(Skill.values()).filter(skill -> EquipmentCatalog.tagSkillItems(skill).isChoice())
            .collect(Collectors.toSet()));
    TagSkillItemData barter = EquipmentCatalog.tagSkillItems(Skill.BARTER);
    assertEquals(2, barter.capsD20());
    assertEquals(List.of(), barter.options());
    assertEquals(List.of("4 + 2 CD bobby pins"), EquipmentCatalog.tagSkillItems(Skill.LOCKPICK).options());
    assertEquals(List.of("Knuckles"), EquipmentCatalog.tagSkillItems(Skill.UNARMED).options());
    for (Skill skill : Skill.values()) {
      TagSkillItemData row = EquipmentCatalog.tagSkillItems(skill);
      assertEquals(skill, row.skill());
      assertEquals(skill == Skill.BARTER ? 2 : 0, row.capsD20(), skill.name());
    }
  }

  /** CRB p. 80: d20 trinket table. */
  @Test
  void trinketTableCoversEveryRoll() {
    assertEquals("A gold pocket watch", EquipmentCatalog.trinket(1).name());
    assertEquals("Wedding ring", EquipmentCatalog.trinket(8).name());
    assertEquals("A pre-war neck-tie", EquipmentCatalog.trinket(20).name());
    assertThrows(IllegalArgumentException.class, () -> EquipmentCatalog.trinket(0));
    assertThrows(IllegalArgumentException.class, () -> EquipmentCatalog.trinket(21));
  }

  @Test
  void validatesTagSkillItemsAndTrinketRows() {
    assertThrows(IllegalArgumentException.class,
        () -> new TagSkillItemData(Skill.BARTER, List.of(), 0, CRB));
    List<String> multiTool = List.of("Multi-Tool");
    assertThrows(IllegalArgumentException.class,
        () -> new TagSkillItemData(Skill.REPAIR, multiTool, 0, List.of()));
    assertThrows(IllegalArgumentException.class,
        () -> new TagSkillItemData(Skill.REPAIR, multiTool, -1, CRB));
    assertFalse(new TagSkillItemData(Skill.BARTER, null, 2, CRB).isChoice());
    assertTrue(new TagSkillItemData(Skill.MELEE_WEAPONS, List.of("Machete", "Baseball bat"), 0, CRB)
        .isChoice());
    Source source = CRB.getFirst();
    assertThrows(IllegalArgumentException.class, () -> new TrinketData(0, "Medal", source));
    assertThrows(IllegalArgumentException.class, () -> new TrinketData(21, "Medal", source));
    assertThrows(IllegalArgumentException.class, () -> new TrinketData(5, " ", source));
  }

  private static List<String> packIds(String originId) {
    return EquipmentCatalog.packsFor(originId, ALL_BOOKS).stream().map(EquipmentPackData::id).toList();
  }

  private static EquipmentPackData pack(String id) {
    return EquipmentCatalog.packById(id).orElseThrow();
  }

  private static List<List<ArmAttachment>> armOptions(String packId) {
    return pack(packId).arms().stream().map(ArmSlot::options).toList();
  }
}
