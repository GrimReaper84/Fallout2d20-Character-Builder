package de.fallout2d20.charbuilder.model.party;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** The GM's party overview (H2): a name and the members in the GM's order, saved as a party file. */
public final class Party {

  private @NonNull String name;
  private final @NonNull List<PartyMember> members;
  private final @Nullable String createdWith;

  /**
   * Creates a party with these members.
   *
   * @param createdWith the app version that created the party; null if unknown
   */
  public Party(@NonNull String name, @NonNull List<PartyMember> members, @Nullable String createdWith) {
    this.name = name;
    this.members = new ArrayList<>(members);
    this.createdWith = createdWith;
  }

  /** An empty party created with this app version. */
  public static @NonNull Party createNew(@NonNull String appVersion) {
    return new Party("", List.of(), appVersion);
  }

  /** The app version that created the party; null if unknown. */
  public @Nullable String getCreatedWith() {
    return createdWith;
  }

  public @NonNull String getName() {
    return name;
  }

  public void setName(@NonNull String name) {
    this.name = name;
  }

  /** The members in order (a copy). */
  public @NonNull List<PartyMember> members() {
    return List.copyOf(members);
  }

  /** Adds the member at the end and returns its index. */
  public int add(@NonNull PartyMember member) {
    members.add(member);
    return members.size() - 1;
  }

  /**
   * Replaces the member at the index.
   *
   * @throws IndexOutOfBoundsException for an index outside the members
   */
  public void set(int index, @NonNull PartyMember member) {
    members.set(index, member);
  }

  /**
   * Removes the member at the index.
   *
   * @throws IndexOutOfBoundsException for an index outside the members
   */
  public void remove(int index) {
    members.remove(index);
  }

  /** Replaces the members in the given order (the table's sort order is kept when saving). */
  public void reorder(@NonNull List<PartyMember> ordered) {
    members.clear();
    members.addAll(ordered);
  }
}
