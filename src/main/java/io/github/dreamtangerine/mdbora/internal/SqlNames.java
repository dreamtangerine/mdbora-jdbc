/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.internal;

/**
 * SQL identifier formatting utilities used by Mdbora internal components.
 */
public final class SqlNames {

  private SqlNames() {
  }

  /**
   * Quotes an SQL identifier and escapes embedded quote characters.
   *
   * @param value unquoted identifier
   * @return safely quoted SQL identifier
   */
  public static String identifier(String value) {
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }
}
