package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.party.Party;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** The party of the GM compendium tab (H2): the party, its file and whether it has unsaved changes. */
final class PartyDocument {

  private final @NonNull Party party;
  private @Nullable Path file;
  private boolean dirty;

  private PartyDocument(@NonNull Party party, @Nullable Path file) {
    this.party = party;
    this.file = file == null ? null : file.toAbsolutePath().normalize();
  }

  /** A new party without file. */
  static @NonNull PartyDocument created(@NonNull Party party) {
    return new PartyDocument(party, null);
  }

  /** A party loaded from its file. */
  static @NonNull PartyDocument loaded(@NonNull Party party, @NonNull Path file) {
    return new PartyDocument(party, file);
  }

  @NonNull Party party() {
    return party;
  }

  @Nullable Path file() {
    return file;
  }

  boolean isDirty() {
    return dirty;
  }

  void markDirty() {
    dirty = true;
  }

  void markSaved(@NonNull Path target) {
    file = target.toAbsolutePath().normalize();
    dirty = false;
  }

  /** The party's name, or "Party". */
  @NonNull String displayName() {
    return party.getName().isBlank() ? "Party" : party.getName();
  }

  /** The tab and window title, e.g. "GM – Commonwealth campaign *"; just "GM" for an unnamed party. */
  @NonNull String title() {
    return "GM" + (party.getName().isBlank() ? "" : " – " + party.getName()) + (dirty ? " *" : "");
  }
}
