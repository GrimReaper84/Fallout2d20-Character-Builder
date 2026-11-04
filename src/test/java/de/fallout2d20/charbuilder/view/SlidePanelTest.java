package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SlidePanelTest {

  private static final double EXTRA = 40;

  @Test
  void coversTheRightThirdOfAWideHost() {
    assertEquals(600, SlidePanel.width(1800, 300), 0.001);
  }

  @Test
  void isAtLeastAsWideAsItsContent() {
    assertEquals(500 + EXTRA, SlidePanel.width(900, 500), 0.001);
  }

  @Test
  void neverExceedsTheHost() {
    assertEquals(400, SlidePanel.width(400, 500), 0.001);
  }
}
