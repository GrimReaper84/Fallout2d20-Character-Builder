package de.fallout2d20.charbuilder.catalog.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.catalog.Source;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A book or magazine publication (CRB pp. 172–180): reading one grants a temporary perk, either the
 * publication's own or that of the issue found (rolled on its d20 table). All weigh {@code <1}, cost 100
 * and have rarity 3 (CRB p. 172).
 *
 * @param id stable catalog id, referenced by save files
 * @param name name as printed
 * @param sources book and page where the publication starts
 * @param summary its temporary perk in short (own words)
 * @param issues the issue table; empty for a publication with one perk
 */
public record MagazineData(
    @JsonProperty(required = true) @NonNull String id,
    @JsonProperty(required = true) @NonNull String name,
    @JsonProperty(required = true) @NonNull List<Source> sources,
    @JsonProperty(required = true) @NonNull String summary,
    @NonNull List<MagazineIssue> issues) {

  /** How books and magazines are labelled next to the item categories. */
  public static final @NonNull String CATEGORY = "Book/Magazine";
  /** CRB p. 172: weight of every book and magazine. */
  public static final @NonNull String WEIGHT = "<1";
  /** CRB p. 172: cost of every book and magazine. */
  public static final int COST = 100;
  /** CRB p. 172: rarity of every book and magazine. */
  public static final int RARITY = 3;

  public MagazineData {
    if (id == null || id.isBlank() || name == null || name.isBlank() || sources == null || sources.isEmpty()
        || summary == null || summary.isBlank()) {
      throw new IllegalArgumentException("Magazine needs id, name, source and summary: " + id);
    }
    sources = List.copyOf(sources);
    issues = issues == null ? List.of() : List.copyOf(issues);
  }

  /** Whether one of the books describing the publication is enabled. */
  public boolean isAvailableWith(@NonNull Set<Book> books) {
    return sources.stream().anyMatch(source -> books.contains(source.book()));
  }

  /** The issue with this title; empty for {@code null} or a title not in the table. */
  public @NonNull Optional<MagazineIssue> issue(@Nullable String title) {
    return issues.stream().filter(issue -> issue.title().equals(title)).findFirst();
  }
}
