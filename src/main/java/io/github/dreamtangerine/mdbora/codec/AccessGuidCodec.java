/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.codec;

import java.util.Locale;
import java.util.UUID;

/**
 * Converts GUID values between the canonical JDBC representation and the
 * braced representation used by Jackcess for Access index keys.
 */
public final class AccessGuidCodec {

  private AccessGuidCodec() {
  }

  /**
   * Converts an Access GUID into a canonical, uppercase JDBC string without
   * surrounding braces or quotes.
   *
   * @param value GUID text in Access or JDBC notation
   * @return canonical uppercase GUID text
   * @throws IllegalArgumentException if the value is not a valid UUID
   */
  public static String toJdbc(String value) {
    String guid = strip(value);

    // Para validar
    UUID.fromString(guid);

    return guid.toUpperCase(Locale.ROOT);
  }

  /**
   * Converts a JDBC GUID into the uppercase, braced text expected by
   * Jackcess when creating an Access index key.
   *
   * @param value GUID text in Access or JDBC notation
   * @return uppercase GUID text surrounded by braces
   * @throws IllegalArgumentException if the value is not a valid UUID
   */
  public static String toJackcess(String value) {
    String guid = strip(value);

    // Para validar
    UUID.fromString(guid);

    return "{" + guid.toUpperCase(Locale.ROOT) + "}";
  }

  private static String strip(String value) {
    String result = null;

    if (null != value) {
      result = value.trim();

      result = stripPair(result, '\'', '\'');
      result = stripPair(result, '"', '"');
      result = stripPair(result, '{', '}');
    }

    return result;
  }

  private static String stripPair(String value, char open, char close) {
    String result = value;
    
    if (value.length() >= 2 && value.charAt(0) == open && value.charAt(value.length() - 1) == close) {
      result = value.substring(1, value.length() - 1).trim();
    }
    
    return result;
  }
}
