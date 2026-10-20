package de.fallout2d20.charbuilder.persistence;

import java.io.Serial;

/** A character file cannot be read because its content or format is not supported. */
public class SaveFormatException extends Exception {

  @Serial
  private static final long serialVersionUID = 1L;

  public SaveFormatException(String message) {
    super(message);
  }

  public SaveFormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
