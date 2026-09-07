/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora;

import io.github.dreamtangerine.mdbora.jdbc.MdboraDriver;
import io.github.dreamtangerine.mdbora.testsupport.MetadataFixtureGenerator;
import io.github.dreamtangerine.mdbora.testsupport.ResultSetPrinter;
import io.github.spannm.jackcess.Database;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies the database and driver metadata exposed by Mdbora.
 */
class MdboraDatabaseMetaDataTest {

  @TempDir
  Path temporaryDirectory;

  /**
   * Verifies the identity and capabilities reported for an Access 2000 MDB database.
   *
   * @throws Exception if the fixture cannot be generated or queried
   */
  @Test
  void exposesMetadataForV2000Database() throws Exception {
    exposesMetadataForDatabase(Database.FileFormat.V2000);
  }

  private void exposesMetadataForDatabase(Database.FileFormat fileFormat) throws Exception {
    Path databaseFile = MetadataFixtureGenerator.generate(temporaryDirectory.resolve("metadata-" + fileFormat.name() + fileFormat.getFileExtension()), fileFormat);

    assertTrue(Files.isRegularFile(databaseFile), "The metadata fixture should exist");

    String url = MdboraDriver.URL_PREFIX + databaseFile;

    Properties properties = new Properties();

    try (Connection connection = DriverManager.getConnection(url, properties)) {
      DatabaseMetaData metadata = connection.getMetaData();

      assertNotNull(metadata);

      verifyDatabaseProduct(metadata, fileFormat.name());

      /*
       * When tests run from target/classes instead of the packaged JAR,
       * the version may be reported as "development".
       */
      assertNotNull(metadata.getDriverVersion());

      assertFalse(metadata.getDriverVersion().isBlank());

      assertEquals(0, metadata.getDriverMajorVersion());
      assertEquals(1, metadata.getDriverMinorVersion());

      assertEquals(url, metadata.getURL());
      assertEquals("", metadata.getUserName());

      assertTrue(metadata.isReadOnly());
      assertFalse(metadata.supportsTransactions());
      assertFalse(metadata.supportsSavepoints());
      assertFalse(metadata.supportsStoredProcedures());
      assertFalse(metadata.supportsBatchUpdates());
      assertFalse(metadata.supportsGetGeneratedKeys());
      assertEquals(Connection.TRANSACTION_NONE, metadata.getDefaultTransactionIsolation());
      assertSame(connection, metadata.getConnection());
    }
  }

  /**
   * Generates and verifies the metadata fixture for every Access format supported by the fixture generator.
   *
   * @param fileFormat Access database format to test
   * @throws Exception if the fixture cannot be generated, opened or verified
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("creatableFileFormats")
  void exposesMetadataForCreatableDatabaseFormats(Database.FileFormat fileFormat) throws Exception {
    exposesMetadataForDatabase(fileFormat);
  }

  /**
   * Provides the Access database formats supported by the metadata fixture generator.
   *
   * @return stream of creatable Access database formats
   */
  public static Stream<Database.FileFormat> creatableFileFormats() {
    Stream<Database.FileFormat> result = Stream.of(
            Database.FileFormat.V2000,
            Database.FileFormat.V2003,
            Database.FileFormat.V2007,
            Database.FileFormat.V2010,
            Database.FileFormat.V2016,
            Database.FileFormat.V2019);

    return result;
  }

  private static void verifyMetadataTable(DatabaseMetaData metadata) throws Exception {
    boolean tableFound = false;

    try (ResultSet tables = metadata.getTables(null, "PUBLIC", "METADATA_SAMPLE", new String[]{"TABLE"})) {
      while (tables.next() && !tableFound) {
        String tableName = tables.getString("TABLE_NAME");

        if ("METADATA_SAMPLE".equalsIgnoreCase(tableName)) {
          tableFound = true;
        }
      }
    }

    assertTrue(tableFound, "MEADATA_SAMPLE should be exposed through JDBC metadata");
  }

  private static void verifyDatabaseProduct(DatabaseMetaData metadata, String expectedVersion) throws Exception {
    printMetadata(metadata);

    assertEquals("Microsoft Access", metadata.getDatabaseProductName());
    assertEquals(expectedVersion, metadata.getDatabaseProductVersion());
    assertEquals("Mdbora JDBC Driver", metadata.getDriverName());

    verifyMetadataTable(metadata);

    exposesOnlyThePublicSchema(metadata);
    exposesPublicSchemaWithPattern(metadata);
  }

