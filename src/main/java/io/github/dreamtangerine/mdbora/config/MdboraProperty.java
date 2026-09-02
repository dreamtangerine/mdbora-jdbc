/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.config;

/**
 * Names of the public Mdbora JDBC connection properties.
 */
public final class MdboraProperty {

  /** Connection read-only mode property. */
  public static final String READ_ONLY = "readOnly";

  /** Internal cache memory budget property, expressed in KiB. */
  public static final String CACHE_SIZE = "cacheSize";

  /** Maximum number of rows retained in memory by supported operations. */
  public static final String MAX_IN_MEMORY_ROWS = "maxInMemoryRows";

  private MdboraProperty() {
  }
}
