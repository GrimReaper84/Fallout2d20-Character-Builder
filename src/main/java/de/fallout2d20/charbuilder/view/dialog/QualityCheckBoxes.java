package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.catalog.weapon.QualitySelection;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponQuality;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponRange;
import de.fallout2d20.charbuilder.view.DisplayConverter;
import de.fallout2d20.charbuilder.view.Layouts;
import de.fallout2d20.charbuilder.view.WholeNumber;
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
 * The qualities of a custom weapon as check boxes (CRB p. 90, WG pp. 56–57), each with its short description and
 * source as tooltip: Thrown with a Close/Medium choice, the rated qualities (Ammo-Hungry, Delay, Recoil) with a
 * number field (empty: no number, Delay then reads "(X)"), Accurate/Inaccurate and Reliable/Unreliable untick each
 * other (a weapon can not have both), and a text field keeps qualities outside the books (e.g. from older files).
 */
final class QualityCheckBoxes {

  private static final int COLUMNS = 4;

  private final @NonNull Map<WeaponQuality, CheckBox> boxes = new EnumMap<>(WeaponQuality.class);
  private final @NonNull ComboBox<WeaponRange> thrownRange =
      new ComboBox<>(FXCollections.observableArrayList(WeaponRange.CLOSE, WeaponRange.MEDIUM));
  private final @NonNull Map<WeaponQuality, TextField> ratings = new EnumMap<>(WeaponQuality.class);
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
      box.setTooltip(new Tooltip(quality.summary() + " (" + quality.source() + ")"));
      boxes.put(quality, box);
      if (quality.isRated()) {
        Integer rating = current.ratings().get(quality);
        TextField number = new TextField(rating == null ? "" : rating.toString());
        number.setPrefColumnCount(2);
        number.setPromptText("X");
        number.disableProperty().bind(box.selectedProperty().not());
        ratings.put(quality, number);
      }
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
      TextField number = ratings.get(quality);
      if (quality == WeaponQuality.THROWN) {
        pane.add(new HBox(Layouts.GAP, box, thrownRange));
      } else if (number != null) {
        pane.add(new HBox(Layouts.GAP, box, number));
      } else {
        pane.add(box);
      }
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
    Map<WeaponQuality, Integer> numbers = new EnumMap<>(WeaponQuality.class);
    ratings.forEach((quality, field) -> {
      Integer number = WholeNumber.atLeast(field.getText(), 1);
      if (ticked.contains(quality) && number != null) {
        numbers.put(quality, number);
      }
    });
    return new QualitySelection(ticked, range, numbers, other.getText()).text();
  }
}
