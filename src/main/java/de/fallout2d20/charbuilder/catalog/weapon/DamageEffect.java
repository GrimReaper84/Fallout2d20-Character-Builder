package de.fallout2d20.charbuilder.catalog.weapon;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * The damage effects of the books (CRB p. 30, WG p. 56), with a short description in own words for tooltips.
 * Weapons keep their damage effects as printed text ({@code Piercing 2}, {@code Persistent (Poison)}); this enum
 * only reads that text.
 */
public enum DamageEffect {
  ARC("Arc", "Each Effect hits another target within Close range for half the damage (rounded up), without extra "
      + "ammunition.", Book.WG, 56),
  BREAKING("Breaking", "Each Effect removes a Cover Die from the target's cover for good, or else 1 DR of the "
      + "weapon's damage type on the location hit.", Book.CRB, 30),
  BURST("Burst", "Each Effect hits one more target within Close range, for 1 more round of ammunition each.",
      Book.CRB, 30),
  FREEZE("Freeze", "The target is frozen for its next turn if the Effects reach half its END (BODY for creatures).",
      Book.WG, 56),
  PERSISTENT("Persistent", "With Effects, the damage repeats at the end of the target's turns, for as many rounds "
      + "as Effects; a type in brackets changes the damage type.", Book.CRB, 30),
  PIERCING("Piercing", "Each Effect ignores X points of the target's DR.", Book.CRB, 30),
  RADIOACTIVE("Radioactive", "Each Effect also deals 1 radiation damage, applied after the attack.", Book.CRB, 30),
  SPREAD("Spread", "Each Effect adds a hit for half the damage (rounded down) on a random location.", Book.CRB, 30),
  STUN("Stun", "With Effects, the target cannot take its normal actions on its next turn.", Book.CRB, 30),
  VICIOUS("Vicious", "Each Effect adds +1 damage.", Book.CRB, 30);

  /** A rating or a type in brackets after the name, e.g. {@code 2} or {@code (Poison)}. */
  private static final @NonNull Pattern RATING_OR_TYPE = Pattern.compile("\\(.*\\)|\\d+");

  private final @NonNull String displayName;
  private final @NonNull String summary;
  private final @NonNull Book book;
  private final int page;

  DamageEffect(@NonNull String displayName, @NonNull String summary, @NonNull Book book, int page) {
    this.displayName = displayName;
    this.summary = summary;
    this.book = book;
    this.page = page;
  }

  public @NonNull String displayName() {
    return displayName;
  }

  /** One sentence in own words, for tooltips. */
  public @NonNull String summary() {
    return summary;
  }

  public @NonNull Source source() {
    return new Source(book, page);
  }

  /**
   * The effect a printed damage effect names, regardless of case, its rating and a type in brackets, e.g.
   * {@code Piercing 2} or {@code Persistent (Poison)}, also the CRB's misprint "Breaker" for Breaking (Nuka Grenade,
   * kept as printed in the catalog); empty if the books know none of that name.
   */
  public static @NonNull Optional<DamageEffect> of(@NonNull String printed) {
    String name = RATING_OR_TYPE.matcher(printed).replaceAll("").strip().toLowerCase(Locale.ROOT);
    if (name.equals("breaker")) {
      return Optional.of(BREAKING);
    }
    return Arrays.stream(values()).filter(effect -> effect.displayName.toLowerCase(Locale.ROOT).equals(name))
        .findFirst();
  }
}
