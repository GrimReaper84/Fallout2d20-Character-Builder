package de.fallout2d20.charbuilder.persistence.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.perk.PerkCatalog;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.ExperienceTable;
import de.fallout2d20.charbuilder.model.character.LegacyBaseline;
import de.fallout2d20.charbuilder.model.character.OriginChoices;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.Vitals;
import de.fallout2d20.charbuilder.model.character.derived.DerivedStats;
import de.fallout2d20.charbuilder.model.character.derived.Resistance;
import de.fallout2d20.charbuilder.model.character.equipment.StartingEquipment;
import de.fallout2d20.charbuilder.persistence.CharacterFile;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** State and steps of one legacy import, see {@link LegacyCharacterImporter}. */
final class LegacyImportRun {

  private static final String DEFAULT_NAME = "Wanderer";
  private static final int MAX_RADIATION_POINTS = 5;
  // Top-level fields of the legacy save format.
  private static final String ORIGIN = "origin";
  private static final String ATTRIBUTES = "attributes";
  private static final String SKILLS = "skills";
  private static final String PERKS = "perks";
  private static final String TRAITS = "traits";
  private static final String VITALS = "vitals";
  private static final String DERIVED = "derived";
  private static final Set<String> CONSUMED_FIELDS = Set.of("schemaVersion", "name", ORIGIN,
      "level", "xp", ATTRIBUTES, SKILLS, PERKS, TRAITS, "progression", VITALS, DERIVED);
  private static final Pattern MELEE_DICE = Pattern.compile("\\+(\\d+)");

  private final ObjectNode root;
  private final Clock clock;
  private final List<ImportWarning> warnings = new ArrayList<>();
  private final ObjectNode payload = JsonNodeFactory.instance.objectNode();

  LegacyImportRun(ObjectNode root, Clock clock) {
    this.root = root;
    this.clock = clock;
  }

  CharacterFile execute() throws SaveFormatException {
    int sourceVersion = sourceVersion();
    JsonNode originNode = root.get(ORIGIN);
    if (originNode == null || !originNode.isTextual() || !CatalogRef.canDeriveId(originNode.asText())) {
      throw new SaveFormatException("Legacy file has no valid origin");
    }
    JsonNode attributesNode = root.get(ATTRIBUTES);
    if (attributesNode == null || !attributesNode.isObject()) {
      throw new SaveFormatException("Legacy file has no attributes");
    }
    String name = name();
    CatalogRef origin = origin(originNode.asText());
    int level = intField(root, "level", 1, 1);
    long xp = intField(root, "xp", 0, 0);
    if (ExperienceTable.levelForXp(xp) != level) {
      info("xp", "XP " + xp + " does not match level " + level
          + " (allowed with milestone levelling); both kept.");
    }
    Map<Attribute, Integer> attributes = attributes(attributesNode);
    Map<Skill, SkillState> skillStates = new EnumMap<>(Skill.class);
    Map<Skill, Integer> skillRanks = skills(skillStates);
    List<PerkSelection> perks = perks();
    List<TraitSelection> traits = traits();
    Progression progression = progression();
    Vitals vitals = vitals();
    List<String> kept = keepRemainingFields();
    LegacyEquipmentImport equipment = new LegacyEquipmentImport(payload, warnings);
    StartingEquipment startingEquipment = equipment.startingEquipment(origin);
    String inventoryNotes = equipment.inventoryNotes();
    reportKeptFields(kept);
    // Legacy saves know no book selection: all supported books are enabled.
    Character character = new Character(name, origin, OriginChoices.NONE, level, xp, true,
        EnumSet.allOf(Book.class), Map.of(), skillStates, traits, perks, progression, vitals,
        new LegacyBaseline(attributes, skillRanks),
        new LegacyImport(sourceVersion, LocalDate.now(clock).toString(), payload),
        startingEquipment, inventoryNotes);
    compareDerived(character);
    return new CharacterFile(character, new ImportReport(sourceVersion, warnings));
  }

  private int sourceVersion() throws SaveFormatException {
    JsonNode version = root.get("schemaVersion");
    if (version == null || version.isNull()) {
      return 1;
    }
    if (version.isInt() && version.asInt() == 2) {
      return 2;
    }
    throw new SaveFormatException("Unsupported save version: " + version);
  }

  private String name() {
    JsonNode node = root.get("name");
    if (node != null && node.isTextual() && !node.asText().isBlank()) {
      return node.asText();
    }
    info("name", "No name found; using \"" + DEFAULT_NAME + "\".");
    return DEFAULT_NAME;
  }

