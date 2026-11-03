package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.view.catalog.CatalogPage;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import org.jspecify.annotations.NonNull;

/**
 * A catalog section of the GM compendium (H2): the list with category filter, search and sortable columns on the
 * left, the selected entry's details with links on the right.
 */
final class CatalogSection {

  private static final double LIST_SHARE = 0.55;
  private static final double PADDING = 8;

  private final @NonNull SectionContent content;
  private final @NonNull CatalogPage<CompendiumEntry> page;
  private final @NonNull SplitPane node;

  /**
   * @param available whether a link target is listed with the current book filter
   * @param open opens a link target
   */
  CatalogSection(@NonNull SectionContent content, @NonNull Predicate<Reference> available,
      @NonNull Consumer<Reference> open) {
    this.content = content;
    page = new CatalogPage<>(content.entries(), CompendiumEntry::name, CompendiumEntry::category,
        content.categories(), _ -> "");
    page.fillAvailableSpace();
    List<Column> columns = content.columns();
    for (int i = 0; i < columns.size(); i++) {
      int index = i;
      Column column = columns.get(i);
      if (column.numeric()) {
        page.addNumberColumn(column.header(), entry -> entry.cells().get(index).sortKey(),
            entry -> entry.cells().get(index).text());
      } else {
        page.addColumn(column.header(), entry -> entry.cells().get(index).text());
      }
    }
    ScrollPane details = new ScrollPane(DetailPanel.placeholder());
    details.setFitToWidth(true);
    details.setPadding(new Insets(PADDING));
    page.selectedItemProperty().addListener((_, _, entry) -> details.setContent(
        entry == null ? DetailPanel.placeholder() : DetailPanel.build(entry, available, open)));
    page.node().setPadding(new Insets(PADDING));
    node = new SplitPane(page.node(), details);
    node.setDividerPositions(LIST_SHARE);
  }

  @NonNull Node node() {
    return node;
  }

  /** Selects the entry with this id, if listed. */
  void select(@NonNull String id) {
    content.entry(id).ifPresent(page::select);
  }

  /** The id of the selected entry. */
  @NonNull Optional<String> selectedId() {
    return Optional.ofNullable(page.selected()).map(CompendiumEntry::id);
  }
}
