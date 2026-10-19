package de.fallout2d20.charbuilder.icons;

import de.fallout2d20.charbuilder.App;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Draws the application icon (user decision 2026-10-19, plan {@code docs/plans/app_icon_plan.md}): the Font
 * Awesome Free "radiation" glyph (CC BY 4.0, recolored) in Pip-Boy green on a dark rounded square. Writes the
 * window icons into the main resources and the installer icons (PNG, ICO, ICNS) into the packaging folder; started
 * by the Gradle task {@code generateIcons}, never by the build. The generated files are committed.
 */
public final class GenerateIcons {

  private static final String FONT = "/META-INF/resources/fontawesome6/6.5.2/fonts/fa-solid-900.ttf";
  /** Font Awesome "radiation" (fa-radiation). */
  private static final String RADIATION = "\uf7b9";
  private static final Color GREEN = new Color(0x14FF14);
  private static final Color BACKGROUND = new Color(0x0A140A);
  /** Corner radius of the rounded square, relative to its size. */
  private static final double CORNER = 0.22;
  /** Larger side of the glyph, relative to the tile. */
  private static final double GLYPH = 0.64;
  /** ICNS entry types holding a PNG of the given size (512 at 2x is the 1024 entry). */
  private static final Map<String, Integer> ICNS_TYPES = icnsTypes();

  private GenerateIcons() {}

  /** @param args the main resource folder of the {@code App} class and the packaging folder */
  public static void main(String[] args) throws IOException, FontFormatException {
    if (args.length != 2) {
      throw new IllegalArgumentException("Usage: GenerateIcons <resource folder> <packaging folder>");
    }
    Path resources = Files.createDirectories(Path.of(args[0]));
    Path packaging = Files.createDirectories(Path.of(args[1]));
    Font font = loadFont();
    for (int size : App.ICON_SIZES) {
      write(resources.resolve("icon-" + size + ".png"), png(draw(font, size)));
    }
    write(packaging.resolve("icon.png"), png(draw(font, 512)));
    write(packaging.resolve("icon.ico"), ico(font));
    write(packaging.resolve("icon.icns"), icns(font));
  }

  private static Font loadFont() throws IOException, FontFormatException {
    try (InputStream in = GenerateIcons.class.getResourceAsStream(FONT)) {
      if (in == null) {
        throw new IOException("Font Awesome font not on the classpath: " + FONT);
      }
      return Font.createFont(Font.TRUETYPE_FONT, in);
    }
  }

  /** The icon at {@code size} pixels, drawn directly at that size (sharper than scaling a large one down). */
  static BufferedImage draw(Font font, int size) {
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      double arc = 2 * CORNER * size;
      g.setColor(BACKGROUND);
      g.fill(new RoundRectangle2D.Double(0, 0, size, size, arc, arc));
      g.setColor(GREEN);
      g.fill(glyph(font, size));
    } finally {
      g.dispose();
    }
    return image;
  }

  /** The glyph outline scaled so its larger side is {@link #GLYPH} of the tile, centered on its own bounds. */
  private static Shape glyph(Font font, int size) {
    FontRenderContext context = new FontRenderContext(null, true, true);
    Shape outline = font.deriveFont(100f).createGlyphVector(context, RADIATION).getOutline();
    Rectangle2D bounds = outline.getBounds2D();
    double scale = GLYPH * size / Math.max(bounds.getWidth(), bounds.getHeight());
    AffineTransform transform = new AffineTransform();
    transform.translate(size / 2.0, size / 2.0);
    transform.scale(scale, scale);
    transform.translate(-bounds.getCenterX(), -bounds.getCenterY());
    return transform.createTransformedShape(outline);
  }

  private static byte[] png(BufferedImage image) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "png", out);
    return out.toByteArray();
  }

  /** Windows icon: header, one directory entry per size, then the PNG images (supported since Windows Vista). */
  private static byte[] ico(Font font) throws IOException {
    List<byte[]> images = new ArrayList<>();
    for (int size : App.ICON_SIZES) {
      images.add(png(draw(font, size)));
    }
    int headerSize = 6 + 16 * images.size();
    ByteBuffer header = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN);
    header.putShort((short) 0).putShort((short) 1).putShort((short) images.size());
    int offset = headerSize;
    for (int i = 0; i < images.size(); i++) {
      int size = App.ICON_SIZES.get(i);
      header.put((byte) (size >= 256 ? 0 : size)).put((byte) (size >= 256 ? 0 : size)).put((byte) 0).put((byte) 0);
      header.putShort((short) 1).putShort((short) 32).putInt(images.get(i).length).putInt(offset);
      offset += images.get(i).length;
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(header.array());
    for (byte[] image : images) {
      out.write(image);
    }
    return out.toByteArray();
  }

  /** macOS icon: "icns" with the total length, then typed entries holding PNG images (big-endian lengths). */
  private static byte[] icns(Font font) throws IOException {
    ByteArrayOutputStream entries = new ByteArrayOutputStream();
    DataOutputStream data = new DataOutputStream(entries);
    for (Map.Entry<String, Integer> type : ICNS_TYPES.entrySet()) {
      byte[] image = png(draw(font, type.getValue()));
      data.writeBytes(type.getKey());
      data.writeInt(8 + image.length);
      data.write(image);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataOutputStream file = new DataOutputStream(out);
    file.writeBytes("icns");
    file.writeInt(8 + entries.size());
    entries.writeTo(file);
    return out.toByteArray();
  }

  private static Map<String, Integer> icnsTypes() {
    Map<String, Integer> types = new LinkedHashMap<>();
    types.put("icp4", 16);
    types.put("icp5", 32);
    types.put("icp6", 64);
    types.put("ic07", 128);
    types.put("ic08", 256);
    types.put("ic09", 512);
    types.put("ic10", 1024);
    return types;
  }

  private static void write(Path file, byte[] bytes) throws IOException {
    try (OutputStream out = Files.newOutputStream(file)) {
      out.write(bytes);
    }
    System.out.println("Wrote " + file);
  }
}
