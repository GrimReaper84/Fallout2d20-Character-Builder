package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.QualitySelection;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponQuality;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jspecify.annotations.NonNull;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * The qualities of a custom weapon as check boxes (CRB p. 90), each with its short description as tooltip:
 * Thrown with a Close/Medium choice, Accurate/Inaccurate and Reliable/Unreliable untick each other (a weapon
 * can not have both), and a text field keeps qualities outside the book (e.g. from older files).
 */
final class QualityCheckBoxes {

  private static final int COLUMNS = 4;

  private final @NonNull Map<WeaponQuality, CheckBox> boxes = new EnumMap<>(WeaponQuality.class);
  private final @NonNull ComboBox<WeaponRange> thrownRange =
      new ComboBox<>(FXCollections.observableArrayList(WeaponRange.CLOSE, WeaponRange.MEDIUM));
  private final @NonNull TextField other = new TextField();
  private final @NonNull MigPane pane = Layouts.wideTable(COLUMNS);

  /** @param qualities the weapon's qualities as stored, e.g. {@code Accurate, Thrown (M)} */
  QualityCheckBoxes(@NonNull String qualities) {
    QualitySelection current = WeaponQuality.parse(qualities);
    thrownRange.setConverter(new DisplayConverter<>(WeaponRange::displayName));
    WeaponRange range = current.thrownRange();
    thrownRange.setValue(range == null ? WeaponRange.CLOSE : range);
    for (WeaponQuality quality : WeaponQuality.values()) {
      CheckBox box = new CheckBox(quality.displayName());
      box.setSelected(current.qualities().contains(quality));
      box.setTooltip(new Tooltip(quality.summary() + " (" + WeaponQuality.SOURCE + ")"));
      boxes.put(quality, box);
    }
    boxes.forEach((quality, box) -> quality.opposite().ifPresent(opposite -> box.selectedProperty()
        .addListener((_, _, selected) -> {
          if (selected) {
            box(opposite).setSelected(false);
          }
        })));
    thrownRange.disableProperty().bind(box(WeaponQuality.THROWN).selectedProperty().not());
    for (WeaponQuality quality : WeaponQuality.values()) {
      CheckBox box = box(quality);
      pane.add(quality == WeaponQuality.THROWN ? new HBox(Layouts.GAP, box, thrownRange) : box);
    }
    other.setText(current.other());
    other.setPromptText("Qualities not in the book, comma-separated");
    pane.add(new Label("Other"), Layouts.sectionStart());
    pane.add(other, Layouts.sectionStart(Layouts.span(COLUMNS - 1).growX()));
  }

  private @NonNull CheckBox box(@NonNull WeaponQuality quality) {
    return Objects.requireNonNull(boxes.get(quality), "every quality has a check box");
  }

  @NonNull MigPane node() {
    return pane;
  }

  /** The ticked qualities as stored in the weapon. */
  @NonNull String text() {
    Set<WeaponQuality> ticked = EnumSet.noneOf(WeaponQuality.class);
    boxes.forEach((quality, box) -> {
      if (box.isSelected()) {
        ticked.add(quality);
      }
    });
    WeaponRange range = null;
    if (ticked.contains(WeaponQuality.THROWN)) {
      range = Objects.requireNonNullElse(thrownRange.getValue(), WeaponRange.CLOSE);
    }
    return new QualitySelection(ticked, range, other.getText()).text();
  }
}
