/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Dreamtangerine
 */

package io.github.dreamtangerine.mdbora.jdbc;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.DatabaseBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import io.github.dreamtangerine.mdbora.access.AccessDatabaseRegistry;
import io.github.dreamtangerine.mdbora.config.MdboraConfiguration;
import io.github.dreamtangerine.mdbora.internal.TemporaryH2;
import io.github.dreamtangerine.mdbora.virtual.AccessVirtualTableRegistrar;

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
   *
   * @return read-only JDBC connection
   *
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

    Connection connection;

    try {
      accessDatabase = openAccessDatabase(accessFile);

      temporaryH2 = TemporaryH2.open(configuration);

      AccessDatabaseRegistry.register(databaseId, accessDatabase);

      registered = true;

      AccessVirtualTableRegistrar.registerAll(temporaryH2.connection(), databaseId, accessDatabase);

      MdboraConnectionContext context = new MdboraConnectionContext(databaseId, accessDatabase, temporaryH2);

      connection = MdboraConnectionProxy.wrap(context, temporaryH2.connection());
    } catch (Exception exception) {
      closeAfterFailure(databaseId, registered, accessDatabase, temporaryH2);

      connection = null;

      throw convertException(accessFile, exception);
    }

    return connection;
  }

  private static Path extractAccessFile(String url) throws SQLException {
    Path accessFile = null;

    if (url == null) {
      throw new SQLException("La URL JDBC de Mdbora no puede ser nula");
    }

    if (!url.startsWith(MdboraDriver.URL_PREFIX)) {
      throw new SQLException("URL JDBC no válida para Mdbora: " + url);
    }

    String rawPath = url.substring(MdboraDriver.URL_PREFIX.length());

    if (rawPath.isBlank()) {
      throw new SQLException("Falta la ruta del archivo MDB o ACCDB en la URL: " + url);
    }

    try {
      accessFile = Path.of(rawPath).toAbsolutePath().normalize();
    } catch (RuntimeException exception) {
      throw new SQLException("Ruta MDB o ACCDB no válida: " + rawPath, exception);
    }

    return accessFile;
  }

  private static void validateAccessFile(Path accessFile) throws SQLException {
    if (!Files.exists(accessFile)) {
      throw new SQLException("El archivo Access no existe: " + accessFile);
    }

    if (!Files.isRegularFile(accessFile)) {
      throw new SQLException("La ruta Access no corresponde a un archivo regular: " + accessFile);
    }

    if (!Files.isReadable(accessFile)) {
      throw new SQLException("El archivo Access no se puede leer: " + accessFile);
    }

    validateAccessExtension(accessFile);
  }

  private static void validateAccessExtension(Path accessFile) throws SQLException {
    String fileName = accessFile.getFileName().toString().toLowerCase(Locale.ROOT);
    boolean supportedExtension = fileName.endsWith(".mdb") || fileName.endsWith(".accdb");

    if (!supportedExtension) {
      throw new SQLException("Mdbora solo admite archivos MDB o ACCDB: " + accessFile);
    }
  }

  private static Database openAccessDatabase(Path accessFile) throws SQLException {
    try {
      return DatabaseBuilder.open(accessFile.toFile());
    } catch (Exception exception) {
      throw new SQLException("No se puede abrir el archivo Access: " + accessFile, exception);
    }
  }

  private static String createDatabaseId() {
    String uuid = UUID.randomUUID().toString().replace("-", "");
    String databaseId = DATABASE_ID_PREFIX + uuid;

    return databaseId;
  }

  private static void closeAfterFailure(String databaseId, boolean registered, Database accessDatabase, TemporaryH2 temporaryH2) {
    if (registered) {
      AccessDatabaseRegistry.detach(databaseId);
    }

    closeTemporaryH2AfterFailure(temporaryH2);
    closeAccessDatabaseAfterFailure(accessDatabase);
  }

  private static void closeTemporaryH2AfterFailure(TemporaryH2 temporaryH2) {
    if (temporaryH2 != null) {
      try {
        temporaryH2.close();
      } catch (Exception ignored) {
        // Se conserva la excepción original de apertura.
      }
    }
  }

  private static void closeAccessDatabaseAfterFailure(Database accessDatabase) {
    if (accessDatabase != null) {
      try {
        accessDatabase.close();
      } catch (Exception ignored) {
        // Se conserva la excepción original de apertura.
      }
    }
  }

  private static SQLException convertException(Path accessFile, Exception exception) {
    SQLException result;

    if (exception instanceof SQLException) {
      result = (SQLException) exception;
    } else {
      result = new SQLException("No se puede abrir la base Access: " + accessFile, exception);
    }

    return result;
  }
}
