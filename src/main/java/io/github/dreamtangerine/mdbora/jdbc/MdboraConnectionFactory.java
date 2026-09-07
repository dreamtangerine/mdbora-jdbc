/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */
package io.github.dreamtangerine.mdbora.jdbc;

import io.github.dreamtangerine.mdbora.access.AccessDatabaseRegistry;
import io.github.dreamtangerine.mdbora.config.MdboraConfiguration;
import io.github.dreamtangerine.mdbora.internal.TemporaryH2;
import io.github.dreamtangerine.mdbora.virtual.AccessVirtualTableRegistrar;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.DatabaseBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

/**
 * Creates Mdbora JDBC connections.
 */
final class MdboraConnectionFactory {

  private static final String DATABASE_ID_PREFIX = "MDBORA_";

  private MdboraConnectionFactory() {
  }

  /**
   * Opens a Mdbora JDBC connection.
   *
   * @param url Mdbora JDBC URL
   * @param properties connection properties
   * @return read-only JDBC connection
   * @throws SQLException if the connection cannot be opened
   */
  static Connection open(String url, Properties properties) throws SQLException {
    MdboraConfiguration configuration = MdboraConfiguration.from(properties);
    Path accessFile = extractAccessFile(url);

    validateAccessFile(accessFile);

    String databaseId = createDatabaseId();

    Database accessDatabase = null;

    TemporaryH2 temporaryH2 = null;

    boolean registered = false;


    try {
      accessDatabase = openAccessDatabase(accessFile);
      temporaryH2 = TemporaryH2.open(configuration);

      AccessDatabaseRegistry.register(databaseId, accessDatabase);

      registered = true;

      AccessVirtualTableRegistrar.registerAll(temporaryH2.connection(), databaseId, accessDatabase, configuration);

      MdboraConnectionContext context = new MdboraConnectionContext(databaseId, url, accessDatabase, temporaryH2);

      return MdboraConnectionProxy.wrap(context, temporaryH2.connection());

    } catch (Exception exception) {
      closeAfterFailure(databaseId, registered, accessDatabase, temporaryH2);

      throw convertException(accessFile, exception);
    }
  }

  /**
   * Extracts the Access file path from a Mdbora JDBC URL.
   *
   * @param url Mdbora JDBC URL
   * @return absolute and normalized Access file path
   * @throws SQLException if the URL or file path is invalid
   */
  private static Path extractAccessFile(String url) throws SQLException {
    if (url == null) {
      throw new SQLException("The Mdbora JDBC URL cannot be null");
    }

    if (!url.startsWith(MdboraDriver.URL_PREFIX)) {
      throw new SQLException("Invalid Mdbora JDBC URL: " + url);
    }

    String rawPath = url.substring(MdboraDriver.URL_PREFIX.length());

    if (rawPath.isBlank()) {
      throw new SQLException("The MDB or ACCDB file path is missing from the URL: " + url);
    }

    try {
      return Path.of(rawPath).toAbsolutePath().normalize();
    } catch (RuntimeException exception) {
      throw new SQLException("Invalid MDB or ACCDB file path: " + rawPath, exception);
    }
  }

  /**
   * Validates that an Access file exists, is readable and has a supported extension.
   *
   * @param accessFile Access file path
   * @throws SQLException if the file cannot be used by Mdbora
   */
  private static void validateAccessFile(Path accessFile) throws SQLException {

    if (!Files.exists(accessFile)) {
      throw new SQLException("The Access file does not exist: " + accessFile);
    }

    if (!Files.isRegularFile(accessFile)) {
      throw new SQLException("The Access path does not refer to a regular file: " + accessFile);
    }

    if (!Files.isReadable(accessFile)) {
      throw new SQLException("The Access file is not readable: " + accessFile);
    }

    validateAccessExtension(accessFile);
  }

  /**
   * Validates that a file has an MDB or ACCDB extension.
   *
   * @param accessFile Access file path
   * @throws SQLException if the file extension is not supported
   */
  private static void validateAccessExtension(Path accessFile) throws SQLException {
    String fileName = accessFile.getFileName().toString().toLowerCase(Locale.ROOT);
    boolean supportedExtension = fileName.endsWith(".mdb") || fileName.endsWith(".accdb");

    if (!supportedExtension) {
      throw new SQLException("Mdbora only supports MDB and ACCDB files: " + accessFile);
    }
  }

  /**
   * Opens an Access database using Jackcess.
   *
   * @param accessFile Access file path
   * @return open Jackcess database
   * @throws SQLException if the database cannot be opened
   */
  private static Database openAccessDatabase(Path accessFile) throws SQLException {
    try {
      return DatabaseBuilder.open(accessFile.toFile());
    } catch (Exception exception) {
      throw new SQLException("Unable to open the Access file: " + accessFile, exception);
    }
  }

  /**
   * Creates a unique identifier for the Access database registry.
   *
   * @return unique database identifier
   */
  private static String createDatabaseId() {
    String uuid = UUID.randomUUID().toString().replace("-", "");

    return DATABASE_ID_PREFIX + uuid;
  }

  /**
   * Releases resources created before a connection opening failure.
   *
   * @param databaseId database registry identifier
   * @param registered whether the Access database was registered
   * @param accessDatabase open Access database, if available
   * @param temporaryH2 temporary SQL catalog, if available
   */
  private static void closeAfterFailure(String databaseId, boolean registered, Database accessDatabase, TemporaryH2 temporaryH2) {
    if (registered) {
      AccessDatabaseRegistry.detach(databaseId);
    }

    closeTemporaryH2AfterFailure(temporaryH2);
    closeAccessDatabaseAfterFailure(accessDatabase);
  }

  /**
   * Closes the temporary SQL catalog after an opening failure.
   *
   * @param temporaryH2 temporary SQL catalog, or {@code null}
   */
  private static void closeTemporaryH2AfterFailure(TemporaryH2 temporaryH2) {
    if (temporaryH2 != null) {
      try {
        temporaryH2.close();
      } catch (Exception ignored) {
        // Preserve the original connection opening exception.
      }
    }
  }

  /**
   * Closes the Access database after an opening failure.
   *
   * @param accessDatabase Access database, or {@code null}
   */
  private static void closeAccessDatabaseAfterFailure(Database accessDatabase) {
    if (accessDatabase != null) {
      try {
        accessDatabase.close();
      } catch (Exception ignored) {
        // Preserve the original connection opening exception.
      }
    }
  }

  /**
   * Converts an opening failure into an appropriate SQL exception.
   *
   * @param accessFile Access file being opened
   * @param exception original exception
   * @return SQL exception to report to the JDBC client
   */
  private static SQLException convertException(Path accessFile, Exception exception) {
    SQLException result;

    if (exception instanceof SQLException) {
      result = (SQLException) exception;
    } else {
      result = new SQLException("Unable to open the Access database: " + accessFile, exception);
    }

    return result;
  }
}
