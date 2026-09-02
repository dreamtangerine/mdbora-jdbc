/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.codec;

import io.github.spannm.jackcess.Column;

/**
 * Maps Jackcess column types to SQL type declarations understood by the
 * internal query engine.
 */
public final class AccessTypeMapper {

  private AccessTypeMapper() {
  }

  /**
   * Creates the SQL type declaration used to expose an Access column.
   *
   * @param column source Access column
   * @return SQL type declaration for the virtual table column
   */
  public static String h2TypeDeclaration(Column column) {
    String result;

    switch (column.getType().name()) {
      case "BOOLEAN":
        result = "BOOLEAN";
        break;
      case "BYTE":
        result = "TINYINT";
        break;
      case "INT":
        result = "SMALLINT";
        break;
      case "LONG":
        result = "INTEGER";
        break;
      case "BIG_INT":
        result = "BIGINT";
        break;
      case "FLOAT":
        result = "REAL";
        break;
      case "DOUBLE":
        result = "DOUBLE PRECISION";
        break;
      case "NUMERIC":
        int precision = positive(column.getPrecision(), 38);
        int scale = Math.max(0, column.getScale());

        result = "DECIMAL(" + precision + "," + scale + ")";
        break;
      case "MONEY":
        result = "DECIMAL(19,4)";
        break;
      case "SHORT_DATE_TIME":
      case "EXT_DATE_TIME":
        result = "TIMESTAMP";
        break;
      case "BINARY":
      case "OLE":
        result = "VARBINARY";
        break;
      default:
        result = "VARCHAR";
        break;
    }

    return result;
  }

  private static int positive(int value, int fallback) {
    return value > 0 ? value : fallback;
  }
}
