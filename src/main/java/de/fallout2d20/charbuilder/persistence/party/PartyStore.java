package de.fallout2d20.charbuilder.persistence.party;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.fallout2d20.charbuilder.AppInfo;
import de.fallout2d20.charbuilder.model.party.Party;
import de.fallout2d20.charbuilder.model.party.PartyMember;
import de.fallout2d20.charbuilder.persistence.SaveFormatException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Reads and writes party files of the GM compendium (H2). A file needs the party {@code format} marker and a known
 * {@code formatVersion}; a newer version asks for an app update. Unknown fields are ignored, missing texts read as
 * empty and missing numbers as 0.
 */
public final class PartyStore {

  private final @NonNull ObjectMapper mapper = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private final @NonNull String appVersion;

  public PartyStore() {
    this(AppInfo.version());
  }

  /** @param appVersion the version written as {@code savedWith} */
  public PartyStore(@NonNull String appVersion) {
    this.appVersion = appVersion;
  }

  public @NonNull Party load(@NonNull Path file) throws IOException, SaveFormatException {
    PartyData data;
    try {
      data = mapper.readValue(Files.readString(file, StandardCharsets.UTF_8), PartyData.class);
    } catch (JsonProcessingException e) {
      throw new SaveFormatException("Not a party file: " + e.getOriginalMessage(), e);
    }
    if (data == null || !PartyData.FORMAT.equals(data.format())) {
      throw new SaveFormatException("Not a party file of " + AppInfo.NAME + ".");
    }
    if (data.formatVersion() < 1) {
      throw new SaveFormatException("The party file has no valid format version.");
    }
    if (data.formatVersion() > PartyData.FORMAT_VERSION) {
      String by = data.savedWith() == null ? "a newer version" : "version " + data.savedWith();
      throw new SaveFormatException("This party was saved with " + by + " of " + AppInfo.NAME + " (file format "
          + data.formatVersion() + "). Please update the app to open it.");
    }
    List<PartyMemberData> members = data.members() == null ? List.of() : data.members();
    return new Party(text(data.name()), members.stream().filter(Objects::nonNull).map(PartyStore::toModel).toList(),
        data.createdWith());
  }

  /** Writes the party in the current format, replacing the target file only after success. */
  public void save(@NonNull Party party, @NonNull Path file) throws IOException {
    Path absolute = file.toAbsolutePath();
    Path temp = Files.createTempFile(absolute.getParent(), ".party", ".tmp");
    try {
      PartyData data = new PartyData(PartyData.FORMAT, PartyData.FORMAT_VERSION, party.getCreatedWith(), appVersion,
          party.getName(), party.members().stream().map(PartyStore::toData).toList());
      Files.writeString(temp, mapper.writeValueAsString(data), StandardCharsets.UTF_8);
      Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private static @NonNull PartyMember toModel(@NonNull PartyMemberData data) {
    return new PartyMember(text(data.name()), text(data.player()), data.level(), data.currentHp(), data.maxHp(),
        data.rads(), data.initiative(), text(data.injuries()));
  }

  private static @NonNull PartyMemberData toData(@NonNull PartyMember member) {
    return new PartyMemberData(member.name(), member.player(), member.level(), member.currentHp(), member.maxHp(),
        member.rads(), member.initiative(), member.injuries());
  }

  private static @NonNull String text(@Nullable String value) {
    return value == null ? "" : value;
  }
}
