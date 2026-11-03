package de.fallout2d20.charbuilder.view;

import de.fallout2d20.charbuilder.AppInfo;
import de.fallout2d20.charbuilder.model.character.PlayerCharacter;
import de.fallout2d20.charbuilder.model.party.Party;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import de.fallout2d20.charbuilder.persistence.party.PartyStore;
import de.fallout2d20.charbuilder.view.compendium.CompendiumView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The GM compendium's document tab (H2) and its party file: opens or selects the tab, New/Open/Save Party, and asks
 * before unsaved party changes are lost. At most one compendium tab is open, holding one party; closing the tab
 * drops the party.
 */
final class CompendiumTab {

  private final @NonNull PartyStore store = new PartyStore();
  private final @NonNull TabPane documents;
  private final @NonNull Supplier<@Nullable Window> owner;
  private final @NonNull Supplier<List<PlayerCharacter>> openCharacters;
  private final @NonNull Runnable onTitleChanged;
  private final @NonNull BiConsumer<String, @Nullable String> showError;
  private @Nullable Tab tab;
  private @Nullable CompendiumView view;
  private @NonNull PartyDocument party = PartyDocument.created(Party.createNew(AppInfo.version()));

  /**
   * @param documents the main window's document tabs
   * @param owner the main window, for dialogs
   * @param openCharacters the characters of the open tabs
   * @param onTitleChanged after the party's title (name, unsaved marker) changed
   * @param showError shows an error with header and message
   */
  CompendiumTab(@NonNull TabPane documents, @NonNull Supplier<@Nullable Window> owner,
      @NonNull Supplier<List<PlayerCharacter>> openCharacters, @NonNull Runnable onTitleChanged,
      @NonNull BiConsumer<String, @Nullable String> showError) {
    this.documents = documents;
    this.owner = owner;
    this.openCharacters = openCharacters;
    this.onTitleChanged = onTitleChanged;
    this.showError = showError;
  }

  /** Opens the compendium tab, or selects it if it is open. */
  void show() {
    if (tab == null) {
      CompendiumView newView = new CompendiumView(party.party(), openCharacters, this::onPartyChanged);
      Tab newTab = new Tab(party.title(), newView.node());
      newTab.setOnCloseRequest(event -> {
        if (!confirmDiscard()) {
          event.consume();
        }
      });
      view = newView;
      tab = newTab;
      documents.getTabs().add(newTab);
    }
    documents.getSelectionModel().select(tab);
  }

  /** Opens or selects the compendium tab on its Party section. */
  void showParty() {
    show();
    if (view != null) {
      view.showParty();
    }
  }

  /** Closes the compendium tab after asking about unsaved party changes. */
  void close() {
    if (tab != null && confirmDiscard()) {
      documents.getTabs().remove(tab);
    }
  }

  /** Whether the tab is the compendium tab. */
  boolean isTab(@Nullable Tab candidate) {
    return candidate != null && candidate.equals(tab);
  }

  /** The compendium tab was removed from the window: the party is dropped. */
  void closed() {
    tab = null;
    view = null;
    party = PartyDocument.created(Party.createNew(AppInfo.version()));
  }

  /** The window title for the compendium tab. */
  @NonNull String title() {
    return party.title();
  }

  /** Starts an empty party after asking about unsaved changes of the current one. */
  void newParty() {
    if (confirmDiscard()) {
      replace(PartyDocument.created(Party.createNew(AppInfo.version())));
    }
  }

  /** Opens a party file after asking about unsaved changes of the current party. */
  void openParty() {
    if (!confirmDiscard()) {
      return;
    }
    File chosen = chooser("Open party").showOpenDialog(owner.get());
    if (chosen == null) {
      return;
    }
    try {
      replace(PartyDocument.loaded(store.load(chosen.toPath()), chosen.toPath()));
    } catch (IOException | SaveFormatException e) {
      showError.accept("The party could not be opened.", e.getMessage());
    }
  }

  /** Saves the party, asking for a file if it has none or {@code askForFile}; returns whether it was written. */
  boolean save(boolean askForFile) {
    Path target = party.file();
    if (askForFile || target == null) {
      FileChooser chooser = chooser("Save party");
      chooser.setInitialFileName(party.displayName().replaceAll("[\\\\/:*?\"<>|]", "_") + ".json");
      File chosen = chooser.showSaveDialog(owner.get());
      if (chosen == null) {
        return false;
      }
      target = chosen.toPath();
    }
    try {
      store.save(party.party(), target);
    } catch (IOException e) {
      showError.accept("The party could not be saved.", e.getMessage());
      return false;
    }
    party.markSaved(target);
    updateTitle();
    return true;
  }

  /** Returns true if the party has no unsaved changes or the user saved or discarded them. */
  boolean confirmDiscard() {
    if (!party.isDirty()) {
      return true;
    }
    if (tab != null) {
      documents.getSelectionModel().select(tab);
    }
    return switch (SaveQuestion.ask(owner.get(), party.displayName())) {
      case SAVE -> save(false);
      case DISCARD -> true;
      case CANCEL -> false;
    };
  }

  private void replace(@NonNull PartyDocument newParty) {
    party = newParty;
    if (view != null) {
      view.setParty(newParty.party());
    }
    showParty();
    updateTitle();
  }

  private void onPartyChanged() {
    party.markDirty();
    updateTitle();
  }

  private void updateTitle() {
    if (tab != null) {
      tab.setText(party.title());
    }
    onTitleChanged.run();
  }

  private @NonNull FileChooser chooser(@NonNull String title) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(title);
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Party files (*.json)", "*.json"));
    Path file = party.file();
    Path directory = file == null ? null : file.getParent();
    if (directory != null && directory.toFile().isDirectory()) {
      chooser.setInitialDirectory(directory.toFile());
    }
    return chooser;
  }
}
