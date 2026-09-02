/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.config;

import java.sql.SQLException;
import java.util.Properties;

/**
 * Immutable configuration for a Mdbora connection.
 */
public final class MdboraConfiguration {

  /**
   * Mdbora currently operates exclusively in read-only mode.
   */
  public static final boolean DEFAULT_READ_ONLY = true;

  /**
   * Default internal cache memory budget, expressed in KiB.
   */
  public static final int DEFAULT_CACHE_SIZE = 2_048;

  /**
   * Default maximum number of rows that supported operations may retain in memory before using temporary storage.
   */
  public static final int DEFAULT_MAX_IN_MEMORY_ROWS = 1_000;

  private final boolean readOnly;
  private final int cacheSize;
  private final int maxInMemoryRows;

  private MdboraConfiguration(boolean readOnly, int cacheSize, int maxInMemoryRows) {
    this.readOnly = readOnly;
    this.cacheSize = cacheSize;
    this.maxInMemoryRows = maxInMemoryRows;
  }

  /**
   * Creates a configuration from the connection properties supplied to {@code DriverManager}.
   *
   * @param properties connection properties, or {@code null}
   *
   * @return validated immutable configuration
   *
   * @throws SQLException if a property contains an invalid value
   */
  public static MdboraConfiguration from(Properties properties) throws SQLException {
    if (properties == null) {
      properties = new Properties();
    }

    boolean readOnly = readBoolean(properties, MdboraProperty.READ_ONLY, DEFAULT_READ_ONLY);
    int cacheSize = readPositiveInteger(properties, MdboraProperty.CACHE_SIZE, DEFAULT_CACHE_SIZE);
    int maxInMemoryRows = readPositiveInteger(properties, MdboraProperty.MAX_IN_MEMORY_ROWS, DEFAULT_MAX_IN_MEMORY_ROWS);

    validateReadOnly(readOnly);

    return new MdboraConfiguration(readOnly, cacheSize, maxInMemoryRows);
  }

  private static void validateReadOnly(boolean readOnly) throws SQLException {
    if (!readOnly) {
      throw new SQLException("La versión actual de Mdbora solo admite conexiones de lectura");
    }
  }

  private static int readPositiveInteger(Properties properties, String propertyName, int defaultValue) throws SQLException {
    String text = properties.getProperty(propertyName);
    int value = defaultValue;

    if (text != null && !text.isBlank()) {
      value = parseInteger(propertyName, text);

      if (value <= 0) {
        throw new SQLException("La propiedad " + propertyName + " debe ser mayor que cero: " + value);
      }
    }

    return value;
  }

  private static int parseInteger(String propertyName, String text) throws SQLException {
    try {
      return Integer.parseInt(text.trim());
    } catch (NumberFormatException exception) {
      throw new SQLException("La propiedad " + propertyName + " debe contener un número entero: " + text, exception);
    }
  }

  private static boolean readBoolean(Properties properties, String propertyName, boolean defaultValue) throws SQLException {
    String text = properties.getProperty(propertyName);
    boolean value = defaultValue;

    if (text != null && !text.isBlank()) {
      String normalized = text.trim();

      if ("true".equalsIgnoreCase(normalized)) {
        value = true;
      } else if ("false".equalsIgnoreCase(normalized)) {
        value = false;
      } else {
        throw new SQLException("La propiedad " + propertyName + " debe ser true o false: " + text);
      }
    }

    return value;
  }

  /**
   * Indicates whether the connection is configured as read-only.
   *
   * @return {@code true}; current Mdbora versions only support read-only mode
   */
  public boolean isReadOnly() {
    return readOnly;
  }

  /**
   * Returns the internal cache memory budget.
   *
   * @return cache size in KiB
   */
  public int getCacheSize() {
    return cacheSize;
  }

  /**
   * Returns the maximum number of rows that supported operations may retain in memory before using temporary storage.
   *
   * @return maximum number of in-memory rows
   */
  public int getMaxInMemoryRows() {
    return maxInMemoryRows;
  }
}
