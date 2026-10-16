package de.fallout2d20.charbuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppInfoTest {

  /** The build writes the Gradle version into a resource; a release version is numeric (MSI needs it). */
  @Test
  void versionComesFromTheBuild() {
    String version = AppInfo.version();
    assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"), version);
  }
}