  /**
   * Prints the database and driver metadata exposed by Mdbora.
   *
   * @param metadata database metadata to print
   * @throws Exception if a metadata value cannot be obtained
   */
  public static void printMetadata(DatabaseMetaData metadata) throws Exception {
    System.out.println();
    System.out.println("Mdbora JDBC metadata");
    System.out.println("====================");

    System.out.printf(
            "Database product name:       %s%n",
            metadata.getDatabaseProductName());

    System.out.printf(
            "Database product version:    %s%n",
            metadata.getDatabaseProductVersion());

    System.out.printf(
            "Database major version:      %d%n",
            metadata.getDatabaseMajorVersion());

    System.out.printf(
            "Database minor version:      %d%n",
            metadata.getDatabaseMinorVersion());

    System.out.printf(
            "Driver name:                 %s%n",
            metadata.getDriverName());

    System.out.printf(
            "Driver version:              %s%n",
            metadata.getDriverVersion());

    System.out.printf(
            "Driver major version:        %d%n",
            metadata.getDriverMajorVersion());

    System.out.printf(
            "Driver minor version:        %d%n",
            metadata.getDriverMinorVersion());

    System.out.printf(
            "JDBC major version:          %d%n",
            metadata.getJDBCMajorVersion());

    System.out.printf(
            "JDBC minor version:          %d%n",
            metadata.getJDBCMinorVersion());

    System.out.printf(
            "Connection URL:              %s%n",
            metadata.getURL());

    System.out.printf(
            "User name:                   %s%n",
            metadata.getUserName());

    System.out.printf(
            "Read only:                   %s%n",
            metadata.isReadOnly());

    System.out.printf(
            "Supports transactions:       %s%n",
            metadata.supportsTransactions());

    System.out.printf(
            "Supports savepoints:          %s%n",
            metadata.supportsSavepoints());

    System.out.printf(
            "Supports stored procedures:  %s%n",
            metadata.supportsStoredProcedures());

    System.out.printf(
            "Supports batch updates:       %s%n",
            metadata.supportsBatchUpdates());

    System.out.printf(
            "Supports generated keys:      %s%n",
            metadata.supportsGetGeneratedKeys());

    System.out.printf(
            "Transaction isolation:       %d%n",
            metadata.getDefaultTransactionIsolation());

    System.out.println();
  }

  private static void exposesOnlyThePublicSchema(DatabaseMetaData metadata) throws Exception {
    try (ResultSet schemas = metadata.getSchemas()) {
      ResultSetPrinter.print("SCHEMAS", schemas);
    }

    boolean publicFound = false;
    boolean informationSchemaFound = false;

    try (ResultSet schemas = metadata.getSchemas()) {

      while (schemas.next()) {
        String schemaName = schemas.getString("TABLE_SCHEM");

        if ("PUBLIC".equalsIgnoreCase(schemaName)) {
          publicFound = true;
        }

        if ("INFORMATION_SCHEMA".equalsIgnoreCase(schemaName)) {
          informationSchemaFound = true;
        }
      }
    }

    assertTrue(publicFound, "PUBLIC should be exposed");
    assertFalse(informationSchemaFound, "The internal H2 INFORMATION_SCHEMA should not be exposed");
    assertFalse(schemasContainAdditionalRows(metadata), "Mdbora should expose exactly one schema");
  }

  private static void exposesPublicSchemaWithPattern(DatabaseMetaData metadata) throws Exception {
    try (ResultSet schemas = metadata.getSchemas(null, "%")) {
      assertTrue(schemas.next());
      assertEquals("PUBLIC", schemas.getString("TABLE_SCHEM"));
      assertNull(schemas.getString("TABLE_CATALOG"));
      assertFalse(schemas.next());
    }
  }

  /**
   * Determines whether Mdbora exposes more than one schema.
   *
   * @param metadata JDBC database metadata
   * @return {@code true} if more than one schema is exposed
   * @throws SQLException if the schemas cannot be read
   */
  private static boolean schemasContainAdditionalRows(DatabaseMetaData metadata) throws SQLException {
    int count = 0;

    try (ResultSet schemas = metadata.getSchemas()) {

      while (schemas.next()) {
        count++;
      }
    }

    return count != 1;
  }
}
