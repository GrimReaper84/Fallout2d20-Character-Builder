package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of the starting equipment selection: the pack reference, the picked option per pack
 * choice ({@code null} = open), Tag skill item picks keyed by skill id, the trinket, the free-arms switch
 * and the Mister Handy's arms.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StartingEquipmentData(
    @Nullable CatalogRefData pack,
    @Nullable List<@Nullable String> choices,
    @Nullable Map<String, @Nullable String> tagSkillItems,
    @Nullable String trinket,
    boolean freeArms,
    @Nullable List<@Nullable ArmData> arms) {}
