package de.fallout2d20.charbuilder.icons;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.fallout2d20.charbuilder.App;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** The committed icons written by {@link GenerateIcons} are there and have the expected sizes and formats. */
class AppIconTest {

  @Test
  void windowIconsInEverySize() throws IOException {
    for (int size : App.ICON_SIZES) {
      try (InputStream in = App.class.getResourceAsStream("icon-" + size + ".png")) {
        assertNotNull(in, "icon-" + size + ".png");
        BufferedImage image = ImageIO.read(in);
        assertEquals(size, image.getWidth());
        assertEquals(size, image.getHeight());
      }
    }
  }

  @Test
  void installerIcons() throws IOException {
    BufferedImage png = ImageIO.read(Path.of("packaging/icon.png").toFile());
    assertEquals(512, png.getWidth());
    byte[] ico = Files.readAllBytes(Path.of("packaging/icon.ico"));
    assertArrayEquals(new byte[] {0, 0, 1, 0}, Arrays.copyOf(ico, 4), "ICO header");
    byte[] icns = Files.readAllBytes(Path.of("packaging/icon.icns"));
    assertEquals("icns", new String(icns, 0, 4, StandardCharsets.US_ASCII));
    assertEquals(icns.length, ((icns[4] & 0xff) << 24) | ((icns[5] & 0xff) << 16) | ((icns[6] & 0xff) << 8)
        | (icns[7] & 0xff), "ICNS length");
  }
}
