package de.fallout2d20.charbuilder.persistence.party;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The party file (H2): {@code format} marks it as a party of this app, {@code formatVersion} is the structure version
 * (raised only on incompatible changes), {@code createdWith} and {@code savedWith} the app versions.
 *
 * @param members in the GM's order; null reads as none
 */
record PartyData(@Nullable String format, int formatVersion, @Nullable String createdWith, @Nullable String savedWith,
    @Nullable String name, @Nullable List<PartyMemberData> members) {

  /** The {@code format} marker of a party file. */
  static final @NonNull String FORMAT = "fallout2d20-party";
  /** The current party file version; every version written by a release must stay readable. */
  static final int FORMAT_VERSION = 1;
}
