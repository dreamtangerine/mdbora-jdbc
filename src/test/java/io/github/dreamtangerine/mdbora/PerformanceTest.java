package io.github.dreamtangerine.mdbora;

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

import io.github.dreamtangerine.mdbora.config.MdboraProperty;
import io.github.dreamtangerine.mdbora.jdbc.MdboraDriver;
import io.github.dreamtangerine.mdbora.testsupport.LargeAccessFixtureGenerator;
import io.github.dreamtangerine.mdbora.tools.MdboraDatabaseScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generates a synthetic Access database and scans all its tables through the Mdbora JDBC driver.
 *
 * <p>
 * The generated database is stored in a temporary directory managed by JUnit. The directory and all its contents are deleted automatically when the test finishes.
 * </p>
 */
class PerformanceTest {

  private static final int CUSTOMER_COUNT = 10_000;

  private static final int ORDER_COUNT = 50_000;

  private static final int ITEM_COUNT = 100_000;

  private static final int CACHE_SIZE_KIB = 2_048;

  private static final int MAX_IN_MEMORY_ROWS = 1_000;

  /**
   * Temporary directory created and removed automatically by JUnit.
   */
  @TempDir
  Path temporaryDirectory;

  /**
   * Generates a representative MDB fixture, opens it through Mdbora and scans every table using the public JDBC API.
   *
   * @throws Exception if the fixture cannot be generated, opened or scanned
   */
  @Test
  void scansSyntheticAccessDatabase() throws Exception {
    Path databaseFile = temporaryDirectory.resolve("mdbora-performance-fixture.mdb");

    LargeAccessFixtureGenerator.generate(databaseFile, CUSTOMER_COUNT, ORDER_COUNT, ITEM_COUNT);

    assertTrue(Files.isRegularFile(databaseFile), "The synthetic MDB fixture should exist");

    Properties properties = createConnectionProperties();
    String jdbcUrl = MdboraDriver.URL_PREFIX + databaseFile;

    try (Connection connection = DriverManager.getConnection(jdbcUrl, properties)) {
      MdboraDatabaseScanner.scanAllTables(connection, false, false);
    }
  }

  /**
   * Creates the connection properties used by the performance test.
   *
   * @return configured JDBC connection properties
   */
  private static Properties createConnectionProperties() {
    Properties properties = new Properties();

    properties.setProperty(MdboraProperty.CACHE_SIZE, Integer.toString(CACHE_SIZE_KIB));
    properties.setProperty(MdboraProperty.MAX_IN_MEMORY_ROWS, Integer.toString(MAX_IN_MEMORY_ROWS));

    return properties;
  }
}
