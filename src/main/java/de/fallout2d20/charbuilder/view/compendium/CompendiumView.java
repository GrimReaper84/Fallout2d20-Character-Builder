package de.fallout2d20.charbuilder.view.compendium;

import de.fallout2d20.charbuilder.catalog.Book;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.party.Party;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.ViewPreferences;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * The GM compendium (H2): a document tab of lists and references beside the character tabs. A top bar with the book
 * filter (Core Rulebook always on) and a Back button for followed links, below a second tab row with the sections
 * ({@link Section}). Catalog sections are built when first shown and rebuilt when the book filter changes; the
 * filter and the last section are remembered between runs.
 */
public final class CompendiumView {

  private static final @NonNull String SECTION_KEY = "compendium.section";
  private static final @NonNull String BOOK_KEY = "compendium.book.";
  private static final double PADDING = 8;

  private final @NonNull Supplier<List<PlayerCharacter>> openCharacters;
  private final @NonNull Runnable onPartyChanged;
  private final @NonNull ViewPreferences preferences = ViewPreferences.user();
  private final @NonNull RandomGenerator random = new SecureRandom();
  private final @NonNull Set<Book> books = EnumSet.of(Book.CRB);
  private final @NonNull Map<Section, Tab> tabs = new EnumMap<>(Section.class);
  private final @NonNull Map<Section, CatalogSection> catalogs = new EnumMap<>(Section.class);
  /** The listed entries per catalog section, also for link targets whose section was not shown yet. */
  private final @NonNull Map<Section, SectionContent> contents = new EnumMap<>(Section.class);
  /** Where the user was before following a link: section and selected entry. */
  private final @NonNull Deque<Location> history = new ArrayDeque<>();
  private final @NonNull Button back = new Button("Back", new FontIcon("fas-arrow-left"));
  private final @NonNull TabPane sections = new TabPane();
  private final @NonNull BorderPane node = new BorderPane();
  private @NonNull Party party;

  /**
   * @param openCharacters the characters of the open tabs, for the party's "Take values from character"
   * @param onPartyChanged after every change of the party (the caller marks it unsaved)
   */
  public CompendiumView(@NonNull Party party, @NonNull Supplier<List<PlayerCharacter>> openCharacters,
      @NonNull Runnable onPartyChanged) {
    this.party = party;
    this.openCharacters = openCharacters;
    this.onPartyChanged = onPartyChanged;
    for (Book book : Book.values()) {
      if (book == Book.CRB || preferences.getBoolean(BOOK_KEY + book.name(), true)) {
        books.add(book);
      }
    }
    sections.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    for (Section section : Section.values()) {
      Tab tab = new Tab(section.title());
      tab.setUserData(section);
      tabs.put(section, tab);
      sections.getTabs().add(tab);
    }
    sections.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> {
      Section section = selectedSection();
      fill(section);
      preferences.putString(SECTION_KEY, section.name());
    });
    back.setTooltip(new Tooltip("Back to the entry before the last followed link"));
    back.setDisable(true);
    back.setOnAction(_ -> goBack());
    node.setTop(topBar());
    node.setCenter(sections);
    Section last = Section.PARTY;
    for (Section section : Section.values()) {
      if (section.name().equals(preferences.getString(SECTION_KEY, Section.PARTY.name()))) {
        last = section;
      }
    }
    fill(Section.PARTY);
    select(last);
  }

  public @NonNull Node node() {
    return node;
  }

  /** Shows another party (New Party, Open Party) and selects the Party section. */
  public void setParty(@NonNull Party newParty) {
    party = newParty;
    tab(Section.PARTY).setContent(null);
    fill(Section.PARTY);
    select(Section.PARTY);
  }

  /** Selects the Party section. */
  public void showParty() {
    select(Section.PARTY);
  }

  private @NonNull HBox topBar() {
    HBox bar = new HBox(Layouts.GAP, new Label("Books"));
    for (Book book : Book.values()) {
      CheckBox box = new CheckBox(book.title());
      box.setSelected(books.contains(book));
      box.setDisable(book == Book.CRB);
      box.setTooltip(new Tooltip(book.summary()));
      box.setOnAction(_ -> {
        if (box.isSelected()) {
          books.add(book);
        } else {
          books.remove(book);
        }
        preferences.putBoolean(BOOK_KEY + book.name(), box.isSelected());
        rebuildCatalogs();
      });
      bar.getChildren().add(box);
    }
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    bar.getChildren().addAll(spacer, back);
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.setPadding(new Insets(PADDING));
    return bar;
  }

  /** Builds the section's content if it has none yet. */
  private void fill(@NonNull Section section) {
    Tab tab = tab(section);
    if (tab.getContent() != null) {
      return;
    }
    tab.setContent(switch (section) {
      case PARTY -> new PartySection(party, openCharacters, onPartyChanged).node();
      case ROLL_TABLES -> new RollTablesSection(books, random, this::open).node();
      default -> catalog(section).node();
    });
  }

  private @NonNull CatalogSection catalog(@NonNull Section section) {
    return catalogs.computeIfAbsent(section,
        _ -> new CatalogSection(content(section), this::isListed, this::open));
  }

  private @NonNull SectionContent content(@NonNull Section section) {
    return contents.computeIfAbsent(section, _ -> CompendiumContent.of(section, books));
  }

  /** The book filter changed: catalog sections and roll tables are built again when shown; links back are dropped. */
  private void rebuildCatalogs() {
    catalogs.clear();
    contents.clear();
    history.clear();
    back.setDisable(true);
    tabs.forEach((section, tab) -> {
      if (section != Section.PARTY) {
        tab.setContent(null);
      }
    });
    fill(selectedSection());
  }

  /** Whether a link target is listed with the current books. */
  private boolean isListed(@NonNull Reference reference) {
    return content(reference.section()).entry(reference.id()).isPresent();
  }

  /** Follows a link: remembers where the user was, then shows the target. */
  private void open(@NonNull Reference reference) {
    Section current = selectedSection();
    history.push(new Location(current, current.isCatalog() ? catalog(current).selectedId().orElse(null) : null));
    back.setDisable(false);
    show(reference.section(), reference.id());
  }

  private void goBack() {
    Location location = history.poll();
    back.setDisable(history.isEmpty());
    if (location != null) {
      show(location.section(), location.id());
    }
  }

  private void show(@NonNull Section section, @Nullable String id) {
    select(section);
    if (id != null && section.isCatalog()) {
      catalog(section).select(id);
    }
  }

  private void select(@NonNull Section section) {
    sections.getSelectionModel().select(tab(section));
  }

  /** The tab of the section; every section has one. */
  private @NonNull Tab tab(@NonNull Section section) {
    return Objects.requireNonNull(tabs.get(section));
  }

  private @NonNull Section selectedSection() {
    Tab selected = sections.getSelectionModel().getSelectedItem();
    return selected != null && selected.getUserData() instanceof Section section ? section : Section.PARTY;
  }
}
