package de.fallout2d20.charbuilder.persistence.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fallout2d20.charbuilder.model.character.Attribute;
import de.fallout2d20.charbuilder.model.character.CatalogRef;
import de.fallout2d20.charbuilder.model.character.Character;
import de.fallout2d20.charbuilder.model.character.ExperienceTable;
import de.fallout2d20.charbuilder.model.character.LegacyBaseline;
import de.fallout2d20.charbuilder.model.character.LegacyImport;
import de.fallout2d20.charbuilder.model.character.PerkSelection;
import de.fallout2d20.charbuilder.model.character.Progression;
import de.fallout2d20.charbuilder.model.character.Skill;
import de.fallout2d20.charbuilder.model.character.SkillState;
import de.fallout2d20.charbuilder.model.character.TraitSelection;
import de.fallout2d20.charbuilder.model.character.Vitals;
import de.fallout2d20.charbuilder.persistence.CharacterFile;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** State and steps of one legacy import, see {@link LegacyCharacterImporter}. */
final class LegacyImportRun {

  private static final String DEFAULT_NAME = "Wanderer";
  private static final int MAX_RADIATION_POINTS = 5;
  private static final Set<String> CONSUMED_FIELDS = Set.of("schemaVersion", "name", "origin",
      "level", "xp", "attributes", "skills", "perks", "traits", "progression", "vitals");

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
    JsonNode originNode = root.get("origin");
    if (originNode == null || !originNode.isTextual() || !CatalogRef.canDeriveId(originNode.asText())) {
      throw new SaveFormatException("Legacy file has no valid origin");
    }
    JsonNode attributesNode = root.get("attributes");
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
    keepRemainingFields();
    Character character = new Character(name, origin, level, xp, true, Map.of(), skillStates,
        traits, perks, progression, vitals, new LegacyBaseline(attributes, skillRanks),
        new LegacyImport(sourceVersion, LocalDate.now(clock).toString(), payload));
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
      info("origin", "Origin \"" + legacyId + "\" is from a book that is not supported; "
          + "kept as unverified legacy entry.");
    } else {
      warning("origin", "Unknown origin \"" + legacyId + "\"; kept as unverified legacy entry.");
    }
    return new CatalogRef(LegacyOriginIds.legacyId(legacyId), legacyId);
  }

  private Map<Attribute, Integer> attributes(JsonNode node) {
    Map<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
    for (Attribute attribute : Attribute.values()) {
      JsonNode value = node.get(attribute.id());
      Integer parsed = value == null ? null : parseInt(value);
      if (parsed == null) {
        warning("attributes", attribute.displayName() + " is missing or invalid; using "
            + Character.ATTRIBUTE_BASE + ".");
        parsed = Character.ATTRIBUTE_BASE;
      } else if (parsed < 4 || parsed > 10) {
        info("attributes", attribute.displayName() + " is " + parsed
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
      warning("attributes", "Unknown attributes kept in the legacy data: " + fieldNames(unknown));
    }
    return attributes;
  }

  private Map<Skill, Integer> skills(Map<Skill, SkillState> states) {
    Map<Skill, Integer> ranks = new EnumMap<>(Skill.class);
    JsonNode node = root.get("skills");
    ObjectNode unknown = JsonNodeFactory.instance.objectNode();
    if (node != null && node.isObject()) {
      for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = it.next();
        Optional<Skill> skill = Skill.fromDisplayName(entry.getKey());
        if (skill.isEmpty()) {
          unknown.set(entry.getKey(), entry.getValue());
          continue;
        }
        JsonNode rankNode = entry.getValue().get("rank");
        Integer rank = rankNode == null ? null : parseInt(rankNode);
        JsonNode tagNode = entry.getValue().get("tag");
        ranks.put(skill.get(), rank == null ? 0 : Math.max(0, rank));
        states.put(skill.get(), new SkillState(tagNode != null && tagNode.asBoolean(), 0, 0));
      }
    }
    if (!unknown.isEmpty()) {
      payload.set("unknownSkills", unknown);
      warning("skills", "Unknown skills kept in the legacy data: " + fieldNames(unknown));
    }
    int missing = 0;
    for (Skill skill : Skill.values()) {
      if (!ranks.containsKey(skill)) {
        ranks.put(skill, 0);
        missing++;
      }
    }
    if (missing > 0) {
      info("skills", missing + " skill(s) missing; imported with rank 0.");
    }
    return ranks;
  }

  private List<PerkSelection> perks() {
    Map<String, Integer> ranks = new LinkedHashMap<>();
    Map<String, List<Attribute>> choices = new LinkedHashMap<>();
    ArrayNode invalid = JsonNodeFactory.instance.arrayNode();
    ObjectNode invalidChoices = JsonNodeFactory.instance.objectNode();
    boolean merged = false;
    for (JsonNode item : elements(root.get("perks"))) {
      String perkName = item.isTextual() ? item.asText() : textOrNull(item.get("name"));
      if (perkName == null || !CatalogRef.canDeriveId(perkName)) {
        invalid.add(item);
        continue;
      }
      int rank = 1;
      if (item.isObject()) {
        JsonNode rankNode = item.get("rank");
        Integer parsed = rankNode == null ? null : parseInt(rankNode);
        rank = parsed == null ? 1 : Math.max(1, parsed);
      }
      merged |= ranks.containsKey(perkName);
      ranks.merge(perkName, rank, Integer::sum);
      List<Attribute> perkChoices = choices.computeIfAbsent(perkName, k -> new ArrayList<>());
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
    if (merged) {
      info("perks", "Repeated perk entries were merged into ranks.");
    }
    if (!invalid.isEmpty()) {
      payload.set("invalidPerks", invalid);
      warning("perks", invalid.size() + " invalid perk entr(y/ies) kept in the legacy data.");
    }
    if (!invalidChoices.isEmpty()) {
      payload.set("invalidPerkChoices", invalidChoices);
      warning("perks", "Unknown attribute choices kept in the legacy data: " + fieldNames(invalidChoices));
    }
    List<PerkSelection> perks = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
      perks.add(new PerkSelection(CatalogRef.fromLegacyName(entry.getKey()), entry.getValue(),
          entry.getValue(), choices.getOrDefault(entry.getKey(), List.of()), List.of()));
    }
    return perks;
  }

  private List<TraitSelection> traits() {
    List<TraitSelection> traits = new ArrayList<>();
    ArrayNode invalid = JsonNodeFactory.instance.arrayNode();
    for (JsonNode item : elements(root.get("traits"))) {
      if (item.isTextual() && CatalogRef.canDeriveId(item.asText())) {
        traits.add(new TraitSelection(CatalogRef.fromLegacyName(item.asText()), true, List.of()));
      } else {
        invalid.add(item);
      }
    }
    if (!invalid.isEmpty()) {
      payload.set("invalidTraits", invalid);
      warning("traits", invalid.size() + " invalid trait entr(y/ies) kept in the legacy data.");
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
    JsonNode node = root.get("vitals");
    if (node == null || !node.isObject()) {
      return Vitals.FULL;
    }
    Integer currentHp = parseNonNegative(node.get("currentHP"));
    if (currentHp == null) {
      JsonNode derived = root.get("derived");
      currentHp = derived == null ? null : parseNonNegative(derived.get("hp"));
    }
    int radiationPoints = intField(node, "radiationPoints", 0, 0);
    if (radiationPoints > MAX_RADIATION_POINTS) {
      info("vitals", "Radiation points " + radiationPoints + " reduced to " + MAX_RADIATION_POINTS + ".");
      radiationPoints = MAX_RADIATION_POINTS;
    }
    return new Vitals(currentHp, intField(node, "rads", 0, 0), radiationPoints);
  }

  private void keepRemainingFields() {
    List<String> kept = new ArrayList<>();
    for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = it.next();
      if (!CONSUMED_FIELDS.contains(entry.getKey())) {
        payload.set(entry.getKey(), entry.getValue().deepCopy());
        kept.add(entry.getKey());
      }
    }
    if (!kept.isEmpty()) {
      info("legacy data", "Kept for later features: " + String.join(", ", kept) + ".");
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
