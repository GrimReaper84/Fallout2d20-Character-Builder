package de.fallout2d20.charbuilder.model.character.equipment;

import de.fallout2d20.charbuilder.catalog.apparel.ApparelData;
import de.fallout2d20.charbuilder.catalog.item.ItemData;
import de.fallout2d20.charbuilder.catalog.weapon.WeaponData;
import org.jspecify.annotations.Nullable;

/**
 * A catalog entry found for a piece of equipment pack text ({@link PackInventory}): exactly one of weapon,
 * apparel or item.
 *
 * @param weapon the weapon found
 * @param apparel the apparel found
 * @param item the item found
 * @param quantity the count the text named, 1 if none
 * @param rest the text after "with", e.g. an ammunition formula; {@code null} if none
 * @param exact whether the text names the entry as the catalog does (else the pack wording is kept as note)
 * @param builtIn whether the text calls the weapon built into the character
 */
record Match(
    @Nullable WeaponData weapon,
    @Nullable ApparelData apparel,
    @Nullable ItemData item,
    int quantity,
    @Nullable String rest,
    boolean exact,
    boolean builtIn) {}
