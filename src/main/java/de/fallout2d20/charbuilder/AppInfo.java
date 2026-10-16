package de.fallout2d20.charbuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jspecify.annotations.NonNull;

/** Name, version and links of the application, for the About dialog and the window title. */
public final class AppInfo {

  public static final @NonNull String NAME = "Fallout 2d20 Character Builder";
  public static final @NonNull String REPOSITORY = "https://github.com/GrimReaper84/Fallout2d20-Character-Builder";
  public static final @NonNull String LICENSE = "MIT License, Copyright (c) 2026 GrimReaper84";

  private static final @NonNull String UNKNOWN_VERSION = "dev";

  private AppInfo() {}

  /** The version the build wrote into {@code version.properties} (from {@code gradle.properties}). */
  public static @NonNull String version() {
    try (InputStream in = AppInfo.class.getResourceAsStream("version.properties")) {
      if (in == null) {
        return UNKNOWN_VERSION;
      }
      Properties properties = new Properties();
      properties.load(in);
      return properties.getProperty("version", UNKNOWN_VERSION);
    } catch (IOException e) {
      return UNKNOWN_VERSION;
    }
  }
}
