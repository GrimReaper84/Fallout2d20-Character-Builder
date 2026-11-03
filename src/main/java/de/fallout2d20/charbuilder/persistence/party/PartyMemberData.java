package de.fallout2d20.charbuilder.persistence.party;

import org.jspecify.annotations.Nullable;

/**
 * A party member in the party file (H2). Missing values read as empty text or 0.
 *
 * @param name the character's name
 * @param player the player's name
 * @param level the character's level
 * @param currentHp current HP
 * @param maxHp maximum HP before rads
 * @param rads radiation damage
 * @param initiative Initiative
 * @param injuries injured hit locations as text
 */
record PartyMemberData(@Nullable String name, @Nullable String player, int level, int currentHp, int maxHp, int rads,
    int initiative, @Nullable String injuries) {}
