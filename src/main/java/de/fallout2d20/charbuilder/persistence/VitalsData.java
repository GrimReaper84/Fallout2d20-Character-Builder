package de.fallout2d20.charbuilder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.fallout2d20.charbuilder.model.character.apparel.HitLocation;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Save-file form of the session vitals; {@code currentHp} and {@code luckPoints} null mean "at maximum",
 * missing lists and notes mean none.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VitalsData(
    @Nullable Integer currentHp,
    int rads,
    int radiationPoints,
    @Nullable Integer luckPoints,
    @Nullable List<HitLocation> injuries,
    @Nullable String notes) {}
