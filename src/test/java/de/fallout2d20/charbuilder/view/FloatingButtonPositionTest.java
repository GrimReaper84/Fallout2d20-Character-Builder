package de.fallout2d20.charbuilder.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FloatingButtonPositionTest {

  @Test
  void draggingUpRaisesThePosition() {
    assertEquals(0.5, FloatingButtonPosition.afterDrag(0, -100, 200), 0.001);
  }

  @Test
  void draggingDownLowersThePosition() {
    assertEquals(0.25, FloatingButtonPosition.afterDrag(0.5, 50, 200), 0.001);
  }

  @Test
  void thePositionStaysWithinTheEdge() {
    assertEquals(1, FloatingButtonPosition.afterDrag(0.5, -1000, 200), 0.001);
    assertEquals(0, FloatingButtonPosition.afterDrag(0.5, 1000, 200), 0.001);
  }

  @Test
  void withoutRoomTheButtonStays() {
    assertEquals(0.5, FloatingButtonPosition.afterDrag(0.5, -100, 0), 0.001);
  }
}