  private CatalogRef origin(String legacyId) {
    Optional<String> supported = LegacyOriginIds.supportedId(legacyId);
    if (supported.isPresent()) {
      return new CatalogRef(supported.get(), legacyId);
    }
    if (LegacyOriginIds.isFromUnsupportedBook(legacyId)) {
      info(ORIGIN, "Origin \"" + legacyId + "\" is from a book that is not supported; "
          + "kept as unverified legacy entry.");
    } else {
      warning(ORIGIN, "Unknown origin \"" + legacyId + "\"; kept as unverified legacy entry.");
    }
    return new CatalogRef(LegacyOriginIds.legacyId(legacyId), legacyId);
  }

  private Map<Attribute, Integer> attributes(JsonNode node) {
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      JsonNode value = node.get(attribute.id());
      Integer parsed = value == null ? null : parseInt(value);
      if (parsed == null) {
        warning(ATTRIBUTES, attribute.displayName() + " is missing or invalid; using "
            + Character.ATTRIBUTE_BASE + ".");
        parsed = Character.ATTRIBUTE_BASE;
      } else if (parsed < 4 || parsed > 10) {
        info(ATTRIBUTES, attribute.displayName() + " is " + parsed
            + ", outside the usual range 4–10.");
      }
      attributes.put(attribute, parsed);
    }
    ObjectNode unknown = JsonNodeFactory.instance.objectNode();
    for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = it.next();
      if (Attribute.fromId(entry.getKey()).isEmpty()) {
        unknown.set(entry.getKey(), entry.getValue());
      }
    }
    if (!unknown.isEmpty()) {
      payload.set("unknownAttributes", unknown);
      warning(ATTRIBUTES, "Unknown attributes kept in the legacy data: " + fieldNames(unknown));
    }
    return attributes;
  }

  private @NonNull Map<Skill, Integer> skills(@NonNull Map<Skill, SkillState> states) {
    Map<Skill, Integer> ranks = new EnumMap<>(Skill.class);
    JsonNode node = root.get(SKILLS);
    ObjectNode unknown = JsonNodeFactory.instance.objectNode();
    if (node != null && node.isObject()) {
      for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
        readSkill(it.next(), ranks, states, unknown);
      }
    }
    if (!unknown.isEmpty()) {
      payload.set("unknownSkills", unknown);
      warning(SKILLS, "Unknown skills kept in the legacy data: " + fieldNames(unknown));
    }
    fillMissingSkills(ranks);
    return ranks;
  }

  /** Reads rank and Tag flag of one legacy skill entry; unknown skill names go to {@code unknown}. */
  private static void readSkill(Map.@NonNull Entry<String, JsonNode> entry,
      @NonNull Map<Skill, Integer> ranks, @NonNull Map<Skill, SkillState> states,
      @NonNull ObjectNode unknown) {
    Optional<Skill> skill = Skill.fromDisplayName(entry.getKey());
    if (skill.isEmpty()) {
      unknown.set(entry.getKey(), entry.getValue());
      return;
    }
    JsonNode rankNode = entry.getValue().get("rank");
    Integer rank = rankNode == null ? null : parseInt(rankNode);
    JsonNode tagNode = entry.getValue().get("tag");
    ranks.put(skill.get(), rank == null ? 0 : Math.max(0, rank));
    states.put(skill.get(), new SkillState(tagNode != null && tagNode.asBoolean(), 0, 0));
  }

  private void fillMissingSkills(@NonNull Map<Skill, Integer> ranks) {
    int missing = 0;
    for (Skill skill : Skill.values()) {
      if (!ranks.containsKey(skill)) {
        ranks.put(skill, 0);
        missing++;
      }
    }
    if (missing > 0) {
      info(SKILLS, missing + " skill(s) missing; imported with rank 0.");
    }
  }

  private @NonNull List<PerkSelection> perks() {
    Map<String, Integer> ranks = new LinkedHashMap<>();
    Map<String, List<Attribute>> choices = new LinkedHashMap<>();
    ArrayNode invalid = JsonNodeFactory.instance.arrayNode();
    ObjectNode invalidChoices = JsonNodeFactory.instance.objectNode();
    boolean merged = false;
    for (JsonNode item : elements(root.get(PERKS))) {
      String perkName = item.isTextual() ? item.asText() : textOrNull(item.get("name"));
      if (perkName == null || !CatalogRef.canDeriveId(perkName)) {
        invalid.add(item);
        continue;
      }
      merged |= ranks.containsKey(perkName);
      ranks.merge(perkName, perkRank(item), Integer::sum);
      collectChoices(perkName, item, choices.computeIfAbsent(perkName, k -> new ArrayList<>()),
          invalidChoices);
    }
    reportPerkProblems(merged, invalid, invalidChoices);
    List<PerkSelection> perks = new ArrayList<>();
    List<String> notInCatalog = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
      CatalogRef ref = CatalogRef.fromLegacyName(entry.getKey());
      if (PerkCatalog.byId(ref.id()).isEmpty()) {
        notInCatalog.add(entry.getKey());
      }
      perks.add(new PerkSelection(ref, entry.getValue(), entry.getValue(),
          choices.getOrDefault(entry.getKey(), List.of()), List.of()));
    }
    if (!notInCatalog.isEmpty()) {
      warning(PERKS, "Perks not in the supported books kept by their legacy name: "
          + String.join(", ", notInCatalog) + ".");
    }
    return perks;
  }

  /** Rank of a legacy perk entry; plain name strings and missing or invalid ranks count as 1. */
  private static int perkRank(@NonNull JsonNode item) {
    JsonNode rankNode = item.get("rank");
    Integer parsed = rankNode == null ? null : parseInt(rankNode);
    return parsed == null ? 1 : Math.max(1, parsed);
  }

  /** Attribute choices (Intense Training); unknown ones are kept per perk in {@code invalidChoices}. */
  private static void collectChoices(@NonNull String perkName, @NonNull JsonNode item,
      @NonNull List<Attribute> perkChoices, @NonNull ObjectNode invalidChoices) {
    for (JsonNode choice : elements(item.get("choices"))) {
      Optional<Attribute> attribute = Attribute.fromId(choice.asText());
      if (attribute.isPresent()) {
        perkChoices.add(attribute.get());
      } else {
        ArrayNode kept = invalidChoices.has(perkName)
            ? (ArrayNode) invalidChoices.get(perkName) : invalidChoices.putArray(perkName);
        kept.add(choice);
      }
    }
  }

  private void reportPerkProblems(boolean merged, @NonNull ArrayNode invalid,
      @NonNull ObjectNode invalidChoices) {
    if (merged) {
      info(PERKS, "Repeated perk entries were merged into ranks.");
    }
    if (!invalid.isEmpty()) {
      payload.set("invalidPerks", invalid);
      warning(PERKS, invalid.size() + " invalid perk entr(y/ies) kept in the legacy data.");
    }
    if (!invalidChoices.isEmpty()) {
      payload.set("invalidPerkChoices", invalidChoices);
      warning(PERKS, "Unknown attribute choices kept in the legacy data: " + fieldNames(invalidChoices));
    }
  }

  private List<TraitSelection> traits() {
    List<TraitSelection> traits = new ArrayList<>();
    ArrayNode invalid = JsonNodeFactory.instance.arrayNode();
    for (JsonNode item : elements(root.get(TRAITS))) {
      if (item.isTextual() && CatalogRef.canDeriveId(item.asText())) {
        traits.add(new TraitSelection(CatalogRef.fromLegacyName(item.asText()), true, List.of()));
      } else {
        invalid.add(item);
      }
    }
    if (!invalid.isEmpty()) {
      payload.set("invalidTraits", invalid);
      warning(TRAITS, invalid.size() + " invalid trait entr(y/ies) kept in the legacy data.");
    }
    return traits;
  }

  private Progression progression() {
    JsonNode node = root.get("progression");
    if (node == null || !node.isObject()) {
      return Progression.NONE;
    }
    return new Progression(intField(node, "pendingSkillRanks", 0, 0), intField(node, "pendingPerks", 0, 0));
  }

  private Vitals vitals() {
    JsonNode node = root.get(VITALS);
    if (node == null || !node.isObject()) {
      return Vitals.FULL;
    }
    Integer currentHp = parseNonNegative(node.get("currentHP"));
    if (currentHp == null) {
      JsonNode derived = root.get(DERIVED);
      currentHp = derived == null ? null : parseNonNegative(derived.get("hp"));
    }
    int radiationPoints = intField(node, "radiationPoints", 0, 0);
    if (radiationPoints > MAX_RADIATION_POINTS) {
      info(VITALS, "Radiation points " + radiationPoints + " reduced to " + MAX_RADIATION_POINTS + ".");
      radiationPoints = MAX_RADIATION_POINTS;
    }
    return new Vitals(currentHp, intField(node, "rads", 0, 0), radiationPoints);
  }

  /**
   * Compares the derived values the legacy app stored with the computed ones and reports all
   * differences in one info. Missing or unreadable values are skipped; the computed values win.
   */
  private void compareDerived(@NonNull Character character) {
    JsonNode derived = root.get(DERIVED);
    if (derived == null || !derived.isObject()) {
      return;
    }
    DerivedStats stats = DerivedStats.of(character);
    List<String> differences = new ArrayList<>();
    addDifference(differences, "HP", intOrNull(derived.get("hp")), stats.maxHp());
    addDifference(differences, "Initiative", intOrNull(derived.get("init")), stats.initiative());
    addDifference(differences, "Defense", intOrNull(derived.get("def")), stats.defense());
    addDifference(differences, "carry weight", intOrNull(derived.get("carry")), stats.carryWeight());
    addDifference(differences, "melee bonus dice", meleeDice(derived.get("melee")), stats.meleeBonusDice());
    addDifference(differences, "physical DR", resistance(derived.get("physDR")), stats.physical());
    addDifference(differences, "energy DR", resistance(derived.get("enDR")), stats.energy());
    addDifference(differences, "radiation DR", resistance(derived.get("radDR")), stats.radiation());
    addDifference(differences, "poison DR", resistance(derived.get("poisDR")), stats.poison());
    if (!differences.isEmpty()) {
      info(DERIVED, "Stored values differ from the rules and were recomputed: "
          + String.join(", ", differences) + ".");
    }
  }

  private static <T> void addDifference(@NonNull List<String> differences, @NonNull String label,
      @Nullable T stored, @NonNull T computed) {
    if (stored != null && !stored.equals(computed)) {
      differences.add(label + " " + stored + " → " + computed);
    }
  }

  private static @Nullable Integer intOrNull(@Nullable JsonNode node) {
    return node == null ? null : parseInt(node);
  }

  /**
   * Legacy melee bonus such as {@code "+1 CD"} or {@code "+1 CD (+1 CD Heavy Handed)"}: the sum of
   * all {@code +N} parts.
   */
  private static @Nullable Integer meleeDice(@Nullable JsonNode node) {
    if (node == null || !node.isTextual()) {
      return intOrNull(node);
    }
    Matcher matcher = MELEE_DICE.matcher(node.asText());
    Integer dice = null;
    while (matcher.find()) {
      dice = (dice == null ? 0 : dice) + Integer.parseInt(matcher.group(1));
    }
    return dice;
  }

  /** Legacy DR: a number, or {@code "Immune"} / {@code "Heals"} (ghouls) for immunity. */
  private static @Nullable Resistance resistance(@Nullable JsonNode node) {
    if (node != null && node.isTextual()
        && (node.asText().equalsIgnoreCase("Immune") || node.asText().equalsIgnoreCase("Heals"))) {
      return Resistance.IMMUNITY;
    }
    Integer value = parseNonNegative(node);
    return value == null ? null : Resistance.of(value);
  }

  /** Copies all fields no feature has taken over into the payload; returns their names. */
  private @NonNull List<String> keepRemainingFields() {
    List<String> kept = new ArrayList<>();
    for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = it.next();
      if (!CONSUMED_FIELDS.contains(entry.getKey())) {
        payload.set(entry.getKey(), entry.getValue().deepCopy());
        kept.add(entry.getKey());
      }
    }
    return kept;
  }

  /** Reports the kept fields that no feature has taken out of the payload. */
  private void reportKeptFields(@NonNull List<String> kept) {
    List<String> remaining = kept.stream().filter(payload::has).toList();
    if (!remaining.isEmpty()) {
      info("legacy data", "Kept for later features: " + String.join(", ", remaining) + ".");
    }
  }

  /** Lenient integer field like the legacy {@code Number(x) || default}, clamped to a minimum. */
  private int intField(JsonNode parent, String field, int defaultValue, int min) {
    JsonNode node = parent.get(field);
    Integer value = node == null ? null : parseInt(node);
    if (value == null) {
      if (node != null && !node.isNull()) {
        info(field, "Invalid value " + node + "; using " + defaultValue + ".");
      }
      return defaultValue;
    }
    if (value < min) {
      info(field, "Value " + value + " raised to " + min + ".");
      return min;
    }
    return value;
  }

  private void info(String field, String message) {
    warnings.add(new ImportWarning(WarningSeverity.INFO, field, message));
  }

  private void warning(String field, String message) {
    warnings.add(new ImportWarning(WarningSeverity.WARNING, field, message));
  }

  private static @Nullable Integer parseNonNegative(@Nullable JsonNode node) {
    Integer value = node == null ? null : parseInt(node);
    return value == null ? null : Math.max(0, value);
  }

  /** Parses numbers and numeric strings (truncating decimals), else {@code null}. */
  private static @Nullable Integer parseInt(JsonNode node) {
    if (node.isNumber()) {
      return (int) node.asDouble();
    }
    if (node.isTextual()) {
      try {
        return (int) Double.parseDouble(node.asText().trim());
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

  private static @Nullable String textOrNull(@Nullable JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }

  private static List<JsonNode> elements(@Nullable JsonNode node) {
    List<JsonNode> elements = new ArrayList<>();
    if (node != null && node.isArray()) {
      node.forEach(elements::add);
    }
    return elements;
  }

  private static String fieldNames(ObjectNode node) {
    List<String> names = new ArrayList<>();
    node.fieldNames().forEachRemaining(names::add);
    return String.join(", ", names);
  }
}
