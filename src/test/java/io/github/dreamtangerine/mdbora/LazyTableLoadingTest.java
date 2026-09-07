package io.github.dreamtangerine.mdbora;

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
import io.github.dreamtangerine.mdbora.jdbc.MdboraDriver;
import io.github.dreamtangerine.mdbora.testsupport.MetadataFixtureGenerator;
import io.github.spannm.jackcess.Database;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Access tables are registered from their definitions and can be resolved when their data is queried.
 */
class LazyTableLoadingTest {

  private static final String TEST_TABLE_NAME = "METADATA_SAMPLE";
  
  @TempDir
  Path temporaryDirectory;

  /**
   * Registers table definitions, retrieves JDBC metadata and executes a query that requires the underlying Jackcess table to be resolved.
   *
   * @throws Exception if the fixture cannot be generated or queried
   */
  @Test
  void resolvesAccessTableWhenItIsQueried() throws Exception {
    Path databaseFile = createFixture();
    String url = MdboraDriver.URL_PREFIX + databaseFile;

    try (Connection connection = DriverManager.getConnection(url)) {
      verifyConnection(connection);
      verifyTableIsExposed(connection, TEST_TABLE_NAME);
      verifyTableCanBeQueried(connection, TEST_TABLE_NAME);
    }
  }

  /**
   * Creates the Access fixture used by the test.
   *
   * @return generated MDB file
   * @throws Exception if the fixture cannot be generated
   */
  private Path createFixture() throws Exception {
    Path output = temporaryDirectory.resolve("lazy-table-loading.mdb");
    Path result = MetadataFixtureGenerator.generate(output, Database.FileFormat.V2000);

    assertTrue(Files.isRegularFile(result), "The metadata fixture should exist");

    return result;
  }

  /**
   * Verifies that the JDBC connection is valid.
   *
   * @param connection Mdbora JDBC connection
   * @throws Exception if the connection metadata cannot be retrieved
   */
  private static void verifyConnection(Connection connection) throws Exception {

    assertNotNull(
            connection,
            "The Mdbora connection should not be null");

    assertFalse(
            connection.isClosed(),
            "The Mdbora connection should be open");

    assertTrue(
            connection.isReadOnly(),
            "The Mdbora connection should be read-only");

    assertNotNull(
            connection.getMetaData(),
            "The JDBC metadata should be available");
  }

  /**
   * Verifies that a table is visible through JDBC metadata before its rows are queried.
   *
   * @param connection Mdbora JDBC connection
   * @param expectedTableName expected table name
   * @throws Exception if the table metadata cannot be read
   */
  /**
   * Verifies that a table is visible through JDBC metadata before its rows are queried.
   *
   * @param connection Mdbora JDBC connection
   * @param expectedTableName expected table name
   * @throws SQLException if the table metadata cannot be read
   */
  private static void verifyTableIsExposed(Connection connection, String expectedTableName) throws SQLException {
    DatabaseMetaData metadata = connection.getMetaData();
    boolean found = false;

    try (ResultSet tables = metadata.getTables(null, null, "%", null)) {
      while (tables.next() && !found) {
        String currentTableName = tables.getString("TABLE_NAME");
        
        System.out.println(currentTableName);

        if (expectedTableName.equalsIgnoreCase(currentTableName)) {
          found = true;
        }
      }
    }

    assertTrue(found, "The table should be exposed through JDBC metadata: " + expectedTableName);
  }

  /**
   * Executes a query that forces Mdbora to resolve the underlying Jackcess table and create a cursor.
   *
   * @param connection Mdbora JDBC connection
   * @param tableName table to query
   * @throws Exception if the table cannot be queried
   */
  private static void verifyTableCanBeQueried(Connection connection, String tableName) throws Exception {
    String sql
            = "SELECT COUNT(*) "
            + "FROM "
            + quoteIdentifier(tableName);

    long rowCount
            = 0L;

    try (Statement statement
            = connection.createStatement(); ResultSet rows
            = statement.executeQuery(sql)) {

      assertTrue(
              rows.next(),
              "The count query should return one row");

      rowCount
              = rows.getLong(1);

      assertFalse(
              rows.next(),
              "The count query should return exactly one row");
    }

    assertTrue(
            rowCount > 0L,
            "The synthetic Access table should contain rows");
  }

  /**
   * Quotes an SQL identifier.
   *
   * @param value unquoted identifier
   * @return quoted SQL identifier
   */
  private static String quoteIdentifier(String value) {
    String result
            = "\""
            + value.replace(
                    "\"",
                    "\"\"")
            + "\"";

    return result;
  }
}
