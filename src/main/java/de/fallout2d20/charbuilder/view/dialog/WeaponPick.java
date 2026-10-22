package de.fallout2d20.charbuilder.view.dialog;

import de.fallout2d20.charbuilder.model.character.weapon.CarriedWeapon;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A weapon picked in the {@link AddWeaponDialog}, with the rounds of ammunition to add along with it.
 *
 * @param weapon the weapon, with its quantity
 * @param ammunitionId the ammunition type of the rounds; {@code null} for none
 * @param rounds rounds to add to the character's ammunition; 0 for none
 */
public record WeaponPick(@NonNull CarriedWeapon weapon, @Nullable String ammunitionId, int rounds) {}
