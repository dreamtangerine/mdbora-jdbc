package io.github.dreamtangerine.mdbora.jdbc;

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

/**
 * Provides version information for the Mdbora JDBC driver.
 */
final class MdboraVersion {

  static final int MAJOR_VERSION = 0;

  static final int MINOR_VERSION = 1;

  private static final String DEVELOPMENT_VERSION = "development";

  private MdboraVersion() {
  }

  /**
   * Returns the driver version declared in the JAR manifest.
   *
   * <p>
   * When Mdbora is executed directly from an IDE or from compiled classes, the package may not contain manifest information. In that case, this method returns
   * {@code development}.</p>
   *
   * @return Mdbora driver version
   */
  static String getVersion() {
    Package driverPackage = MdboraDriver.class.getPackage();
    String result = null;

    if (driverPackage != null) {
      result = driverPackage.getImplementationVersion();
    }

    if (result == null || result.isBlank()) {
      result = DEVELOPMENT_VERSION;
    }

    return result;
  }
}
