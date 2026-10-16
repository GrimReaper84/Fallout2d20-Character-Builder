package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.model.character.creation.HintArea;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** The tabs of the character view, in display order, with the hint area each one shows. */
enum CharacterTab {
  STATS("Stats", HintArea.STATS),
  STATUS("Status", null),
  PERKS("Perks", HintArea.PERKS),
  WEAPONS("Weapons", null),
  ARMOR("Armor", HintArea.ARMOR),
  GEAR("Gear", HintArea.GEAR);

  private final @NonNull String title;
  private final @Nullable HintArea area;

  CharacterTab(@NonNull String title, @Nullable HintArea area) {
    this.title = title;
    this.area = area;
  }

  @NonNull String title() {
    return title;
  }

  @Nullable HintArea area() {
    return area;
  }

  /** Where a hint of the area is shown: the tab's title, or "Header" for name and origin. */
  static @NonNull String titleOf(@NonNull HintArea area) {
    return switch (area) {
      case HEADER -> "Header";
      case STATS -> STATS.title;
      case PERKS -> PERKS.title;
      case ARMOR -> ARMOR.title;
      case GEAR -> GEAR.title;
    };
  }
}
