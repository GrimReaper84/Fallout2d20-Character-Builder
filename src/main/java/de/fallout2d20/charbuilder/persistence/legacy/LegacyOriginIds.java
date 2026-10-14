package de.fallout2d20.charbuilder.persistence.legacy;

import de.fallout2d20.charbuilder.model.character.CatalogRef;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Mapping of the internal origin ids of the legacy app (v0.4.5, {@code data/origins.ts}) to
 * catalog ids derived from the printed origin names (CRB p. 51 ff., WOA p. 18 ff., WG p. 10 ff.).
 * All other origins get the id {@code legacy-<id>}.
 */
final class LegacyOriginIds {

  private static final Map<String, String> SUPPORTED_BOOK_ORIGINS = Map.ofEntries(
      Map.entry("Survivor", "survivor"),
      Map.entry("Vault Dweller", "vault-dweller"),
      Map.entry("Brotherhood", "brotherhood-initiate"),
      Map.entry("Ghoul", "ghoul"),
      Map.entry("Super Mutant", "super-mutant"),
      Map.entry("Mister Handy", "mister-handy"),
      Map.entry("Gen3Synth", "generation-3-synth"),
      Map.entry("Protectron", "protectron"),
      Map.entry("ChildOfAtom", "child-of-atom"),
      Map.entry("Tribal", "tribal"),
      Map.entry("BrotherhoodOutcast", "brotherhood-outcast"),
      Map.entry("Nightkin", "nightkin"));

  /** Legacy origins from books that are not supported (Settler's Guide, Royal Flush, legacy only). */
  private static final Set<String> UNSUPPORTED_BOOK_ORIGINS = Set.of("Minuteman", "NCR",
      "Robobrain", "Securitron", "EnclaveRemnant", "FollowerApocalypse", "Viper", "Gen2Synth");

  private LegacyOriginIds() {}

  /** Catalog id of an origin from one of the supported books. */
  static Optional<String> supportedId(String legacyId) {
    return Optional.ofNullable(SUPPORTED_BOOK_ORIGINS.get(legacyId));
  }

  static boolean isFromUnsupportedBook(String legacyId) {
    return UNSUPPORTED_BOOK_ORIGINS.contains(legacyId);
  }

  /** Id for an origin that is not supported or unknown; camel case is split into words. */
  static String legacyId(String legacyId) {
    return "legacy-" + CatalogRef.toId(legacyId.replaceAll("([a-z])([A-Z0-9])", "$1 $2"));
  }
}
